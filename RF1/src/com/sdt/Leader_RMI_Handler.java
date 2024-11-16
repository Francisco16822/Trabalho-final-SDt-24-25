package com.sdt;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Leader_RMI_Handler extends UnicastRemoteObject implements LeaderInterface {
    private String nodeId;
    private SendTransmitter transmitter;
    private Map<String, String> documentVersions;
    private Map<String, Set<String>> ackMap;
    private MessageList messageList;
    private static final int MAJORITY = 2;  // Define a maioria para 3 nós

    public Leader_RMI_Handler(String nodeId, MessageList messageList) throws RemoteException {
        super();
        this.nodeId = nodeId;
        this.documentVersions = new HashMap<>();
        this.ackMap = new HashMap<>();
        this.messageList = messageList;
        this.transmitter = new SendTransmitter(nodeId, this, messageList);
    }

    @Override
    public void updateDocument(String documentId, String content) throws RemoteException {
        documentVersions.put(documentId, content);
        System.out.println("Documento " + documentId + " atualizado para a nova versão pelo cliente.");
        transmitter.sendDocumentUpdate(documentId, content);
    }

    @Override
    public synchronized void receiveAck(String documentId, String nodeId) throws RemoteException {
        ackMap.computeIfAbsent(documentId, k -> new HashSet<>()).add(nodeId);
        if (ackMap.get(documentId).size() >= MAJORITY) {
            transmitter.sendCommit(documentId);
            ackMap.remove(documentId);
        }
    }
}
