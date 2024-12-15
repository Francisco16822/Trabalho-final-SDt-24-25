package com.sdt;


public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].matches("[0-9]+")) {
            String nodeId = "Node" + args[0];
            MessageList messageList = new MessageList();
            Node node = new Node(nodeId, messageList);
            node.start();
        } else {
            System.out.println("Argumento esperado: identificador de nó (ex: '1', '2', ...)");

        }
    }
}
