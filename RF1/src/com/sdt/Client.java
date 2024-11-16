package com.sdt;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;

public class Client {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            LeaderInterface leader = (LeaderInterface) registry.lookup("Leader");

            String documentId = "[ Doc1 ]";
            String content = " Versão Inicial ";
            String requestId = UUID.randomUUID().toString();

            System.out.println("Enviando documento ao líder: " + documentId + " com Request ID: " + requestId);
            leader.updateDocument(documentId, content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
