package com.sdt;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("leader")) {
            try {
                MessageList messageList = new MessageList();
                Leader_RMI_Handler leader = new Leader_RMI_Handler("Leader", messageList);
                Registry registry = LocateRegistry.createRegistry(1099);
                registry.rebind("Leader", leader);

                new SendTransmitter("Leader", leader, messageList).start();


                //leader.updateDocument("doc1", "Conteúdo do documento 1");
                //leader.updateDocument("doc2", "Conteúdo do documento 2");



            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (args.length > 0 && args[0].matches("[0-9]+")) {
            MessageList messageList = new MessageList();
            Node node = new Node("Node" + args[0], messageList);
            node.start();
        } else {
            System.out.println("Argumento esperado: 'leader' ou identificador de nó.");
        }
    }
}
