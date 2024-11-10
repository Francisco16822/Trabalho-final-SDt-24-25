package com.sdt;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Leader extends UnicastRemoteObject implements LeaderInterface {
    private String nodeId;
    private SendTransmitter transmitter;
    private Map<String, String> documentVersions;
    private Map<String, Set<String>> ackMap;
    private MessageList messageList;
    private static final int MAJORITY = 2;  // Define a maioria para 3 nos
    private static final int ACK_PORT = 4447;  // Porta para receber ACKs

    public Leader(String nodeId, MessageList messageList) throws RemoteException {
        super();
        this.nodeId = nodeId;
        this.documentVersions = new HashMap<>();
        this.ackMap = new HashMap<>();
        this.messageList = messageList;
        this.transmitter = new SendTransmitter(nodeId, this, messageList);

        startAckReceiver();
    }

    @Override
    public void updateDocument(String documentId, String content) throws RemoteException {
        documentVersions.put(documentId, content);
        System.out.println("Documento " + documentId + " atualizado para a nova versão pelo cliente.");
        transmitter.sendDocumentUpdate(documentId, content);
    }

    public void receiveAck(String documentId, String nodeId) {
        ackMap.computeIfAbsent(documentId, k -> new HashSet<>()).add(nodeId);
        if (ackMap.get(documentId).size() >= MAJORITY) {
            transmitter.sendCommit(documentId);
            ackMap.remove(documentId);
        }
    }

    private void startAckReceiver() {
        Thread ackReceiverThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(ACK_PORT)) {
                System.out.println(nodeId + " esta a receber os ACKs na porta " + ACK_PORT);

                while (true) {
                    byte[] buffer = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String ackMessage = new String(packet.getData(), 0, packet.getLength());

                    // Verifica se a mensagem é um ACK
                    if (ackMessage.startsWith("ACK")) {
                        String[] parts = ackMessage.split(":");
                        String documentId = parts[1];
                        String senderNodeId = parts[2];
                        System.out.println(nodeId + " recebeu ACK de " + senderNodeId + " para documento " + documentId);

                        receiveAck(documentId, senderNodeId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        ackReceiverThread.start();
    }
}