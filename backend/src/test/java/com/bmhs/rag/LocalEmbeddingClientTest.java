package com.bmhs.rag;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalEmbeddingClientTest {
    @Test
    void teiProtocolCallsEmbedAndParsesNestedVector() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/embed", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "[[0.1,0.2,0.3]]".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            LocalEmbeddingClient client = new LocalEmbeddingClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(), "", "BAAI/bge-small-zh-v1.5", "tei", Duration.ofSeconds(2));

            assertEquals(List.of(0.1, 0.2, 0.3), client.embed("启动失败"));
            assertEquals("{\"inputs\":\"启动失败\"}", requestBody.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void openAiProtocolRemainsBackwardCompatible() throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/embeddings", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            byte[] response = "{\"data\":[{\"embedding\":[0.4,0.5]}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            LocalEmbeddingClient client = new LocalEmbeddingClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(), "", "test-model", "openai", Duration.ofSeconds(2));

            assertEquals(List.of(0.4, 0.5), client.embed("hello"));
            assertEquals("/embeddings", path.get());
        } finally {
            server.stop(0);
        }
    }
}
