package com.bmhs.guidance;

import com.bmhs.experimentcreation.ApiException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class DeepSeekClient implements GuidanceModelClient {
    private static final int MAX_RESPONSE_CHARS = 20000;
    private final HttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public DeepSeekClient(
            @Value("${bm-hs.ai.base-url:}") String baseUrl,
            @Value("${bm-hs.ai.api-key:}") String apiKey,
            @Value("${bm-hs.ai.model:deepseek-chat}") String model,
            @Value("${bm-hs.ai.timeout:PT45S}") Duration timeout) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.gson = new Gson();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "deepseek-chat" : model.trim();
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("模型请求超时时间必须大于 0");
        }
        this.timeout = timeout;
    }

    @Override
    public GuidanceModels.ModelReply complete(String systemPrompt, String userPrompt) {
        if (baseUrl.isBlank() || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MODEL_NOT_CONFIGURED", "DeepSeek 尚未配置");
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);
        payload.add("messages", gson.toJsonTree(new Object[]{
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        }));
        payload.addProperty("temperature", 0.2);
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                    .build();
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MODEL_ENDPOINT_INVALID", "模型服务地址无效");
        }
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "MODEL_REQUEST_FAILED", "模型服务暂时不可用，请稍后重试");
            }
            return parseResponse(response.body());
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "MODEL_TIMEOUT", "模型请求超时，请稍后重试");
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "MODEL_REQUEST_FAILED", "模型服务暂时不可用，请稍后重试");
        }
    }

    private GuidanceModels.ModelReply parseResponse(String body) {
        try {
            JsonObject root = gson.fromJson(body, JsonObject.class);
            JsonArray choices = root == null ? null : root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) throw invalidResponse();
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            String content = message == null || !message.has("content") ? "" : message.get("content").getAsString();
            if (content.isBlank()) throw invalidResponse();
            if (content.length() > MAX_RESPONSE_CHARS) content = content.substring(0, MAX_RESPONSE_CHARS);
            String responseModel = root.has("model") ? root.get("model").getAsString() : model;
            return new GuidanceModels.ModelReply(content.trim(), responseModel);
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException exception) {
            throw invalidResponse();
        }
    }

    private ApiException invalidResponse() {
        return new ApiException(HttpStatus.BAD_GATEWAY, "MODEL_RESPONSE_INVALID", "模型返回内容无效，请稍后重试");
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = value.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        try {
            URI uri = URI.create(normalized);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getUserInfo() != null || uri.getHost() == null) return "";
            if ("http".equalsIgnoreCase(uri.getScheme()) && !isLoopback(uri.getHost())) return "";
        } catch (IllegalArgumentException exception) {
            return "";
        }
        return normalized;
    }

    private boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }
}
