package dev.enrique.bank.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpServer;

/**
 * Stands in for account-service over real HTTP, so the Feign client, its JSON and the circuit
 * breaker fallback run as in production. Every request gets the answer set with {@link #respond}.
 */
public class FakeAccountService {
    public record ReceivedRequest(String path, String body) {
    }

    private final HttpServer server;
    private final List<ReceivedRequest> received = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String body = "{}";

    public FakeAccountService() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", exchange -> {
            received.add(new ReceivedRequest(exchange.getRequestURI().getPath(),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length == 0 ? -1 : response.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(response);
            }
        });
        server.start();
    }

    public String url() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    public void respond(int status, String body) {
        this.status = status;
        this.body = body;
    }

    public List<ReceivedRequest> received() {
        return received;
    }

    public void reset() {
        received.clear();
        respond(200, "{}");
    }
}
