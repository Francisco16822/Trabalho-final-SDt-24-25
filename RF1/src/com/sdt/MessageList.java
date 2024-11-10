package RF1.src.com.sdt;

import java.util.ArrayList;
import java.util.List;

public class MessageList {
    private List<String> messages = new ArrayList<>();

    // Adiciona uma mensagem de forma sincronizada
    public synchronized void addMessage(String message) {
        messages.add(message);
    }

    // Remove uma mensagem específica de forma sincronizada
    public synchronized void removeMessage(String message) {
        messages.remove(message);
    }

    // Retorna uma cópia das mensagens, garantindo segurança de leitura
    public synchronized List<String> getClone() {
        return new ArrayList<>(messages);
    }

    // Prepara as mensagens para envio e limpa a lista original
    public synchronized List<String> createSendStructure() {
        List<String> preparedMessages = new ArrayList<>(messages);
        messages.clear();
        return preparedMessages;
    }
}
