package com.sdt;

public class Main {
    public static void main(String[] args) {

        if (args.length > 0 && args[0].equals("leader")) {
            // Cria uma instância compartilhada do MessageList para o líder
            MessageList messageList = new MessageList();

            // Inicia o líder
            Node leaderNode = new Node("Leader", true, messageList);
            leaderNode.start();

            // Simula uma atualização de documento logo após o início para testar o fluxo de mensagens
            Leader leader = leaderNode.getLeader();
            if (leader != null) {
                try {
                    leader.updateDocument("Doc1", "Conteúdo inicial do documento");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else if (args.length > 0 && args[0].matches("[0-9]+")) {
            // Cria uma instância compartilhada do MessageList para o nó
            MessageList messageList = new MessageList();

            // Inicia o nó
            Node node = new Node("Node" + args[0], false, messageList);
            node.start();

        } else {
            System.out.println("Por favor, forneça o argumento 'leader' ou um identificador de nó (ex: 1, 2, etc.)");
        }
    }
}