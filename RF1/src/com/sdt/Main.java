package com.sdt;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("leader")) {
            Node leaderNode = new Node("Leader", true);
            leaderNode.start();
        } else if (args.length > 0 && args[0].matches("[0-9]+")) {
            Node node = new Node("Node" + args[0], false);
            node.start();
        } else {
            System.out.println("Argumento esperado: 'leader' ou identificador de nó.");
        }
    }
}
