package com.bmhs.git;

import com.bmhs.experimentcreation.ApiException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class GiteaClient implements GiteaGateway {
    private static final int MAX_TREE_ENTRIES = 5_000;
    private static final int MAX_TEXT_FILES = 500;
    private static final long MAX_FILE_BYTES = 1_000_000L;
    private static final long MAX_TOTAL_BYTES = 20_000_000L;
    private final GitConfig config;
    private final HttpClient client;
    private final Gson gson = new Gson();

    public GiteaClient(GitConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public GitModels.GiteaRepository ensureRepository(long runId, long studentId, String owner, String name,
                                                       String webhookUrl, String webhookSecret,
                                                       boolean prepareCredential) {
        requireConfigured();
        String bootstrapPassword = randomBootstrapPassword();
        JsonObject user = new JsonObject();
        user.addProperty("username", owner);
        user.addProperty("email", "bmhs-run-" + runId + "@users.invalid");
        user.addProperty("full_name", "BM-HS run " + runId);
        user.addProperty("password", bootstrapPassword);
        user.addProperty("must_change_password", false);
        user.addProperty("restricted", true);
        user.addProperty("visibility", "private");
        user.addProperty("send_notify", false);
        ApiResponse createUser = request("POST", "/admin/users", config.adminToken(), null, user);
        if (createUser.status() == 409 && prepareCredential) {
            ApiResponse resetPassword = request("PATCH", "/admin/users/" + segment(owner),
                    config.adminToken(), null, Map.of("password", bootstrapPassword));
            if (resetPassword.status() != 200) throw giteaUnavailable();
        } else if (createUser.status() != 201 && createUser.status() != 409) {
            throw giteaUnavailable();
        }

        ApiResponse repoResponse = request("POST", "/admin/users/" + segment(owner) + "/repos",
                config.adminToken(), null, Map.of("name", name, "private", true,
                        "auto_init", false, "default_branch", "main", "description", "BM-HS experiment run " + runId));
        JsonObject repo;
        if (repoResponse.status() == 201) repo = parseObject(repoResponse.body());
        else if (repoResponse.status() == 409) {
            ApiResponse existing = request("GET", "/repos/" + segment(owner) + "/" + segment(name), config.adminToken(), null, null);
            if (existing.status() != 200) throw giteaUnavailable();
            repo = parseObject(existing.body());
        } else throw giteaUnavailable();
        if (!repo.has("private") || !repo.get("private").getAsBoolean()) throw giteaUnavailable();
        long repoId = requiredLong(repo, "id");
        if (repoId < 1) throw giteaUnavailable();
        ensurePushWebhook(owner, name, webhookUrl, webhookSecret);
        return new GitModels.GiteaRepository(repoId, owner, name, prepareCredential ? bootstrapPassword : null);
    }

    @Override
    public String createGitToken(String username, long runId, String bootstrapPassword) {
        if (bootstrapPassword == null || bootstrapPassword.isBlank()) throw giteaUnavailable();
        JsonObject body = new JsonObject();
        body.addProperty("name", "bmhs-run-" + runId + "-git-push");
        JsonArray scopes = new JsonArray();
        scopes.add("read:repository");
        scopes.add("write:repository");
        body.add("scopes", scopes);
        ApiResponse response = requestBasic("POST", "/users/" + segment(username) + "/tokens",
                username, bootstrapPassword, body);
        if (response.status() != 201) throw giteaUnavailable();
        JsonObject parsed = parseObject(response.body());
        String token = firstString(parsed, "sha1", "token");
        if (token.isBlank()) throw giteaUnavailable();
        return token;
    }

    @Override
    public GitModels.CommitInfo commit(String owner, String repo, String sha, String token) {
        ApiResponse response = request("GET", "/repos/" + segment(owner) + "/" + segment(repo)
                + "/git/commits/" + segment(sha), token, null, null);
        if (response.status() == 404) throw new ApiException(HttpStatus.BAD_REQUEST, "GIT_COMMIT_NOT_PUSHED", "该 commit 尚未推送到此实验仓库");
        if (response.status() != 200) throw giteaUnavailable();
        try {
            JsonObject root = parseObject(response.body());
            JsonObject commit = root.getAsJsonObject("commit");
            JsonObject author = commit == null ? null : commit.getAsJsonObject("author");
            JsonObject tree = commit == null ? null : commit.getAsJsonObject("tree");
            JsonArray parents = root.getAsJsonArray("parents");
            List<String> parentShas = new ArrayList<>();
            if (parents != null) for (JsonElement parent : parents) parentShas.add(parent.getAsJsonObject().get("sha").getAsString());
            Instant committedAt = parseInstant(author == null ? null : string(author, "date"));
            return new GitModels.CommitInfo(string(root, "sha"), string(tree, "sha"),
                    string(commit, "message"), string(author, "name"), committedAt, List.copyOf(parentShas));
        } catch (RuntimeException exception) {
            if (exception instanceof ApiException apiException) throw apiException;
            throw giteaUnavailable();
        }
    }

    @Override
    public List<GitModels.CommitFile> filesAtCommit(String owner, String repo, GitModels.CommitInfo commit, String token) {
        ApiResponse treeResponse = request("GET", "/repos/" + segment(owner) + "/" + segment(repo)
                + "/git/trees/" + segment(commit.treeSha()) + "?recursive=true", token, null, null);
        if (treeResponse.status() != 200) throw giteaUnavailable();
        JsonObject treeRoot = parseObject(treeResponse.body());
        JsonArray entries = treeRoot.getAsJsonArray("tree");
        if (treeRoot.has("truncated") && treeRoot.get("truncated").getAsBoolean()) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "GIT_COMMIT_TOO_LARGE", "commit 文件树被仓库服务截断，无法保证完整验收");
        }
        if (entries == null || entries.size() > MAX_TREE_ENTRIES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "GIT_COMMIT_TOO_LARGE", "commit 文件数量超过验收上限");
        }
        List<GitModels.CommitFile> files = new ArrayList<>();
        long totalBytes = 0;
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            if (!"blob".equals(string(entry, "type"))) continue;
            String path = string(entry, "path");
            if (!candidateTextPath(path)) continue;
            long size = entry.has("size") && !entry.get("size").isJsonNull() ? entry.get("size").getAsLong() : 0;
            if (path.isBlank() || size < 0) continue;
            if (size > MAX_FILE_BYTES) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "GIT_COMMIT_FILE_TOO_LARGE", "commit 中存在超过验收上限的单个文件");
            totalBytes += size;
            if (totalBytes > MAX_TOTAL_BYTES) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "GIT_COMMIT_TOO_LARGE", "commit 总大小超过验收上限");
            ApiResponse blobResponse = request("GET", "/repos/" + segment(owner) + "/" + segment(repo)
                    + "/git/blobs/" + segment(string(entry, "sha")), token, null, null);
            if (blobResponse.status() != 200) throw giteaUnavailable();
            JsonObject blob = parseObject(blobResponse.body());
            String encoding = string(blob, "encoding");
            if (!"base64".equalsIgnoreCase(encoding)) continue;
            byte[] content;
            try { content = Base64.getMimeDecoder().decode(string(blob, "content")); }
            catch (IllegalArgumentException exception) { continue; }
            if (content.length != size) continue;
            String text;
            try { text = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(content)).toString(); }
            catch (Exception exception) { continue; }
            if (containsControlCharacters(text)) continue;
            files.add(new GitModels.CommitFile(path, string(entry, "sha"), text));
            if (files.size() > MAX_TEXT_FILES) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "GIT_COMMIT_TOO_MANY_TEXT_FILES", "commit 可分析文本文件超过验收上限");
        }
        return List.copyOf(files);
    }

    private void ensurePushWebhook(String owner, String repo, String url, String secret) {
        String path = "/repos/" + segment(owner) + "/" + segment(repo) + "/hooks";
        ApiResponse list = request("GET", path, config.adminToken(), null, null);
        if (list.status() != 200) throw giteaUnavailable();
        JsonArray hooks = gson.fromJson(list.body(), JsonArray.class);
        Long existingHookId = null;
        if (hooks != null) for (JsonElement item : hooks) {
            JsonObject hook = item.getAsJsonObject();
            JsonObject hookConfig = hook.getAsJsonObject("config");
            if (hookConfig != null && url.equals(string(hookConfig, "url"))) {
                existingHookId = hook.get("id").getAsLong();
                break;
            }
        }
        JsonObject configData = new JsonObject();
        configData.addProperty("url", url);
        configData.addProperty("content_type", "json");
        configData.addProperty("secret", secret);
        JsonArray events = new JsonArray();
        events.add("push");
        JsonObject hook = new JsonObject();
        hook.addProperty("type", "gitea");
        hook.add("config", configData);
        hook.add("events", events);
        hook.addProperty("active", true);
        ApiResponse created = existingHookId == null
                ? request("POST", path, config.adminToken(), null, hook)
                : request("PATCH", path + "/" + existingHookId, config.adminToken(), null, hook);
        if (existingHookId == null ? created.status() != 201 : created.status() < 200 || created.status() >= 300) {
            throw giteaUnavailable();
        }
    }

    private ApiResponse request(String method, String path, String token, String sudo, Object body) {
        return send(method, path, "token " + token, sudo, body);
    }

    private ApiResponse requestBasic(String method, String path, String username, String password, Object body) {
        String credentials = Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return send(method, path, "Basic " + credentials, null, body);
    }

    private ApiResponse send(String method, String path, String authorization, String sudo, Object body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(config.apiBaseUrl() + "/api/v1" + path))
                    .timeout(Duration.ofSeconds(20)).header("Authorization", authorization)
                    .header("Accept", "application/json");
            if (sudo != null) builder.header("Sudo", sudo);
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type", "application/json").method(method,
                    HttpRequest.BodyPublishers.ofString(gson.toJson(body)));
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(response.statusCode(), response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw giteaUnavailable();
        } catch (Exception exception) {
            throw giteaUnavailable();
        }
    }

    private String randomBootstrapPassword() {
        byte[] bytes = new byte[36];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "aA1!";
    }

    private void requireConfigured() {
        if (!config.configured() || !isAllowedPublicUrl(config.publicBaseUrl())
                || !isAllowedPublicUrl(config.webhookTargetUrl())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GIT_REPOSITORY_NOT_CONFIGURED",
                    "Gitea 尚未完成配置；仓库地址与凭据未发放");
        }
        URI api = URI.create(config.apiBaseUrl());
        // Compose deployments use http://gitea:3000 on the isolated gitea-net. Do not
        // allow arbitrary cleartext API hosts because the admin token travels on this link.
        if (!List.of("http", "https").contains(api.getScheme()) || api.getHost() == null || api.getUserInfo() != null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GIT_REPOSITORY_NOT_CONFIGURED", "Gitea API 地址配置无效");
        }
        if ("http".equalsIgnoreCase(api.getScheme()) && !isLoopback(api.getHost())
                && !"gitea".equalsIgnoreCase(api.getHost())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GIT_REPOSITORY_NOT_CONFIGURED", "Gitea HTTP API 仅允许 loopback 或隔离网络服务名 gitea");
        }
        if (config.webhookSecret().length() < 32) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "GIT_WEBHOOK_NOT_CONFIGURED", "Gitea webhook 密钥长度不足");
    }

    private boolean isLoopback(String host) { return host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1"); }
    static boolean isAllowedPublicUrl(String value) {
        if (value == null) return false;
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        }
        catch (IllegalArgumentException exception) { return false; }
    }
    private static String segment(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private String firstString(JsonObject object, String... names) {
        for (String name : names) if (object.has(name) && !object.get(name).isJsonNull()) return object.get(name).getAsString();
        return "";
    }
    private String string(JsonObject object, String name) {
        return object == null || !object.has(name) || object.get(name).isJsonNull() ? "" : object.get(name).getAsString();
    }
    private long requiredLong(JsonObject object, String name) { return object.has(name) ? object.get(name).getAsLong() : -1L; }
    private Instant parseInstant(String value) { try { return value == null || value.isBlank() ? null : Instant.parse(value); } catch (Exception ignored) { return null; } }
    private JsonObject parseObject(String body) { try { return gson.fromJson(body, JsonObject.class); } catch (RuntimeException exception) { throw giteaUnavailable(); } }
    private boolean containsControlCharacters(String value) {
        return value.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
                && codePoint != '\n' && codePoint != '\r' && codePoint != '\t' && codePoint != '\f');
    }
    private boolean candidateTextPath(String path) {
        if (path == null || path.isBlank() || path.startsWith("/") || path.indexOf('\\') >= 0) return false;
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        for (String part : lower.split("/")) {
            if (part.equals("..") || part.equals(".") || List.of(".git", "node_modules", "target", "build", "dist", "out",
                    ".venv", "venv", "__pycache__", ".idea", ".vscode", "vendor", ".gradle").contains(part)) return false;
        }
        String name = lower.substring(lower.lastIndexOf('/') + 1);
        if (List.of(".env", ".env.local", ".npmrc", ".pypirc", "id_rsa", "id_ed25519", "credentials",
                "secrets.yml", "secrets.yaml", "service-account.json").contains(name) || name.startsWith(".env.")) return false;
        if (List.of("pom.xml", "package.json", "requirements.txt", "pyproject.toml", "makefile", "dockerfile").contains(name)) return true;
        int dot = name.lastIndexOf('.');
        return dot > 0 && List.of("java", "py", "js", "jsx", "ts", "tsx", "html", "css", "json", "xml", "yml",
                "yaml", "properties", "sql", "md", "txt", "toml", "gradle", "kts", "sh", "vue", "svelte")
                .contains(name.substring(dot + 1));
    }
    private ApiException giteaUnavailable() { return new ApiException(HttpStatus.BAD_GATEWAY, "GITEA_REQUEST_FAILED", "代码仓库服务暂时不可用"); }
    private record ApiResponse(int status, String body) {}
}
