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
            // Se é líder, inicializa a transmissão de mensagens
            SendTransmitter transmitter = new SendTransmitter(nodeId);
            transmitter.start();
        }else{

        receiveMessages();}
    }

    private void receiveMessages() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);

            System.out.println(nodeId + " conectado ao grupo multicast, a aguardar mensagens...");

            while (true) {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());

                // Extrai o tipo de mensagem e o ID do nó remetente
                String[] parts = receivedMessage.split(":");
                String messageType = parts[0];
                String senderId = parts[1];
                
                System.out.println(nodeId + " recebeu um " + messageType + " do " + senderId);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}