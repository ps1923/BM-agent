package com.bmhs.guidance;

import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeepSeekClientTest {
    @Test
    void refusesToCallWhenModelSecretIsNotConfigured() {
        DeepSeekClient client = new DeepSeekClient("", "", "deepseek-chat", Duration.ofSeconds(2));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.complete("system", "user"));

        assertEquals("MODEL_NOT_CONFIGURED", exception.code());
    }

    @Test
    void rejectsEndpointsContainingUserInfo() {
        DeepSeekClient client = new DeepSeekClient("https://user:secret@example.com/v1", "key",
                "deepseek-chat", Duration.ofSeconds(2));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.complete("system", "user"));

        assertEquals("MODEL_NOT_CONFIGURED", exception.code());
    }

    @Test
    void rejectsPlainHttpForRemoteModelEndpoints() {
        DeepSeekClient client = new DeepSeekClient("http://api.example.com/v1", "key",
                "deepseek-chat", Duration.ofSeconds(2));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.complete("system", "user"));

        assertEquals("MODEL_NOT_CONFIGURED", exception.code());
    }

    @Test
    void enablesJsonModeOnlyForStructuredAcceptanceCalls() throws Exception {
        AtomicReference<JsonObject> received = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            received.set(JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject());
            byte[] body = "{\"model\":\"test-model\",\"choices\":[{\"message\":{\"content\":\"{}\"}}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            DeepSeekClient client = new DeepSeekClient("http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                    "test-api-key", "deepseek-chat", Duration.ofSeconds(3));

            client.completeJson("Return JSON.", "{}");
            assertEquals("json_object", received.get().getAsJsonObject("response_format").get("type").getAsString());

            client.complete("Return text.", "hello");
            assertFalse(received.get().has("response_format"));
        } finally {
            server.stop(0);
        }
    }
}
