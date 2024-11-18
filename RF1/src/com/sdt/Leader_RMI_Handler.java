package com.sdt;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Leader_RMI_Handler extends UnicastRemoteObject implements LeaderInterface {
    private String nodeId;
    private SendTransmitter transmitter;
    private Map<String, String> documentVersions;
    private Map<String, Set<String>> ackMap;
    private MessageList messageList;
    private static final int MAJORITY = 2;
    private List<String> pendingUpdates;
    private List<String> activeNodes;


    private List<String> transactionLog = new ArrayList<>();

    public Leader_RMI_Handler(String nodeId, MessageList messageList) throws RemoteException {
        super();
        this.nodeId = nodeId;
        this.documentVersions = new HashMap<>();
        this.ackMap = new HashMap<>();
        this.messageList = messageList;
        this.transmitter = new SendTransmitter(nodeId, this, messageList);
        this.pendingUpdates = new ArrayList<>();
        this.activeNodes = new ArrayList<>();
    }

    // Método para adicionar um nó à lista de nós ativos
    public synchronized void addNode(String nodeId) throws RemoteException {
        if (!activeNodes.contains(nodeId)) {
            activeNodes.add(nodeId);
            sendDocumentsToNewNode(nodeId);
            notifyActiveNodesAboutNewNode(nodeId);
            System.out.println("Nó " + nodeId + " adicionado à lista de nós ativos.");
        }
    }

    // Método para remover um nó da lista de nós ativos
    public synchronized void removeNode(String nodeId) throws RemoteException {
        activeNodes.remove(nodeId);
        redistributeDocuments(nodeId);
        System.out.println("Nó " + nodeId + " removido da lista de nós ativos.");
    }

    // Método para lidar com a saída de um nó
    public synchronized void handleNodeExit(String nodeId) throws RemoteException {

        removeNode(nodeId);
        System.out.println("Nó " + nodeId + " removido da lista de nós ativos.");
    }


    private void sendDocumentsToNewNode(String nodeId) throws RemoteException {

        for (Map.Entry<String, String> entry : documentVersions.entrySet()) {
            String documentId = entry.getKey();
            String content = entry.getValue();
            transmitter.sendDocumentUpdateToNode(nodeId, documentId, content);
        }


        for (String transaction : transactionLog) {
            transmitter.sendPendingUpdateToNode(nodeId, transaction);
        }
    }

    // Redistribui os documentos de um nó removido para os outros nós
    private void redistributeDocuments(String nodeId) throws RemoteException {
        for (Map.Entry<String, String> entry : documentVersions.entrySet()) {
            String documentId = entry.getKey();
            String content = entry.getValue();

            for (String activeNodeId : activeNodes) {
                if (!activeNodeId.equals(nodeId)) {
                    transmitter.sendDocumentUpdateToNode(activeNodeId, documentId, content);
                }
            }
        }

        // Redistribui as atualizações pendentes aos nós ativos
        for (String updateMessage : pendingUpdates) {
            for (String activeNodeId : activeNodes) {
                if (!activeNodeId.equals(nodeId)) {
                    transmitter.sendPendingUpdateToNode(activeNodeId, updateMessage);
                }
            }
        }
    }

    // Método para registar as transações incrementais
    private synchronized void logTransaction(String documentId, String content) {
        String transaction = documentId + ":" + content + ":" + System.currentTimeMillis();
        transactionLog.add(transaction);
        System.out.println("Transação registada: " + transaction);
    }

    @Override
    public void updateDocument(String documentId, String content) throws RemoteException {
        documentVersions.put(documentId, content);
        System.out.println("Documento " + documentId + " atualizado para a nova versão pelo cliente.");
        transmitter.sendDocumentUpdate(documentId, content);


        logTransaction(documentId, content);
    }

    @Override
    public synchronized void receiveAck(String documentId, String nodeId) throws RemoteException {
        ackMap.computeIfAbsent(documentId, k -> new HashSet<>()).add(nodeId);
        if (ackMap.get(documentId).size() >= MAJORITY) {
            transmitter.sendCommit(documentId);
            ackMap.remove(documentId);
        }
    }

    @Override
    public List<String> getDocumentList() throws RemoteException {
        return new ArrayList<>(documentVersions.keySet());
    }

    @Override
    public Map<String, String> getDocumentVersions() throws RemoteException {
        return new HashMap<>(documentVersions);
    }

    @Override
    public List<String> getPendingUpdates() throws RemoteException {
        return new ArrayList<>(pendingUpdates);
    }


    private void notifyActiveNodesAboutNewNode(String nodeId) throws RemoteException {
        for (String activeNodeId : activeNodes) {
            if (!activeNodeId.equals(nodeId)) {
                transmitter.sendNewNodeNotificationToNode(activeNodeId, nodeId);
                System.out.println("A notificar o nó " + activeNodeId + " sobre o novo nó " + nodeId);
            }
        }
    }
}
