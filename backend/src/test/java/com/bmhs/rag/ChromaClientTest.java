package com.bmhs.rag;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChromaClientTest {
    @Test
    void queryUsesScopedAndFilterForCourseExperimentAndApproval() throws Exception {
        AtomicReference<String> queryBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/collections", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (exchange.getRequestURI().getPath().endsWith("/query")) queryBody.set(body);
            String response = exchange.getRequestURI().getPath().endsWith("/query")
                    ? "{\"ids\":[[\"bug-1\"]],\"distances\":[[0.2]]}"
                    : "{\"id\":\"collection-1\"}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            ChromaClient client = new ChromaClient("http://127.0.0.1:" + server.getAddress().getPort(),
                    "test_collection", "", Duration.ofSeconds(2));

            List<RagModels.ChromaHit> hits = client.query(List.of(0.1), 4L, 2L, 8);

            assertEquals(List.of(new RagModels.ChromaHit("bug-1", 0.2)), hits);
            assertTrue(queryBody.get().contains("\"$and\""));
            assertTrue(queryBody.get().contains("\"course_id\""));
            assertTrue(queryBody.get().contains("\"experiment_id\""));
            assertTrue(queryBody.get().contains("\"approved\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void upsertAcceptsChromaNullSuccessResponse() throws Exception {
        AtomicReference<String> upsertBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/collections", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/upsert")) {
                upsertBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] response = "null".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } else {
                byte[] response = "{\"id\":\"collection-1\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
            exchange.close();
        });
        server.start();
        try {
            ChromaClient client = new ChromaClient("http://127.0.0.1:" + server.getAddress().getPort(),
                    "test_collection", "", Duration.ofSeconds(2));

            client.upsert(new RagModels.IndexRequest("bug-1", "问题\n解决", 4L, 2L, 8L,
                    "Java/Spring Boot", "approved"), List.of(0.1, 0.2));

            assertTrue(upsertBody.get().contains("\"ids\":[\"bug-1\"]"));
        } finally {
            server.stop(0);
        }
    }
}
