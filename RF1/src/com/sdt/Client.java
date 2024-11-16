package com.sdt;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;

public class Client {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            LeaderInterface leader = (LeaderInterface) registry.lookup("Leader");

            String documentId = UUID.randomUUID().toString(); // Gera ID único
            leader.updateDocument(documentId, "Conteúdo inicial do documento");

            System.out.println("Documento enviado com ID: " + documentId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
