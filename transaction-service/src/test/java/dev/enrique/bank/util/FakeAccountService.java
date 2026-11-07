package dev.enrique.bank.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpServer;

/**
 * Stands in for account-service over real HTTP, so the Feign client, its headers, its JSON and the
 * circuit breaker fallback run as in production. Answers are set per path prefix with {@link #respond}.
 */
public class FakeAccountService {
    public record ReceivedRequest(String path, String authorization, String idempotencyKey, String body) {
    }

    private record Answer(int status, String body) {
    }

    private final HttpServer server;
    private final List<ReceivedRequest> received = new CopyOnWriteArrayList<>();
    private final Map<String, Answer> answers = new ConcurrentHashMap<>();

    public FakeAccountService() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            received.add(new ReceivedRequest(path,
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("Idempotency-Key"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            Answer answer = answers.entrySet().stream()
                    .filter(entry -> path.startsWith(entry.getKey()))
                    .max((a, b) -> Integer.compare(a.getKey().length(), b.getKey().length()))
                    .map(Map.Entry::getValue)
                    .orElse(new Answer(404, "{\"status\":404,\"message\":\"Not found\"}"));
            byte[] response = answer.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(answer.status(), response.length == 0 ? -1 : response.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(response);
            }
        });
        server.start();
    }

    public String url() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    public void respond(String pathPrefix, int status, String body) {
        answers.put(pathPrefix, new Answer(status, body));
    }

    public List<ReceivedRequest> received(String pathPrefix) {
        return received.stream().filter(request -> request.path().startsWith(pathPrefix)).toList();
    }

    public void reset() {
        received.clear();
        answers.clear();
    }
}
