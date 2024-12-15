package com.sdt;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
    private static final long LEADER_TIMEOUT = 20000; // Timeout para considerar o líder falho
    private CopyOnWriteArrayList<String> activeNodes = new CopyOnWriteArrayList<>();

    public Node(String nodeId, MessageList messageList) {
        this.nodeId = nodeId;
        this.messageList = messageList;
    }

    public void start() {
        try {
            // Tentar se conectar ao líder
            leader = (LeaderInterface) Naming.lookup("//localhost/Leader");
            leader.addNode(nodeId);
            // Caso não haja líder, ou se o líder falhar, entraremos em processo de eleição
            synchronizeWithLeader();
            receiver = new MessageReceiver(nodeId, messageList);
            receiver.start();
            sendTransmitter = new SendTransmitter(nodeId, new Leader_RMI_Handler(nodeId, messageList), messageList);
            sendTransmitter.start();

            // Inicia a monitoria do líder, apenas se este nó não for o líder
            if (!isLeader) {
                startLeaderMonitor();
            }
        } catch (Exception e) {
            System.out.println("Falha ao conectar ao líder. Iniciar eleição...");
            startLeaderElection();
        }
    }

    private void startLeaderMonitor() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // Checa a falha a cada 5 segundos
                    if (leader == null) {
                        System.out.println("Líder não está disponível, iniciando eleição...");
                        startLeaderElection();
                    } else {
                        long lastHeartbeatTime = leader.updateHeartbeatTime(nodeId);
                        if (System.currentTimeMillis() - lastHeartbeatTime > LEADER_TIMEOUT) {
                            System.out.println("Falha no líder detectada, iniciando eleição...");
                            startLeaderElection(); // Caso o líder não responda dentro do tempo limite, inicia a eleição
                        }
                    }
                } catch (InterruptedException | RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void startLeaderElection() {
        try {
            // O nó que detectar a ausência de líder inicia uma eleição
            System.out.println(nodeId + " a iniciar eleição...");
            isLeader = true; // Inicialmente, consideramos que o nó pode ser o líder

            // Enviar uma mensagem para todos os outros nós, dizendo que está iniciando uma eleição
            for (String otherNode : activeNodes) {
                if (otherNode.compareTo(nodeId) > 0) {
                    isLeader = false;
                    break;
                }
            }

            if (isLeader) {
                // O nó com o maior ID será eleito líder
                System.out.println(nodeId + " é o novo líder!");
                nodeId = "LEADER";
                Leader_RMI_Handler leaderHandler = new Leader_RMI_Handler(nodeId, messageList);
                try {
                    Registry registry = LocateRegistry.createRegistry(1099);  // Porta padrão para RMI
                    registry.rebind("Leader", leaderHandler);
                } catch (RemoteException e) {
                    System.err.println("Erro ao registar o líder no RMI Registry: " + e.getMessage());
                    e.printStackTrace();
                }

                leader = new Leader_RMI_Handler(nodeId, messageList);

                // Enviar o heartbeat a partir de agora
                SendTransmitter sendTransmitter = new SendTransmitter(nodeId, leaderHandler, messageList);
                sendTransmitter.startHeartbeats(); // Start sending heartbeats

                for (String otherNode : activeNodes) {
                    leader.addNode(otherNode);
                }
            } else {
                // Caso contrário, o nó aguarda o novo líder
                System.out.println(nodeId + " aguardando novo líder.");
                leader = (LeaderInterface) Naming.lookup("//localhost/Leader");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CopyOnWriteArrayList<String> ActiveNodes() {
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
            System.out.println("Sincronizar documentos do líder...");

            leader.updateAckTime(nodeId);

            // Atualiza os documentos
            for (Map.Entry<String, String> entry : documentVersions.entrySet()) {
                String documentId = entry.getKey();
                String content = entry.getValue();
                System.out.println("Documento sincronizado: " + documentId + " -> " + content);
                messageList.addMessage("SYNC " + documentId + ":" + content + ":" + System.currentTimeMillis());
            }

            // Aplica as atualizações pendentes
            for (String update : pendingUpdates) {
                System.out.println("Aplicando atualização pendente: " + update);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}