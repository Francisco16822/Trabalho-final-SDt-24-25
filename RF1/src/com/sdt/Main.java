package com.sdt;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("leader")) {
            MessageList messageList = new MessageList();
            Node leaderNode = new Node("Leader", true, messageList);
            leaderNode.start();

            Leader leader = leaderNode.getLeader();
            if (leader != null) {
                try {
                    Thread.sleep(2000);  // Pausa para inicialização dos nós
                    leader.updateDocument("Doc1", "Versão Inicial");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (args.length > 0 && args[0].matches("[0-9]+")) {
            MessageList messageList = new MessageList();
            Node node = new Node("Node" + args[0], false, messageList);
            node.start();
        } else {
            System.out.println("Argumento esperado: 'leader' ou identificador de nó.");
        }
    }
}
