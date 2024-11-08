package com.sdt;

public class Main {
    public static void main(String[] args) {

        if (args.length > 0 && args[0].equals("leader")) {
            // Inicia o líder
            Node leader = new Node("Leader", true);
            leader.start();
        } else if (args.length > 0 && args[0].matches("[0-9]+")) {
            // Inicia um nó comum
            Node node = new Node("Node" + args[0], false);
            node.start();
        } else {
            System.out.println("Por favor, forneça o argumento 'leader' ou um identificador de nó (ex: 1, 2, etc.)");
        }
    }
}