package com.bmhs.rag;

import com.bmhs.experimentcreation.ApiException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
public class ChromaClient {
    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    private final String baseUrl;
    private final String collectionName;
    private final String apiKey;
    private final Duration timeout;
    private volatile String collectionId;

    public ChromaClient(@Value("${bm-hs.chroma.base-url:}") String baseUrl,
                        @Value("${bm-hs.chroma.collection:bm_hs_bugs}") String collectionName,
                        @Value("${bm-hs.chroma.api-key:}") String apiKey,
                        @Value("${bm-hs.chroma.timeout:PT20S}") Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Chroma 请求超时时间必须大于 0");
        }
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
        this.baseUrl = normalize(baseUrl);
        this.collectionName = collectionName == null || collectionName.isBlank() ? "bm_hs_bugs" : collectionName.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.timeout = timeout;
    }

    public void upsert(RagModels.IndexRequest request, List<Double> embedding) {
        String collection = collection();
        JsonObject payload = new JsonObject();
        payload.add("ids", gson.toJsonTree(List.of(request.documentId())));
        payload.add("embeddings", gson.toJsonTree(List.of(embedding)));
        payload.add("documents", gson.toJsonTree(List.of(request.document())));
        JsonObject metadata = new JsonObject();
        metadata.addProperty("course_id", request.courseId() == null ? "" : request.courseId().toString());
        metadata.addProperty("experiment_id", Long.toString(request.experimentId()));
        metadata.addProperty("node_id", request.nodeId() == null ? "" : request.nodeId().toString());
        metadata.addProperty("technology_stack", request.technologyStack() == null ? "" : request.technologyStack());
        metadata.addProperty("status", request.status());
        payload.add("metadatas", gson.toJsonTree(List.of(metadata)));
        send("/api/v1/collections/" + collection + "/upsert", payload, true);
    }

    public List<RagModels.ChromaHit> query(List<Double> embedding, Long courseId, int limit) {
        return query(embedding, courseId, null, limit);
    }

    public List<RagModels.ChromaHit> query(List<Double> embedding, Long courseId, Long experimentId, int limit) {
        return query(embedding, courseId, experimentId, null, limit);
    }

    public List<RagModels.ChromaHit> query(List<Double> embedding, Long courseId, Long experimentId,
                                           String technologyStack, int limit) {
        String collection = collection();
        JsonObject payload = new JsonObject();
        payload.add("query_embeddings", gson.toJsonTree(List.of(embedding)));
        payload.addProperty("n_results", limit);
        JsonArray conditions = new JsonArray();
        conditions.add(equalsCondition("course_id", courseId.toString()));
        if (experimentId != null) conditions.add(equalsCondition("experiment_id", experimentId.toString()));
        conditions.add(equalsCondition("status", "approved"));
        if (technologyStack != null && !technologyStack.isBlank()) {
            JsonArray stackOptions = new JsonArray();
            stackOptions.add(equalsCondition("technology_stack", technologyStack));
            stackOptions.add(equalsCondition("technology_stack", ""));
            JsonObject stackFilter = new JsonObject();
            stackFilter.add("$or", stackOptions);
            conditions.add(stackFilter);
        }
        JsonObject where = new JsonObject();
        where.add("$and", conditions);
        payload.add("where", where);
        JsonObject root = send("/api/v1/collections/" + collection + "/query", payload);
        JsonArray ids = firstArray(root, "ids");
        JsonArray distances = firstArray(root, "distances");
        List<RagModels.ChromaHit> hits = new ArrayList<>();
        if (ids == null) return hits;
        for (int i = 0; i < ids.size(); i++) {
            double distance = distances != null && i < distances.size() ? distances.get(i).getAsDouble() : Double.MAX_VALUE;
            hits.add(new RagModels.ChromaHit(ids.get(i).getAsString(), distance));
        }
        return hits;
    }

    private JsonObject equalsCondition(String field, String value) {
        JsonObject condition = new JsonObject();
        JsonObject equality = new JsonObject();
        equality.addProperty("$eq", value);
        condition.add(field, equality);
        return condition;
    }

    private String collection() {
        if (baseUrl.isBlank()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CHROMA_NOT_CONFIGURED", "Chroma 尚未配置");
        String existing = collectionId;
        if (existing != null) return existing;
        synchronized (this) {
            if (collectionId != null) return collectionId;
            JsonObject payload = new JsonObject();
            payload.addProperty("name", collectionName);
            payload.addProperty("get_or_create", true);
            JsonObject result = send("/api/v1/collections", payload);
            if (!result.has("id")) throw new ApiException(HttpStatus.BAD_GATEWAY, "CHROMA_RESPONSE_INVALID", "Chroma 返回内容无效");
            collectionId = result.get("id").getAsString();
            return collectionId;
        }
    }

    private JsonObject send(String path, JsonObject payload) {
        return send(path, payload, false);
    }

    private JsonObject send(String path, JsonObject payload, boolean allowNullResponse) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(timeout).header("Content-Type", "application/json");
            if (!apiKey.isBlank()) builder.header("Authorization", "Bearer " + apiKey);
            HttpResponse<String> response = httpClient.send(builder.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload))).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "CHROMA_REQUEST_FAILED", "Chroma 服务暂时不可用");
            }
            String responseBody = response.body();
            if (allowNullResponse && (responseBody == null || responseBody.isBlank() || "null".equals(responseBody.trim()))) {
                return new JsonObject();
            }
            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            if (root == null) throw new ApiException(HttpStatus.BAD_GATEWAY, "CHROMA_RESPONSE_INVALID", "Chroma 返回内容无效");
            return root;
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "CHROMA_TIMEOUT", "Chroma 请求超时");
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "CHROMA_REQUEST_FAILED", "Chroma 服务暂时不可用");
        }
    }

    private JsonArray firstArray(JsonObject root, String name) {
        if (!root.has(name) || !root.get(name).isJsonArray() || root.getAsJsonArray(name).isEmpty()) return null;
        return root.getAsJsonArray(name).get(0).getAsJsonArray();
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
}
