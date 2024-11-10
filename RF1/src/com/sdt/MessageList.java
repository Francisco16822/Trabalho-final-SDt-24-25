package com.sdt;

import java.util.ArrayList;
import java.util.List;

public class MessageList {
    private List<String> messages = new ArrayList<>();

    public synchronized void addMessage(String message) {
        messages.add(message);
    }

    public synchronized void removeMessage(String message) {
        messages.remove(message);
    }

    // Retorna uma cópia das mensagens, garantindo segurança de leitura
    public synchronized List<String> getClone() {
        return new ArrayList<>(messages);
    }

    public synchronized List<String> createSendStructure() {
        List<String> preparedMessages = new ArrayList<>(messages);
        messages.clear();
        return preparedMessages;
    }
}
