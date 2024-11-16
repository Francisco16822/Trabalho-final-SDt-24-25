package com.sdt;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageReceiver extends Thread {
    private String nodeId;
    private MessageList messageList;
    private Map<String, String> localDocuments = new HashMap<>(); // Armazena documentos sincronizados
    private List<String> temporarySyncList = new ArrayList<>(); // Lista temporária para SYNC
    private static final String LEADER_RMI_HOST = "localhost";
    private static final int LEADER_RMI_PORT = 1099;

    public MessageReceiver(String nodeId, MessageList messageList) {
        this.nodeId = nodeId;
        this.messageList = messageList;
    }

    @Override
    public void run() {
        try {
            Registry registry = LocateRegistry.getRegistry(LEADER_RMI_HOST, LEADER_RMI_PORT);
            LeaderInterface leader = (LeaderInterface) registry.lookup("Leader");

            while (true) {
                List<String> messages = messageList.getClone();
                for (String message : messages) {
                    if (message.startsWith("DOC_UPDATE")) {
                        String[] parts = message.split(":");
                        String documentId = parts[1];
                        String content = parts[2];
                        temporarySyncList.add(documentId);
                        localDocuments.put(documentId, content); // Armazena localmente o documento
                        sendAck(documentId, leader); // Envia ACK via RMI
                    } else if (message.startsWith("COMMIT")) {
                        String documentId = message.split(":")[1];
                        commitDocument(documentId);
                    }
                }
                Thread.sleep(1000); // Reduz carga
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendAck(String documentId, LeaderInterface leader) {
        try {
            leader.receiveAck(documentId, nodeId);
            System.out.println(nodeId + " enviou ACK para documento: " + documentId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commitDocument(String documentId) {
        System.out.println(nodeId + " commitando documento: " + documentId);
        if (temporarySyncList.contains(documentId)) {
            String content = localDocuments.get(documentId);
            System.out.println("Documento " + documentId + " sincronizado com conteúdo: " + content);
            temporarySyncList.remove(documentId);
        }
    }
}
