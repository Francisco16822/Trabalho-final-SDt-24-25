package com.sdt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessageList {
    private final CopyOnWriteArrayList<String> messages = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> pendingMessages = new CopyOnWriteArrayList<>();

    public void addMessage(String message) {
        messages.add(message);
    }

    public synchronized List<String> createSendStructure() {
        List<String> preparedMessages = new ArrayList<>(messages);
        preparedMessages.addAll(pendingMessages);
        pendingMessages.clear();
        messages.clear();
        return preparedMessages;
    }

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