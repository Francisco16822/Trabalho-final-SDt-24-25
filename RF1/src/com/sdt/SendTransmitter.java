package com.sdt;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class SendTransmitter extends Thread {
    private static final String MULTICAST_ADDRESS = "224.0.0.1";
    private static final int PORT = 4446;
    private String nodeId;

    // Tipos de mensagem
    public enum MessageType {
        SYNC_REQUEST,
        HEARTBEAT
    }

    public SendTransmitter(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public void run() {
        while (true) {
            try {
                sendMessage(MessageType.SYNC_REQUEST);
                sendMessage(MessageType.HEARTBEAT);

                // Intervalo entre envios
                Thread.sleep(5000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessage(MessageType messageType) {
        try (MulticastSocket socket = new MulticastSocket()) {
            String formattedMessage = messageType + ":" + nodeId;
            byte[] buffer = formattedMessage.getBytes();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
            System.out.println(nodeId + " enviou um " + messageType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}