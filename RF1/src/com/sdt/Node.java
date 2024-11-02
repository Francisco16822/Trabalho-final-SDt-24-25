package com.sdt;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Node extends Thread {
    private String nodeId;
    private boolean isLeader;
    private static final String MULTICAST_ADDRESS = "224.0.0.1";
    private static final int PORT = 4446;

    public Node(String nodeId, boolean isLeader) {
        this.nodeId = nodeId;
        this.isLeader = isLeader;
    }

    @Override
    public void run() {
        if (isLeader) {
            startLeaderTasks();
        } else {
            receiveMessages();
        }
    }

    private void startLeaderTasks() {
        while (true) {
            try {
                // Envia uma mensagem de sincronização como líder
                sendMessage("Pedido de Sincronização de " + nodeId);

                // Envia heartbeat para os nós comuns
                sendMessage("HEARTBEAT de " + nodeId);

                // Intervalo entre envios
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessage(String message) {
        try (MulticastSocket socket = new MulticastSocket()) {
            byte[] buffer = message.getBytes();
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
            System.out.println(nodeId + " enviou: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            System.out.println(nodeId + " conectado ao grupo multicast, aguardando mensagens...");

            while (true) {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                System.out.println(nodeId + " recebeu: " + receivedMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}