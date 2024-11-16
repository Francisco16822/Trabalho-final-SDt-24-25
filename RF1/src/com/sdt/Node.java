package com.sdt;

public class Node {
    private String nodeId;
    private boolean isLeader;
    private Leader_RMI_Handler leader;
    private MessageReceiver receiver;

    public Node(String nodeId, boolean isLeader) {
        this.nodeId = nodeId;
        this.isLeader = isLeader;
    }

    public void start() {
        if (isLeader) {
            try {
                leader = new Leader_RMI_Handler(nodeId);
                System.out.println(nodeId + " iniciado como líder.");

                java.rmi.registry.LocateRegistry.createRegistry(1099).rebind("Leader", leader);
                System.out.println("Líder registrado no RMI.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            receiver = new MessageReceiver(nodeId, new MessageList());
            receiver.start();
        }
    }

    public Leader_RMI_Handler getLeader() {
        return leader;
    }
}
