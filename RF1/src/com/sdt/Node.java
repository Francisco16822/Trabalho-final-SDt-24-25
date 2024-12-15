package com.sdt;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Node {
    private String nodeId;
    private MessageList messageList;
    private MessageReceiver receiver;
    private SendTransmitter sendTransmitter;
    private LeaderInterface leader;
    private boolean isLeader = false; // Indica se este nó é o líder

    private static final long LEADER_TIMEOUT = 15000; // Timeout para considerar o líder falho
    private long lastHeartbeatTime = System.currentTimeMillis(); // Tempo do último heartbeat recebido

    public Node(String nodeId, MessageList messageList) {
        this.nodeId = nodeId;
        this.messageList = messageList;
    }

    public void start() {
        try {
            // Conecta ao líder existente
            leader = (LeaderInterface) Naming.lookup("//localhost/Leader");
            leader.addNode(nodeId);
            synchronizeWithLeader();

            // Inicializa receptor e transmissor
            receiver = new MessageReceiver(nodeId, messageList);
            receiver.start();
            sendTransmitter = new SendTransmitter(nodeId, new Leader_RMI_Handler(nodeId, messageList), messageList);
            sendTransmitter.start();

            // Inicia monitor de falha do líder
            startLeaderMonitor();
        } catch (Exception e) {
            System.out.println("Falha ao conectar ao líder. Iniciando eleição...");
            startLeaderElection();
        }
    }

    private void startLeaderMonitor() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // Verifica a cada 5 segundos
                    if (System.currentTimeMillis() - lastHeartbeatTime > LEADER_TIMEOUT) {
                        System.out.println("Líder inativo. Iniciando eleição...");
                        startLeaderElection();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startLeaderElection() {
        try {
            // Eleição pelo maior nodeId
            System.out.println(nodeId + " iniciando eleição...");
            isLeader = true; // Assume temporariamente que será o líder
            for (String otherNode : ActiveNodes()) {
                if (otherNode.compareTo(nodeId) > 0) {
                    isLeader = false;
                    break;
                }
            }

            if (isLeader) {
                System.out.println(nodeId + " se tornou o novo líder!");
                leader = new Leader_RMI_Handler(nodeId, messageList);
                for (String otherNode : ActiveNodes()) {
                    leader.addNode(otherNode);
                }
            } else {
                System.out.println(nodeId + " aguardando novo líder.");
                leader = (LeaderInterface) Naming.lookup("//localhost/Leader");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CopyOnWriteArrayList<String> ActiveNodes() {
        // Simula uma lista de nós ativos (em produção, isso seria obtido do RMI Registry)
        try {
            return leader.getActiveNodes();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private void synchronizeWithLeader() {
        try {
            Map<String, String> documentVersions = leader.getDocumentVersions();
            List<String> pendingUpdates = leader.getPendingUpdates();
            System.out.println("Sincronizando documentos do líder...");
            leader.updateAckTime(nodeId);

            for (Map.Entry<String, String> entry : documentVersions.entrySet()) {
                String documentId = entry.getKey();
                String content = entry.getValue();
                System.out.println("Documento sincronizado: " + documentId + " -> " + content);
                messageList.addMessage("SYNC " + documentId + ":" + content + ":" + System.currentTimeMillis());
            }

            for (String update : pendingUpdates) {
                System.out.println("Aplicando update pendente: " + update);
                messageList.addMessage(update);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
