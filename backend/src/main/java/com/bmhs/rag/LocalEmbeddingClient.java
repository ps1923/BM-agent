package com.bmhs.rag;

import com.bmhs.experimentcreation.ApiException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class LocalEmbeddingClient implements EmbeddingClient {
    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String protocol;
    private final Duration timeout;

    public LocalEmbeddingClient(
            @Value("${bm-hs.embedding.base-url:}") String baseUrl,
            @Value("${bm-hs.embedding.api-key:}") String apiKey,
            @Value("${bm-hs.embedding.model:BAAI/bge-small-zh-v1.5}") String model,
            @Value("${bm-hs.embedding.protocol:openai}") String protocol,
            @Value("${bm-hs.embedding.timeout:PT20S}") Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Embedding 请求超时时间必须大于 0");
        }
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.baseUrl = normalize(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "BAAI/bge-small-zh-v1.5" : model.trim();
        this.protocol = "tei".equalsIgnoreCase(protocol == null ? "" : protocol.trim()) ? "tei" : "openai";
        this.timeout = timeout;
    }

    @Override
    public List<Double> embed(String text) {
        if (baseUrl.isBlank()) throw unavailable("EMBEDDING_NOT_CONFIGURED", "本地 Embedding 服务尚未配置");
        JsonObject payload = new JsonObject();
        String path;
        if ("tei".equals(protocol)) {
            payload.addProperty("inputs", text);
            path = "/embed";
        } else {
            payload.addProperty("model", model);
            payload.addProperty("input", text);
            path = "/embeddings";
        }
        HttpRequest.Builder builder;
        try {
            builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(timeout).header("Content-Type", "application/json");
        } catch (IllegalArgumentException exception) {
            throw unavailable("EMBEDDING_ENDPOINT_INVALID", "Embedding 服务地址无效");
        }
        if (!apiKey.isBlank()) builder.header("Authorization", "Bearer " + apiKey);
        try {
            HttpResponse<String> response = httpClient.send(builder.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload))).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw unavailable("EMBEDDING_REQUEST_FAILED", "Embedding 服务暂时不可用");
            }
            JsonElement root = gson.fromJson(response.body(), JsonElement.class);
            JsonArray vector = vector(root);
            if (vector == null || vector.isEmpty()) throw unavailable("EMBEDDING_RESPONSE_INVALID", "Embedding 返回内容无效");
            List<Double> result = new ArrayList<>(vector.size());
            vector.forEach(value -> result.add(value.getAsDouble()));
            return result;
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable("EMBEDDING_TIMEOUT", "Embedding 请求超时");
        } catch (Exception exception) {
            throw unavailable("EMBEDDING_REQUEST_FAILED", "Embedding 服务暂时不可用");
        }
    }

    private JsonArray vector(JsonElement root) {
        if (root == null || root.isJsonNull()) return null;
        if ("tei".equals(protocol)) {
            if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) return null;
            JsonElement first = root.getAsJsonArray().get(0);
            return first != null && first.isJsonArray() ? first.getAsJsonArray() : null;
        }
        if (!root.isJsonObject() || !root.getAsJsonObject().has("data")) return null;
        JsonArray data = root.getAsJsonObject().getAsJsonArray("data");
        if (data == null || data.isEmpty() || !data.get(0).isJsonObject()) return null;
        return data.get(0).getAsJsonObject().getAsJsonArray("embedding");
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = value.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        try {
            URI uri = URI.create(normalized);
            boolean secure = "https".equalsIgnoreCase(uri.getScheme());
            boolean loopback = "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()) || "::1".equals(uri.getHost());
            if ((!secure && !loopback) || uri.getHost() == null || uri.getUserInfo() != null) return "";
        } catch (IllegalArgumentException exception) {
            return "";
        }
        return normalized;
    }

    private ApiException unavailable(String code, String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }
}
