package com.sdt;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public class Node {
    private String nodeId;
    private MessageList messageList;
    private MessageReceiver receiver;
    private SendTransmitter sendTransmitter;
    private LeaderInterface leader;

    public Node(String nodeId, MessageList messageList) {
        this.nodeId = nodeId;
        this.messageList = messageList;
        //o start deve ser chamado aqui
    }

    public void start() {
        try {

            leader = (LeaderInterface) Naming.lookup("//localhost/Leader");


            leader.addNode(nodeId);


            synchronizeWithLeader();


            receiver = new MessageReceiver(nodeId, messageList);
            receiver.start();
            sendTransmitter = new SendTransmitter(nodeId, new Leader_RMI_Handler(nodeId, messageList), messageList);
            sendTransmitter.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void synchronizeWithLeader() {
        try {

            Map<String, String> documentVersions = leader.getDocumentVersions();
            List<String> pendingUpdates = leader.getPendingUpdates();
            System.out.println("A sincronizar documentos do líder...");


            for (Map.Entry<String, String> entry : documentVersions.entrySet()) {
                String documentId = entry.getKey();
                String content = entry.getValue();


                System.out.println("Documento Sincronizado: " + documentId + " -> " + content);


                messageList.addMessage("SYNC " + documentId + ":" + content + ":" + System.currentTimeMillis(), true);
            }


            for (String update : pendingUpdates) {
                System.out.println("Aplicação de atualização pendente: " + update);
                messageList.addMessage(update, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Método principal para inicializar o nó
    public static void main(String[] args) {

        Node node = new Node("Node1", new MessageList());
        node.start();


        try {
            Thread.sleep(5000);
            //node.shutdown();  // Simula a saída do nó
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
