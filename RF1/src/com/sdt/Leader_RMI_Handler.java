package com.sdt;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Leader_RMI_Handler extends UnicastRemoteObject implements LeaderInterface {
    private String nodeId;
    private SendTransmitter transmitter;
    private Map<String, String> documentVersions;  // Armazena os documentos e suas versões
    private Map<String, Set<String>> ackMap;
    private MessageList messageList;
    private List<String> pendingUpdates;
    protected List<String> activeNodes;
    private Map<String, Long> lastAckTimestampMap;

    protected long activeAcksCount;

    private static final long ACK_TIMEOUT = 15000; // 15 segundos
    private static final long ACK_CHECK_INTERVAL = 5000; // Verificação de falhas a cada 5 segundos

    public Leader_RMI_Handler(String nodeId, MessageList messageList) throws RemoteException {
        super();
        this.nodeId = nodeId;
        this.documentVersions = new HashMap<>();
        this.ackMap = new HashMap<>();
        this.messageList = messageList;
        this.transmitter = new SendTransmitter(nodeId, this, messageList);
        this.pendingUpdates = new ArrayList<>();
        this.activeNodes = new ArrayList<>();
        this.lastAckTimestampMap = new HashMap<>();

        startAckMonitor();
    }

    @Override
    public Map<String, String> getDocumentVersions() throws RemoteException {
        return documentVersions;
    }

    @Override
    public List<String> getPendingUpdates() throws RemoteException {
        return pendingUpdates;
    }

    @Override
    public synchronized void receiveAck(String documentId, String nodeId) throws RemoteException {
        updateAckTime(nodeId);

        // Adiciona o ACK no mapa correspondente
        ackMap.putIfAbsent(documentId, new HashSet<>());
        ackMap.get(documentId).add(nodeId);

        Set<String> acks = ackMap.get(documentId);
        long activeAcksCount = acks.stream().filter(activeNodes::contains).count();

        int majority = (int) Math.ceil(activeNodes.size() / 2.0);

        // Enviar commit se a maioria for atingida
        if (activeAcksCount >= majority) {
            System.out.println("Maioria atingida para o documento " + documentId);
            transmitter.sendCommit(documentId);
            ackMap.remove(documentId);
            pendingUpdates.clear();
        }
    }





    @Override
    public void updateAckTime(String nodeId) throws RemoteException {
        lastAckTimestampMap.put(nodeId, System.currentTimeMillis());
    }

    private synchronized void checkForFailedNodes() throws RemoteException {
        long currentTime = System.currentTimeMillis();
        for (String nodeId : new HashSet<>(activeNodes)) {
            Long lastAckTime = lastAckTimestampMap.get(nodeId);
            if (lastAckTime != null && currentTime - lastAckTime > ACK_TIMEOUT) {
                System.out.println("Falha detectada no nó " + nodeId + ", removendo...");
                removeNode(nodeId);
                lastAckTimestampMap.remove(nodeId);
            }
        }
    }

    private void startAckMonitor() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(ACK_CHECK_INTERVAL);
                    if (!pendingUpdates.isEmpty()) {
                        System.out.println("Verificando pendências: " + pendingUpdates);
                        checkForFailedNodes();
                    }
                } catch (InterruptedException | RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    @Override
    public synchronized void addNode(String nodeId) throws RemoteException {
        if (!activeNodes.contains(nodeId)) {
            activeNodes.add(nodeId);
            lastAckTimestampMap.put(nodeId, System.currentTimeMillis());
            System.out.println("Nó " + nodeId + " adicionado aos nós ativos.");
        }
    }

    @Override
    public synchronized void updateDocument(String documentId, String content) throws RemoteException {
        documentVersions.put(documentId, content);
        ackMap.put(documentId, new HashSet<>());
        pendingUpdates.add(documentId);
        transmitter.sendDocumentUpdate(documentId, content);
    }

    private void removeNode(String nodeId) throws RemoteException {
        activeNodes.remove(nodeId);
        lastAckTimestampMap.remove(nodeId);
        System.out.println("Nó " + nodeId + " removido da lista de nós ativos.");
    }
}
