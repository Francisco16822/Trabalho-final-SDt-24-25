package RF1.src.com.sdt;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;

public class SendTransmitter extends Thread {
    private static final String MULTICAST_ADDRESS = "224.0.0.1";
    private static final int PORT = 5555;
    private String nodeId;
    private Leader leader;
    private MessageList messageList;

    public SendTransmitter(String nodeId, Leader leader, MessageList messageList) {
        this.nodeId = nodeId;
        this.leader = leader;
        this.messageList = messageList;
    }

    public void sendDocumentUpdate(String documentId, String content) {
        String updateMessage = "DOC_UPDATE:" + documentId + ":" + content;
        messageList.addMessage(updateMessage);
    }

    public void sendCommit(String documentId) {
        String commitMessage = "COMMIT:" + documentId;
        messageList.addMessage(commitMessage);
        System.out.println("Documento " + documentId + " tornado permanente.");
    }

    @Override
    public void run() {
        while (true) {
            try {
                List<String> messagesToSend = messageList.createSendStructure();

                for (String message : messagesToSend) {
                    sendMulticastMessage(message);
                }

                Thread.sleep(5000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMulticastMessage(String message) {
        try (MulticastSocket socket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
            System.out.println(nodeId + " enviou: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
