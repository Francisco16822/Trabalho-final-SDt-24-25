package com.sdt;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;

public class Node {
    private String nodeId;
    private boolean isLeader;
    private static final String MULTICAST_ADDRESS = "224.0.0.1";
    private static final int PORT = 4446;

    public Node(String nodeId, boolean isLeader) {
        this.nodeId = nodeId;
        this.isLeader = isLeader;
    }

    public void start() {
        if (isLeader) {
            // Se é líder, inicia a transmissão de mensagens a cada 5 segundos
            SendTransmitter transmitter = new SendTransmitter(nodeId);
            transmitter.startSending();
        }
        else{

        // A thread é criada para receber mensagens
        MessageReceiver receiver = new MessageReceiver(nodeId);
        receiver.start();}
    }

}