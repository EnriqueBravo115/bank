package dev.enrique.bank.util;

import java.util.List;
import java.util.Map;

import org.springframework.web.client.RestClient;

/** Reads the emails Keycloak sent to the Mailpit test container. */
public class Mailbox {
    private final RestClient client;

    public Mailbox(String mailpitUrl) {
        this.client = RestClient.create(mailpitUrl);
    }

    public void clear() {
        client.delete().uri("/api/v1/messages").retrieve().toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    public long countTo(String address) {
        Map<String, Object> body = client.get().uri("/api/v1/messages").retrieve().body(Map.class);
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
        return messages.stream()
                .map(message -> (List<Map<String, Object>>) message.get("To"))
                .filter(to -> to.stream().anyMatch(recipient -> address.equalsIgnoreCase((String) recipient.get("Address"))))
                .count();
    }
}
