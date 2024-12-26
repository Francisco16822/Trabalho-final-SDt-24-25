package com.sdt;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.util.*;
import java.io.*;

public class SendTransmitter extends Thread {
    private static final String MULTICAST_ADDRESS = "224.0.1.0";
    private static final int PORT = 4446;
    private String nodeId;
    private Leader_RMI_Handler leader;
    private MessageList messageList;
    private boolean isLeader;

    private Set<String> committedDocuments = new HashSet<>();
    private List<String> tempUpdates = new ArrayList<>();
    private MulticastSocket socket;
    private List<String> pendingUpdates;

    public SendTransmitter(String nodeId, Leader_RMI_Handler leader, MessageList messageList) {
        this.nodeId = nodeId;
        this.leader = leader;
        this.messageList = messageList;
        this.isLeader = "Leader".equals(nodeId);
        this.pendingUpdates = new ArrayList<>();
        try {
            this.socket = new MulticastSocket();
            this.socket.joinGroup(InetAddress.getByName(MULTICAST_ADDRESS));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendDocumentUpdate(String documentId, String content) {
        String updateMessage = " SYNC " + documentId + ":" + content;
        messageList.addMessage(updateMessage);
        pendingUpdates.add(documentId);
    }

    public void sendCommit(String documentId) {
        String commitMessage = "HEARTBEAT COMMIT " + documentId;

        if (!committedDocuments.contains(documentId)) {
            committedDocuments.add(documentId);
            sendMulticastMessage(commitMessage);
            System.out.println("COMMIT enviado para " + documentId);
            committedDocuments.remove(documentId);
        }
    }


    @Override
    public void run() {
        if (!isLeader) return;

        while (true) {
            try {
                sendHeartbeat();
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendHeartbeat() {
        List<String> allMessages = messageList.createSendStructure();
        String consolidatedHeartbeat = String.join(";", allMessages);
        sendMulticastMessage("HEARTBEAT" + consolidatedHeartbeat);
    }

    private void sendMulticastMessage(String message) {
        try {
            byte[] plainMessage = message.getBytes();
            DatagramPacket packet = new DatagramPacket(plainMessage, plainMessage.length, InetAddress.getByName(MULTICAST_ADDRESS), PORT);
            socket.send(packet);
            System.out.println(nodeId + " enviou: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
