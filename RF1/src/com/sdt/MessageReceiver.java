package com.sdt;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class MessageReceiver extends Thread {
    private String nodeId;
    private MessageList messageList;
    private static final String MULTICAST_ADDRESS = "224.0.1.0";
    private static final int MULTICAST_PORT = 4446;

    private List<String> tempUpdates = new ArrayList<>(); // Lista temporária para SYNC
    private List<String> definitiveUpdates = new ArrayList<>(); // Lista definitiva após COMMIT

    public MessageReceiver(String nodeId, MessageList messageList) {
        this.nodeId = nodeId;
        this.messageList = messageList;
    }

    private void sendAck(String documentId) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            LeaderInterface leader = (LeaderInterface) registry.lookup("Leader");
            leader.receiveAck(documentId, nodeId);
            System.out.println(nodeId + " enviou ACK via RMI para o líder");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erro ao enviar ACK para o líder.");
        }
    }

    private void respondToHeartbeat() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            LeaderInterface leader = (LeaderInterface) registry.lookup("Leader");
            leader.updateAckTime(nodeId);
            System.out.println(nodeId + " respondeu ao heartbeat do líder.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erro ao responder ao heartbeat do líder.");
        }
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);

            while (true) {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                System.out.println(nodeId + " recebeu: " + receivedMessage);
                respondToHeartbeat();

                if (receivedMessage.startsWith("HEARTBEAT SYNC")) {
                    handleSyncMessage(receivedMessage);
                } else if (receivedMessage.startsWith("HEARTBEAT COMMIT")) {
                    handleCommitMessage(receivedMessage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSyncMessage(String receivedMessage) {
        String[] parts = receivedMessage.split(":");
        String documentId = parts[0];
        String content = parts[1];
        tempUpdates.add(documentId + ":" + content);
        System.out.println(nodeId + " recebeu atualização SYNC e adicionou à lista temporária.");
        sendAck(documentId);
    }

    private void handleCommitMessage(String receivedMessage) {
        String[] parts = receivedMessage.split(" ");
        String documentId = parts[2];
        applyCommit(documentId);
        System.out.println(nodeId + " aplicou atualização COMMIT para o documento: " + documentId);
    }

    private void applyCommit(String documentId) {
        tempUpdates.removeIf(update -> {
            if (update.startsWith(documentId + ":")) {
                definitiveUpdates.add(update);
                return true; // Remove da lista temporária
            }
            return false;
        });
    }
}
