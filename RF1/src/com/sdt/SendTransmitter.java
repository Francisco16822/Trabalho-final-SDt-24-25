package com.sdt;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;

public class SendTransmitter extends Thread {
    private static final String MULTICAST_ADDRESS = "224.0.0.1";
    private static final int PORT = 4446;
    private String nodeId;

    // Tipos de mensagem
    public enum MessageType {
        SYNC_REQUEST,
        HEARTBEAT
    }

    // Lista de mensagens a serem enviadas
    private final List<String> messageQueue = new ArrayList<>();

    public SendTransmitter(String nodeId) {
        this.nodeId = nodeId;
    }

    public void startSending() {
        this.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Adiciona mensagens à lista
                messageQueue.add(formatMessage(MessageType.SYNC_REQUEST));
                messageQueue.add(formatMessage(MessageType.HEARTBEAT));

                // Intervalo de 5 segundos entre envios
                Thread.sleep(5000);

                // Envia a lista de mensagens
                sendMessages();

                // Limpa a lista após o envio
                messageQueue.clear();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String formatMessage(MessageType messageType) {
        return messageType + ":" + nodeId;
    }

    private void sendMessages() {
        try (MulticastSocket socket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);

            // Concatena todas as mensagens na lista numa única string
            StringBuilder concatenatedMessages = new StringBuilder();
            for (String message : messageQueue) {
                concatenatedMessages.append(message).append(";");
            }

            // Envia a string concatenada como um pacote
            byte[] buffer = concatenatedMessages.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);

            System.out.println(nodeId + " enviou a lista de mensagens: " + concatenatedMessages);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}