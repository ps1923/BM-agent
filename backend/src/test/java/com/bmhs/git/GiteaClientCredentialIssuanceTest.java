package com.bmhs.git;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiteaClientCredentialIssuanceTest {
    @Test
    void recoversPartialProvisioningAndCreatesRepositoryScopedTokenWithUserPassword() throws Exception {
        AtomicReference<String> resetPassword = new AtomicReference<>();
        AtomicReference<String> tokenAuthorization = new AtomicReference<>();
        AtomicReference<JsonObject> tokenRequest = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/", exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (method.equals("POST") && path.equals("/api/v1/admin/users")) {
                respond(exchange, 409, "{}");
            } else if (method.equals("PATCH") && path.equals("/api/v1/admin/users/test-owner")) {
                JsonObject body = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                resetPassword.set(body.get("password").getAsString());
                respond(exchange, 200, "{}");
            } else if (method.equals("POST") && path.equals("/api/v1/admin/users/test-owner/repos")) {
                respond(exchange, 409, "{}");
            } else if (method.equals("GET") && path.equals("/api/v1/repos/test-owner/test-repo")) {
                respond(exchange, 200, "{\"id\":77,\"private\":true}");
            } else if (method.equals("GET") && path.equals("/api/v1/repos/test-owner/test-repo/hooks")) {
                respond(exchange, 200, "[]");
            } else if (method.equals("POST") && path.equals("/api/v1/repos/test-owner/test-repo/hooks")) {
                respond(exchange, 201, "{}");
            } else if (method.equals("POST") && path.equals("/api/v1/users/test-owner/tokens")) {
                tokenAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                tokenRequest.set(JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject());
                respond(exchange, 201, "{\"sha1\":\"scoped-git-token\"}");
            } else {
                respond(exchange, 404, "{}");
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            String localBase = "http://127.0.0.1:" + port;
            GitConfig config = new GitConfig(localBase, localBase, "admin-api-token",
                    "w".repeat(40), localBase + "/api/integrations/gitea/webhook",
                    Base64.getEncoder().encodeToString(new byte[32]));
            GiteaClient client = new GiteaClient(config);

            GitModels.GiteaRepository repository = client.ensureRepository(9L, 4L, "test-owner", "test-repo",
                    localBase + "/api/integrations/gitea/webhook", "w".repeat(40), true);
            String token = client.createGitToken("test-owner", 9L, repository.bootstrapPassword());

            assertEquals(77L, repository.id());
            assertNotNull(repository.bootstrapPassword());
            assertNotEquals("admin-api-token", repository.bootstrapPassword());
            assertEquals(repository.bootstrapPassword(), resetPassword.get());
            assertEquals("scoped-git-token", token);
            String expectedBasic = "Basic " + Base64.getEncoder().encodeToString(
                    ("test-owner:" + repository.bootstrapPassword()).getBytes(StandardCharsets.UTF_8));
            assertEquals(expectedBasic, tokenAuthorization.get());
            assertEquals("bmhs-run-9-git-push", tokenRequest.get().get("name").getAsString());
            assertTrue(tokenRequest.get().getAsJsonArray("scopes").toString().contains("read:repository"));
            assertTrue(tokenRequest.get().getAsJsonArray("scopes").toString().contains("write:repository"));
        } finally {
            server.stop(0);
        }
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
