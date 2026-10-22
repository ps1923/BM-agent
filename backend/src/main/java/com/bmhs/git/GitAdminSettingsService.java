package com.bmhs.git;

import com.bmhs.auth.AuthContext;
import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.bmhs.git.GitAdminSettingsModels.SettingsRequest;
import static com.bmhs.git.GitAdminSettingsModels.SettingsView;

@Service
public class GitAdminSettingsService {
    private static final Pattern HOSTNAME = Pattern.compile("(?i)(?=.{1,253}$)[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?");
    private final GitSettingsRepository repository;
    private final GitConfig config;
    private final GitCredentialCipher cipher;

    public GitAdminSettingsService(GitSettingsRepository repository, GitConfig config, GitCredentialCipher cipher) {
        this.repository = repository;
        this.config = config;
        this.cipher = cipher;
    }

    public SettingsView get(AuthenticatedUser user) {
        requireAdmin(user);
        String source = config.databaseSettingsPresent() ? "database"
                : config.configured() ? "environment" : "unconfigured";
        String updatedAt = repository.find().map(GitSettingsRepository.StoredSettings::updatedAt).orElse(null);
        return new SettingsView(config.apiBaseUrl(), config.publicBaseUrl(), config.webhookTargetUrl(),
                !config.adminToken().isBlank(), !config.webhookSecret().isBlank(), config.configured(), source, updatedAt);
    }

    @Transactional
    public SettingsView save(AuthenticatedUser user, SettingsRequest request) {
        requireAdmin(user);
        if (request == null) throw badRequest("配置内容不能为空");

        String apiUrl = normalize(validateInternalApiUrl(request.apiBaseUrl()));
        String publicUrl = normalize(validatePublicHttpsUrl(request.publicBaseUrl(), "公网克隆地址"));
        String webhookUrl = validatePublicHttpsUrl(request.webhookTargetUrl(), "Webhook 地址");

        String adminToken = request.clearAdminToken() ? "" : preserveOrReplace(request.adminToken(), config.adminToken());
        String webhookSecret = request.clearWebhookSecret() ? "" : preserveOrReplace(request.webhookSecret(), config.webhookSecret());
        if (!adminToken.isBlank() && (adminToken.length() < 8 || adminToken.length() > 4096)) {
            throw badRequest("Gitea 管理令牌长度必须为 8–4096 个字符");
        }
        if (!webhookSecret.isBlank() && (webhookSecret.length() < 32 || webhookSecret.length() > 4096)) {
            throw badRequest("Webhook 密钥长度必须为 32–4096 个字符");
        }

        String adminCiphertext = adminToken.isBlank() ? null : cipher.encrypt(adminToken);
        String webhookCiphertext = webhookSecret.isBlank() ? null : cipher.encrypt(webhookSecret);
        repository.save(apiUrl, publicUrl, adminCiphertext, webhookCiphertext, webhookUrl, user.id());
        return get(user);
    }

    private String preserveOrReplace(String submitted, String current) {
        String value = submitted == null ? "" : submitted.trim();
        return value.isBlank() ? current : value;
    }

    private String validateInternalApiUrl(String value) {
        URI uri = parse(value, "Gitea 内部 API 地址");
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (!(host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1") || host.equals("gitea"))) {
            throw badRequest("Gitea 内部 API 主机仅允许 localhost、127.0.0.1、::1 或 gitea");
        }
        if (!uri.getRawPath().isEmpty() && !uri.getRawPath().equals("/")) {
            throw badRequest("Gitea 内部 API 地址不能包含额外路径");
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) throw badRequest("Gitea 内部 API 仅支持 HTTP 或 HTTPS");
        return value.trim();
    }

    private String validatePublicHttpsUrl(String value, String label) {
        URI uri = parse(value, label);
        if (!"https".equalsIgnoreCase(uri.getScheme())) throw badRequest(label + "必须使用 HTTPS");
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (!isValidPublicHostname(host) || host.equals("gitea") || host.equals("localhost")
                || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")
                || host.endsWith(".intranet") || host.endsWith(".lan") || isNonPublicIpv4(host)
                || host.equals("::1") || host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80:")) {
            throw badRequest(label + "必须指向公网可访问的 HTTPS 主机");
        }
        return value.trim();
    }

    private boolean isValidPublicHostname(String host) {
        if (!HOSTNAME.matcher(host).matches() || !host.contains(".")) return false;
        String[] labels = host.split("\\.", -1);
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63 || !Character.isLetterOrDigit(label.charAt(0))
                    || !Character.isLetterOrDigit(label.charAt(label.length() - 1))) return false;
        }
        String tld = labels[labels.length - 1];
        return !tld.equals("test") && !tld.equals("example") && !tld.equals("invalid")
                && !tld.equals("localhost") && !tld.equals("local");
    }

    private URI parse(String value, String label) {
        if (value == null || value.isBlank() || value.length() > 1000) throw badRequest(label + "不能为空且不能超过 1000 个字符");
        try {
            URI uri = URI.create(value.trim());
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || uri.getPort() > 65535 || uri.getPort() == 0) throw badRequest(label + "格式无效，不能包含账号、密码、查询参数或片段");
            return uri;
        } catch (IllegalArgumentException exception) {
            throw badRequest(label + "格式无效");
        }
    }

    private boolean isNonPublicIpv4(String host) {
        // Reject non-canonical numeric forms such as 127.1 and 2130706433. Some HTTP
        // clients interpret those as loopback even though they are not dotted quads.
        if (!host.matches("[0-9.]+")) return false;
        String[] parts = host.split("\\.");
        if (parts.length != 4) return true;
        try {
            int a = Integer.parseInt(parts[0]), b = Integer.parseInt(parts[1]);
            for (String part : parts) if (part.length() > 1 && part.startsWith("0")) return true;
            for (String part : parts) if (Integer.parseInt(part) > 255) return true;
            int c = Integer.parseInt(parts[2]);
            return a == 0 || a == 10 || a == 127 || a >= 224 || (a == 169 && b == 254)
                    || (a == 172 && b >= 16 && b <= 31) || (a == 192 && (b == 168 || b == 0 && c == 0
                    || b == 0 && c == 2 || b == 88 && c == 99))
                    || (a == 198 && (b == 18 || b == 19 || b == 51 && c == 100))
                    || (a == 203 && b == 0 && c == 113)
                    || (a == 100 && b >= 64 && b <= 127);
        } catch (NumberFormatException exception) {
            return true;
        }
    }

    private String normalize(String value) {
        String normalized = value;
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    private void requireAdmin(AuthenticatedUser user) { AuthContext.requireRole(user, "admin"); }
    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "GIT_SETTINGS_INVALID", message);
    }
}
