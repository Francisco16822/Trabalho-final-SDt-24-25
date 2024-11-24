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
    private Map<String, Integer> ackCountMap = new HashMap<>();

    private Map<String, Set<String>> pendingAckRequests = new HashMap<>(); // Pedidos pendentes de ACK por documento
    private Map<String, Integer> failedAckCounts = new HashMap<>(); // Contagem de falhas consecutivas por nó



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
        this.failedAckCounts = new HashMap<>();

        startAckMonitor();
    }

    // Método para adicionar um nó à lista de nós ativos
    public synchronized void addNode(String nodeId) throws RemoteException {
        if (!activeNodes.contains(nodeId)) {
            activeNodes.add(nodeId);
            sendDocumentsToNewNode(nodeId);
            notifyActiveNodesAboutNewNode(nodeId);
            System.out.println("\n" +nodeId + " ------> ATIVO.");
            System.out.println("NÓS ATIVOS : " + activeNodes+ "\n");

        }
    }

    // Método para remover um nó da lista de nós ativos
    public synchronized void removeNode(String nodeId) throws RemoteException {
        activeNodes.remove(nodeId);
        redistributeDocuments(nodeId);
        System.out.println("Nó " + nodeId + " removido da lista de nós ativos.\n");
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
        System.out.println();
    }

    @Override
    public void updateDocument(String documentId, String content) throws RemoteException {
        documentVersions.put(documentId, content);
        System.out.println(documentId + " atualizado para a nova versão pelo cliente.\n");
        ackCountMap.put(documentId, 0);
        transmitter.sendDocumentUpdate(documentId, content);


        logTransaction(documentId, content);
    }

    @Override
    public synchronized void receiveAck(String documentId, String nodeId) throws RemoteException {
        // Verificar se o nó é ativo
        if (!activeNodes.contains(nodeId)) {
            System.out.println("ACK recebido de um nó não ativo: " + nodeId);
            return;
        }

        ackMap.putIfAbsent(documentId, new HashSet<>()); // Inicializa um conjunto para o documento, se necessário
        if (ackMap.get(documentId).contains(nodeId)) {
            return;
        }

        ackMap.get(documentId).add(nodeId);

        // Inicializar contador para o documento, se necessário
        ackCountMap.putIfAbsent(documentId, 0);

        // Incrementar o contador de ACKs para o documento
        ackCountMap.put(documentId, ackCountMap.get(documentId) + 1);
        System.out.println("ACK recebido de " + nodeId + " ----> " + documentId);

        // Calcular o quorum necessário
        float majority = (float) Math.ceil(activeNodes.size() / 2.0);

        // Verificar se o quorum foi atingido
        if (ackCountMap.get(documentId) >= majority) {
            System.out.println("Maioria atingida para o documento " + documentId + ". Enviando COMMIT.\n");
            ackCountMap.remove(documentId); // Limpar contador após atingir o quorum
            transmitter.sendCommit(documentId); // Enviar o COMMIT
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


    private void notifyActiveNodesAboutNewNode(String newNodeId) throws RemoteException {
        for (String activeNodeId : activeNodes) {
            if (!activeNodeId.equals(newNodeId)) {
                transmitter.sendNewNodeNotificationToNode(activeNodeId, newNodeId);
                System.out.println("A notificar o nó " + activeNodeId + " sobre o novo nó " + newNodeId);
            }
        }
    }


    //Sprint 4

    //Método para rastrear pedidos de ACK
    private synchronized void trackAckRequests(String documentId) {
        pendingAckRequests.putIfAbsent(documentId, new HashSet<>(activeNodes)); // Aguardar ACK de todos os nós
        ackCountMap.put(documentId, 0); // Inicializa contador de ACKs como 0
    }

    private synchronized void checkForFailedNodes() throws RemoteException {
        for (Map.Entry<String, Set<String>> entry : pendingAckRequests.entrySet()) {

            for (String nodeId : new HashSet<>(activeNodes)) {
                // Incrementar falhas consecutivas para nós que não responderam
                failedAckCounts.put(nodeId, failedAckCounts.getOrDefault(nodeId, 0) + 1);

                // Se ultrapassar o limite de falhas, remover nó
                if (failedAckCounts.get(nodeId) >= 2) {
                    System.out.println(nodeId + " Falha no envio de ACK's. O nó será removido");
                    removeNode(nodeId);
                }
            }
        }
    }

    private void startAckMonitor() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // Verificar a cada 5 segundos
                    checkForFailedNodes();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
