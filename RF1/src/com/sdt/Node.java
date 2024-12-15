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
    private static final long LEADER_TIMEOUT = 20000; // Timeout para considerar o líder falho
    private CopyOnWriteArrayList<String> activeNodes = new CopyOnWriteArrayList<>(); // Lista de nós ativos

    public Node(String nodeId, MessageList messageList) {
        this.nodeId = nodeId;
        this.messageList = messageList;
    }

    public void start() {
        try {
            // Tentar se conectar ao líder
            leader = (LeaderInterface) Naming.lookup("//localhost/Leader");
            // Caso não haja líder, ou se o líder falhar, entraremos em processo de eleição
            synchronizeWithLeader();
            receiver = new MessageReceiver(nodeId, messageList);
            receiver.start();
            sendTransmitter = new SendTransmitter(nodeId, new Leader_RMI_Handler(nodeId, messageList), messageList);
            sendTransmitter.start();
            startLeaderMonitor();
        } catch (Exception e) {
            System.out.println("Falha ao conectar ao líder. Iniciando eleição...");
            startLeaderElection();
        }
    }

    // Método para monitorar o líder e verificar se ele está ativo
    private void startLeaderMonitor() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    if (leader != null) {
                        long lastHeartbeatTime = leader.updateHeartbeatTime(nodeId);
                        if (System.currentTimeMillis() - lastHeartbeatTime > LEADER_TIMEOUT) {
                            System.out.println("Líder inativo. Iniciando eleição...");
                            startLeaderElection();  // Inicia uma nova eleição
                        }
                    }
                } catch (InterruptedException | RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Método para iniciar a eleição de líder
    private void startLeaderElection() {
        try {
            // O nó que detectar a ausência de líder inicia uma eleição
            System.out.println(nodeId + " iniciando eleição...");
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
                leader = new Leader_RMI_Handler(nodeId, messageList);
                // Registra o líder no RMI Registry
                Naming.rebind("//localhost/Leader", leader);
                System.out.println(nodeId + " registrado como líder.");

                // Realiza o processo de inicialização do líder, enviando os documentos, etc.
                for (String otherNode : activeNodes) {
                    leader.addNode(otherNode);  // Adiciona outros nós ao líder
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

    // Método para obter os nós ativos
    private CopyOnWriteArrayList<String> getActiveNodes() {
        try {
            if (leader != null) {
                return leader.getActiveNodes();
            } else {
                throw new RuntimeException("Líder não disponível.");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return new CopyOnWriteArrayList<>();
        }
    }

    // Método para sincronizar documentos com o líder
    private void synchronizeWithLeader() {
        try {
            if (leader != null) {
                Map<String, String> documentVersions = leader.getDocumentVersions();
                List<String> pendingUpdates = leader.getPendingUpdates();
                System.out.println("Sincronizando documentos do líder...");
                leader.updateAckTime(nodeId); // Atualiza o tempo de resposta do nó

                // Sincroniza os documentos com o líder
                for (Map.Entry<String, String> entry : documentVersions.entrySet()) {
                    String documentId = entry.getKey();
                    String content = entry.getValue();
                    System.out.println("Documento sincronizado: " + documentId + " -> " + content);
                    messageList.addMessage("SYNC " + documentId + ":" + content + ":" + System.currentTimeMillis());
                }

                // Aplica as atualizações pendentes
                for (String update : pendingUpdates) {
                    System.out.println("Aplicando update pendente: " + update);
                    messageList.addMessage(update);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
