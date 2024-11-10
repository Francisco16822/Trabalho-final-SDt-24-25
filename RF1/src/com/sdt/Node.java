package RF1.src.com.sdt;

public class Node {
    private String nodeId;
    private boolean isLeader;
    private MessageList messageList;
    private Leader leader;
    private MessageReceiver receiver;

    public Node(String nodeId, boolean isLeader, MessageList messageList) {
        this.nodeId = nodeId;
        this.isLeader = isLeader;
        this.messageList = messageList;
    }

    public void start() {
        if (isLeader) {
            try {
                // Inicializa o líder
                leader = new Leader(nodeId, messageList);
                System.out.println(nodeId + " iniciado como líder.");

                // Inicia a thread de transmissão para o líder
                SendTransmitter transmitter = new SendTransmitter(nodeId, leader, messageList);
                transmitter.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Inicia o receptor de mensagens para um nó comum
            receiver = new MessageReceiver(nodeId, messageList);
            receiver.start();
        }
    }

    public Leader getLeader() {
        return leader;
    }

    public String getNodeId() {
        return nodeId;
    }
}
