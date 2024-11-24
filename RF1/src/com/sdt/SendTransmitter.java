package com.sdt;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.util.*;
import java.io.*;
import java.util.zip.GZIPOutputStream;

public class SendTransmitter extends Thread {
    private static final String MULTICAST_ADDRESS = "224.0.1.0";
    private static final int PORT = 4446;
    private String nodeId;
    private Leader_RMI_Handler leader;
    private MessageList messageList;
    private boolean isLeader;

    private Set<String> committedDocuments = new HashSet<>();
    private List<String> tempUpdates = new ArrayList<>();
    private MulticastSocket socket;

    public SendTransmitter(String nodeId, Leader_RMI_Handler leader, MessageList messageList) {
        this.nodeId = nodeId;
        this.leader = leader;
        this.messageList = messageList;
        this.isLeader = "Leader".equals(nodeId);
        try {

            this.socket = new MulticastSocket();
            this.socket.joinGroup(InetAddress.getByName(MULTICAST_ADDRESS));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sendDocumentUpdate(String documentId, String content) {
        String updateMessage = " SYNC " + documentId + ":" + content;
        messageList.addMessage(updateMessage);
    }


    public void sendCommit(String documentId) {
        //Tira a parte "HEARTBEAT SYNC" do nome do documeto
        String cleanDocumentId = documentId.replace("HEARTBEAT SYNC", "").trim();
        String commitMessage = " COMMIT " + cleanDocumentId;

        //Condição para nao mostrar mensagem repetida
        if (!committedDocuments.contains(cleanDocumentId)) {
            committedDocuments.add(cleanDocumentId);
            messageList.addMessage(commitMessage);
            System.out.println("Documento " + cleanDocumentId + " tornado permanente.\n");
        }
    }


    public void sendHeartbeat() {
        List<String> allMessages = messageList.createSendStructure();
        String consolidatedHeartbeat = String.join(";", allMessages);
        sendMulticastMessage("HEARTBEAT" + consolidatedHeartbeat);
    }

    @Override
    public void run() {
        if (!isLeader) return;

        while (true) {
            try {
                sendHeartbeat();
                applyTempUpdates();
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Método para aplicar atualizações pendentes
    private synchronized void applyTempUpdates() {
        for (String update : tempUpdates) {

            String[] parts = update.split(":");
            String documentId = parts[0].replace("SYNC ", "").trim();
            String content = parts[1].trim();
            long timestamp = Long.parseLong(parts[2].trim());


            messageList.addMessage(update);
            System.out.println(nodeId + " adicionou atualização pendente: " + documentId);
        }
        tempUpdates.clear();
    }


    // Método para enviar a mensagem por multicast
    private void sendMulticastMessage(String message) {
        try {
            byte[] plainMessage = message.getBytes();
            DatagramPacket packet = new DatagramPacket(plainMessage, plainMessage.length, InetAddress.getByName(MULTICAST_ADDRESS), PORT);
            socket.send(packet);
            System.out.println(nodeId + " enviou: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sendDocumentUpdateToNode(String nodeId, String documentId, String content) throws RemoteException {

        try {
            System.out.println("A enviar atualização do documento " + documentId + " para o nó " + nodeId);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Erro ao enviar atualização para o nó " + nodeId, e);
        }
    }


    public void sendPendingUpdateToNode(String nodeId, String updateMessage) throws RemoteException {

        try {
            System.out.println("A enviar atualização pendente para o nó " + nodeId);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Erro ao enviar atualização pendente para o nó " + nodeId, e);
        }
    }

    // Método para adicionar atualizações temporárias
    public synchronized void addTempUpdate(String updateMessage) {
        tempUpdates.add(updateMessage);
    }

    // Novo método: Envia uma notificação para um nó sobre a chegada de um novo nó
    public void sendNewNodeNotificationToNode(String activeNodeId, String newNodeId) throws RemoteException {
        try {
            String message = "NEW_NODE " + newNodeId;
            System.out.println("\nNotificação enviada para o nó " + activeNodeId + " sobre o novo nó " + newNodeId);
            sendMulticastMessageToNode(activeNodeId, message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Erro ao enviar notificação de novo nó para o nó " + activeNodeId, e);
        }
    }

    // Método auxiliar para enviar a mensagem de notificação para um nó específico via multicast
    private void sendMulticastMessageToNode(String nodeId, String message) throws RemoteException {
        try {
            byte[] plainMessage = message.getBytes();
            DatagramPacket packet = new DatagramPacket(plainMessage, plainMessage.length, InetAddress.getByName(MULTICAST_ADDRESS), PORT);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Erro ao enviar a mensagem multicast para o nó " + nodeId, e);
        }
    }
}
