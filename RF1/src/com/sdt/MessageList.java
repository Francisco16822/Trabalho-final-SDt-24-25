package com.sdt;

import java.util.ArrayList;
import java.util.List;

public class MessageList {
    private List<String> messages = new ArrayList<>();
    private List<String> pendingMessages = new ArrayList<>();


    public synchronized void addMessage(String message) {
        messages.add(message);
    }

    // Prepara as mensagens para envio e limpa a lista
    public synchronized List<String> createSendStructure() {
        List<String> preparedMessages = new ArrayList<>(messages);
        preparedMessages.addAll(pendingMessages);
        pendingMessages.clear();
        messages.clear();
        return preparedMessages;
    }

    // Método para comparar e resolver conflitos

    public synchronized String resolveConflicts(String existingMessage, String newMessage) {

        long existingTimestamp = Long.parseLong(existingMessage.split(":")[2]);
        long newTimestamp = Long.parseLong(newMessage.split(":")[2]);

        if (newTimestamp > existingTimestamp) {
            return newMessage;
        } else {
            return existingMessage;
        }
    }
}
