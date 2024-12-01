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
    private List<String> activeNodes;
    private Map<String, Long> lastAckTimestampMap;

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

    // Implementação do método para retornar versões dos documentos
    @Override
    public Map<String, String> getDocumentVersions() throws RemoteException {
        return documentVersions;
    }

    // Implementação do método para retornar documentos pendentes
    @Override
    public List<String> getPendingUpdates() throws RemoteException {
        return pendingUpdates;
    }


    public void UpdateAckTime(String nodeid) throws RemoteException{
        lastAckTimestampMap.put(nodeId, System.currentTimeMillis());
    }

    // Método que processa o recebimento de ACK
    @Override
    public synchronized void receiveAck(String documentId, String nodeId) throws RemoteException {
        UpdateAckTime(nodeId);
        // Adiciona o ACK ao mapa de ACKs
        Set<String> acks = ackMap.get(documentId);
        if (acks == null) {
            acks = new HashSet<>();
            ackMap.put(documentId, acks);
        }
        acks.add(nodeId); // Adiciona o ACK para o nó que enviou a confirmação

        float majority = (float) Math.ceil(activeNodes.size() / 2.0);
        if (ackMap.get(documentId).size() >= majority) {
            transmitter.sendCommit(documentId);
            ackMap.remove(documentId);
            if (acks.size() == activeNodes.size()) {
                // Todos os ACKs foram recebidos, remove da lista de pendências
                pendingUpdates.remove(documentId);
                System.out.println("Todos os ACKs recebidos para o documento " + documentId);
            }
        }
    }

    // Método que verifica se algum nó falhou devido ao timeout
    private synchronized void checkForFailedNodes() throws RemoteException {
        long currentTime = System.currentTimeMillis();

        for (String nodeId : new HashSet<>(activeNodes)) {
            Long lastAckTime = lastAckTimestampMap.get(nodeId);
            if (currentTime - lastAckTime > ACK_TIMEOUT) {
                System.out.println("Falha detectada no nó " + nodeId + ", removendo...");
                removeNode(nodeId);
                lastAckTimestampMap.remove(nodeId); // Remove o nó falho do mapa de timestamps
            }
        }
    }

    // Método que inicia o monitoramento dos ACKs
    private void startAckMonitor() {
        new Thread(() -> {
            while (true) {
                try {
                    if (!pendingUpdates.isEmpty()){
                        Thread.sleep(ACK_CHECK_INTERVAL);
                        checkForFailedNodes(); // Verificar se algum nó falhou
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
            lastAckTimestampMap.put(nodeId, System.currentTimeMillis()); // Inicializa o timestamp do nó
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

    // Método que remove um nó da lista de nós ativos
    private void removeNode(String nodeId) throws RemoteException {
        activeNodes.remove(nodeId);
        lastAckTimestampMap.remove(nodeId);
        System.out.println("Nó " + nodeId + " removido da lista de nós ativos.");
    }
}
