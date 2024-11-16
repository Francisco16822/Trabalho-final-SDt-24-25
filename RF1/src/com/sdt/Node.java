package com.sdt;

public class Node {
    private String nodeId;
    private MessageList messageList;
    private MessageReceiver receiver;

    public Node(String nodeId, MessageList messageList) {
        this.nodeId = nodeId;
        this.messageList = messageList;
    }

    public void start() {
        receiver = new MessageReceiver(nodeId, messageList);
        receiver.start();
    }

    public String getNodeId() {
        return nodeId;
    }
}
