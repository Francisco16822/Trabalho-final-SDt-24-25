package com.sdt;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

public class Leader_RMI_Handler extends UnicastRemoteObject implements LeaderInterface {
    private final String nodeId;
    private final SendTransmitter transmitter;
    private final Map<String, String> documentVersions;
    private final Map<String, Set<String>> ackMap; // Thread-safe
    private final MessageList messageList;
    private final List<String> pendingUpdates; // Thread-safe
    public final CopyOnWriteArrayList<String> activeNodes; // Thread-safe
    private final ConcurrentHashMap<String, Long> lastAckTimestampMap; // Thread-safe
    private static final long ACK_TIMEOUT = 15000; // 15 segundos
    private static final long ACK_CHECK_INTERVAL = 5000;
    private Map<String, Integer> failedAckCounts = new HashMap<>();
    public Leader_RMI_Handler(String nodeId, MessageList messageList) throws RemoteException {
        super();
        this.nodeId = nodeId;
        this.documentVersions = new ConcurrentHashMap<>();
        this.ackMap = new ConcurrentHashMap<>();
        this.messageList = messageList;
        this.transmitter = new SendTransmitter(nodeId, this, messageList);
        this.pendingUpdates = new CopyOnWriteArrayList<>();
        this.activeNodes = new CopyOnWriteArrayList<>();
        this.lastAckTimestampMap = new ConcurrentHashMap<>();
        startMonitor();
    }

    @Override
    public synchronized Map<String, String> getDocumentVersions() throws RemoteException {
        return new HashMap<>(documentVersions);
    }

    @Override
    public CopyOnWriteArrayList<String> getActiveNodes() throws RemoteException {
        return activeNodes;
    }

    @Override
    public List<String> getPendingUpdates() throws RemoteException {
        return new ArrayList<>(pendingUpdates);
    }

    @Override
    public synchronized void receiveAck(String documentId, String nodeId) throws RemoteException {
        updateAckTime(nodeId);
        ackMap.computeIfAbsent(documentId, k -> ConcurrentHashMap.newKeySet());
        ackMap.get(documentId).add(nodeId);

        Set<String> acks = ackMap.get(documentId);
        long activeAcksCount = acks.stream().filter(activeNodes::contains).count();

        float majority = (float) (activeNodes.size() / 2.0);

        if (activeAcksCount >= majority && !pendingUpdates.isEmpty()) {
            System.out.println("Maioria atingida para o documento " + documentId);
            transmitter.sendCommit(documentId);
            ackMap.remove(documentId);
            pendingUpdates.clear();
        }
    }


    @Override
    public synchronized void updateAckTime(String nodeId) throws RemoteException {
        lastAckTimestampMap.put(nodeId, System.currentTimeMillis());
    }

    @Override
    public synchronized long updateHeartbeatTime(String nodeId) throws RemoteException {
        long currentTime = System.currentTimeMillis();
        lastAckTimestampMap.put(nodeId, currentTime);
        return currentTime;
    }

    private void startMonitor() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(ACK_CHECK_INTERVAL);  // Intervalo entre as verificações de falhas
                    if (!pendingUpdates.isEmpty()) {
                        resendPendingUpdates();  // Reenviar atualizações pendentes
                    }
                    checkForFailedNodes();  // Verificação de falhas
                } catch (InterruptedException | RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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


    private synchronized void resendPendingUpdates() {
        for (String documentId : new ArrayList<>(pendingUpdates)) {
            String content = documentVersions.get(documentId);
            if (content != null) {
                System.out.println("Reenviar atualizações pendentes: " + documentId);
                transmitter.sendDocumentUpdate(documentId, content);
            }
        }
    }

    @Override
    public synchronized void addNode(String nodeId) throws RemoteException {
        if (!activeNodes.contains(nodeId)) {
            activeNodes.add(nodeId);
            lastAckTimestampMap.put(nodeId, System.currentTimeMillis());
            System.out.println("Nó " + nodeId + " adicionado aos nós ativos.");
            for (Map.Entry<String, String> entry : documentVersions.entrySet()) {
                transmitter.sendDocumentUpdate(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public synchronized void updateDocument(String documentId, String content) throws RemoteException {
        documentVersions.put(documentId, content);
        ackMap.put(documentId, ConcurrentHashMap.newKeySet());
        pendingUpdates.add(documentId);
        transmitter.sendDocumentUpdate(documentId, content);
    }

    @Override
    public synchronized void setNewLeader(String nodeId) throws RemoteException {
        System.out.println("Novo líder definido: " + nodeId);
        activeNodes.clear();
        activeNodes.add(nodeId); // Atualiza a lista de nós ativos
    }

    private synchronized void removeNode(String nodeId) throws RemoteException {
        activeNodes.remove(nodeId);
        lastAckTimestampMap.remove(nodeId);
        System.out.println("Nó " + nodeId + " removido da lista de nós ativos.");
    }
}