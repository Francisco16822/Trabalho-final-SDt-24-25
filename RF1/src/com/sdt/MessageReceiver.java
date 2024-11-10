package com.sdt;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.DatagramSocket;

public class MessageReceiver extends Thread {
    private String nodeId;
    private MessageList messageList;
    private static final String MULTICAST_ADDRESS = "224.0.0.1";
    private static final int MULTICAST_PORT = 4446;
    private static final int ACK_PORT = 4447;

    public MessageReceiver(String nodeId, MessageList messageList) {
        this.nodeId = nodeId;
        this.messageList = messageList;
    }

    private void sendAck(String documentId) {
        try (DatagramSocket socket = new DatagramSocket()) {
            String ackMessage = "ACK:" + documentId + ":" + nodeId;
            byte[] buffer = ackMessage.getBytes();

            InetAddress leaderAddress = InetAddress.getByName("localhost");  // Endereço IP do líder
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, leaderAddress, ACK_PORT);
            // Envia o pacote de ACK ao líder
            socket.send(packet);
            System.out.println(nodeId + " enviou ACK para " + documentId);
        } catch (Exception e) {
            e.printStackTrace();
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

                if (receivedMessage.startsWith("DOC_UPDATE")) {
                    String[] parts = receivedMessage.split(":");
                    String documentId = parts[1];
                    sendAck(documentId);
                    messageList.addMessage(receivedMessage);
                } else if (receivedMessage.startsWith("COMMIT")) {
                    String documentId = receivedMessage.split(":")[1];
                    messageList.addMessage(receivedMessage);
                } else if (receivedMessage.startsWith("HEARTBEAT")) {
                    System.out.println(nodeId + " recebeu heartbeat do líder.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
