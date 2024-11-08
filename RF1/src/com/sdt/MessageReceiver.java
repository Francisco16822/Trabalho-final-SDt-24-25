package com.sdt;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MessageReceiver extends Thread {
    private String nodeId;
    private static final String MULTICAST_ADDRESS = "224.0.0.1";
    private static final int PORT = 4446;

    public MessageReceiver(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);

            System.out.println(nodeId + " conectado ao grupo multicast, a aguardar mensagens...");

            while (true) {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());

                // Dividir a mensagem concatenada em mensagens individuais
                String[] individualMessages = receivedMessage.split(";");

                for (String message : individualMessages) {
                    // Extrai o tipo de mensagem e o ID do nó remetente para cada mensagem
                    String[] parts = message.split(":");
                    if (parts.length == 2) {
                        String messageType = parts[0];
                        String senderId = parts[1];
                        System.out.println(nodeId + " recebeu um " + messageType + " do " + senderId);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}