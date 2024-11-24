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

    private List<String> tempUpdates = new ArrayList<>();

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

                if (receivedMessage.startsWith("HEARTBEAT SYNC")) {
                    String[] parts = receivedMessage.split(":");
                    String documentId = parts[0];
                    String content = parts[1];
                    tempUpdates.add(receivedMessage);
                    System.out.println(nodeId + " recebeu atualização SYNC.");
                    sendAck(documentId); // Envia ACK via RMI
                } else if (receivedMessage.startsWith("HEARTBEAT COMMIT")) {
                    applyTempUpdates();
                    System.out.println(nodeId + " aplicou atualizações COMMIT.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyTempUpdates() {

        for (String update : tempUpdates) {
            messageList.addMessage(update); // Adiciona como pendente (SYNC)
            //System.out.println(nodeId + " adicionou atualização pendente: " + update);
        }
        tempUpdates.clear();
    }
}
