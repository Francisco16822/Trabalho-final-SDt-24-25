package RF1.src.com.sdt;

public class Main {
    public static void main(String[] args) {

        // Verifica se o primeiro argumento é "leader" ou um identificador de nó (ex: "1", "2")
        if (args.length > 0 && args[0].equals("leader")) {
            // Cria um MessageList compartilhado
            MessageList messageList = new MessageList();

            // Inicia o líder
            Node leaderNode = new Node("Leader", true, messageList);
            leaderNode.start();

        } else if (args.length > 0 && args[0].matches("[0-9]+")) {
            // Cria um MessageList compartilhado
            MessageList messageList = new MessageList();

            // Inicia um nó comum
            Node node = new Node("Node" + args[0], false, messageList);
            node.start();

        } else {
            System.out.println("Por favor, forneça o argumento 'leader' ou um identificador de nó (ex: 1, 2, etc.)");
        }
    }
}
