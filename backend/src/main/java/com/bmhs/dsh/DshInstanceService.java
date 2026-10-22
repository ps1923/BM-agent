package com.bmhs.dsh;

import com.bmhs.auth.AuthContext;
import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class DshInstanceService {
    private static final Logger logger = LoggerFactory.getLogger(DshInstanceService.class);
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);
    private static final int CLEANUP_BATCH_SIZE = 50;

    private final DshInstanceRepository repository;
    private final DshTokenRepository tokenRepository;
    private final DshTokenService tokenService;
    private final DshContainerRuntime runtime;
    private final String dockerBinary;
    private final String image;
    private final String apiBaseUrl;
    private final String publicBaseUrl;
    private final boolean enabled;
    private final boolean insecureHttpEnabled;
    private final double cpuLimit;
    private final int memoryLimitMb;
    private final int pidsLimit;
    private final Duration startupTimeout;

    public DshInstanceService(DshInstanceRepository repository, DshTokenRepository tokenRepository,
                              DshTokenService tokenService, DshContainerRuntime runtime,
                              @Value("${bm-hs.dsh.docker-binary:docker}") String dockerBinary,
                              @Value("${bm-hs.dsh.image:}") String image,
                              @Value("${bm-hs.dsh.api-base-url:}") String apiBaseUrl,
                              @Value("${bm-hs.dsh.public-base-url:}") String publicBaseUrl,
                              @Value("${bm-hs.dsh.enabled:false}") boolean enabled,
                              @Value("${bm-hs.dsh.insecure-http-enabled:false}") boolean insecureHttpEnabled,
                              @Value("${bm-hs.dsh.cpu-limit:1.0}") double cpuLimit,
                              @Value("${bm-hs.dsh.memory-limit-mb:2048}") int memoryLimitMb,
                              @Value("${bm-hs.dsh.pids-limit:256}") int pidsLimit,
                              @Value("${bm-hs.dsh.startup-timeout:PT90S}") Duration startupTimeout) {
        this.repository = repository;
        this.tokenRepository = tokenRepository;
        this.tokenService = tokenService;
        this.runtime = runtime;
        this.dockerBinary = dockerBinary;
        this.image = image;
        this.apiBaseUrl = apiBaseUrl;
        this.publicBaseUrl = publicBaseUrl;
        this.enabled = enabled;
        this.insecureHttpEnabled = insecureHttpEnabled;
        this.cpuLimit = cpuLimit;
        this.memoryLimitMb = memoryLimitMb;
        this.pidsLimit = pidsLimit;
        this.startupTimeout = startupTimeout;
    }

    public DshInstanceModels.InstanceView status(AuthenticatedUser user) {
        requireStudent(user);
        if (!enabled) return new DshInstanceModels.InstanceView("stopped", null, null);
        return repository.findByStudent(user.id()).map(instance -> view(instance, true))
                .orElseGet(() -> new DshInstanceModels.InstanceView("stopped", null, null));
    }

    public DshInstanceModels.InstanceView launch(AuthenticatedUser user) {
        requireStudent(user);
        if (!enabled) unavailable("DSH 实例服务尚未启用");
        validateConfiguration();
        DshInstanceModels.Instance instance = repository.getOrCreate(user.id(), UUID.randomUUID().toString(),
                "bmhs-dsh-data-" + UUID.randomUUID().toString().replace("-", ""), cpuLimit, memoryLimitMb, pidsLimit);
        if ("running".equals(instance.status())) return view(instance, true);
        if (!repository.claimStart(instance.id())) {
            return repository.findByStudent(user.id()).map(current -> view(current, false))
                    .orElseThrow(() -> new IllegalStateException("DSH 实例状态读取失败"));
        }

        String networkName = networkName(instance.routeKey());
        DshInstanceModels.RuntimeHandle started = null;
        String failureStage = "container_start";
        try {
            String publicUrl = publicUrl(instance.routeKey());
            URI publicUri = URI.create(publicUrl);
            DshTokenService.CredentialPair credentials = tokenService.issuePair(user.id(), instance.id());
            DshContainerSpec spec = new DshContainerSpec(image, dockerBinary, apiBaseUrl, publicUrl,
                    publicUri.getRawAuthority(), containerName(instance.routeKey()), networkName,
                    instance.storageVolume(), instance.routeKey(), credentials.access().token(),
                    credentials.refreshCredential(), cpuLimit, memoryLimitMb, pidsLimit, startupTimeout,
                    insecureHttpEnabled);
            started = runtime.start(spec);
            failureStage = "persist_running";
            if (!repository.markRunning(instance.id(), started.containerId(), started.proxyPort())) {
                throw new IllegalStateException("DSH 启动状态无法提交");
            }
            failureStage = "load_running_state";
            DshInstanceModels.Instance running = repository.findByStudent(user.id()).orElseThrow();
            return new DshInstanceModels.InstanceView(running.status(), started.launchUrl(), running.lastActiveAt());
        } catch (RuntimeException exception) {
            logger.warn("DSH launch failed at {} ({})", failureStage, exception.getClass().getSimpleName());
            if (started != null) {
                try { runtime.stop(dockerBinary, started.containerId(), networkName, startupTimeout); }
                catch (RuntimeException ignored) { }
            }
            tokenRepository.revokeStudentTokens(user.id(), instance.id());
            tokenRepository.revokeStudentRefreshTokens(user.id(), instance.id());
            repository.markError(instance.id());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DSH_START_FAILED", "DSH 启动失败，请稍后重试");
        }
    }

    public void stop(AuthenticatedUser user) {
        requireStudent(user);
        if (!enabled) return;
        DshInstanceModels.Instance instance = repository.findByStudent(user.id()).orElse(null);
        if (instance == null || !repository.claimStopNow(instance.id(), user.id())) return;
        try {
            runtime.stop(dockerBinary, instance.containerId(), networkName(instance.routeKey()), startupTimeout);
            tokenRepository.revokeStudentTokens(user.id(), instance.id());
            tokenRepository.revokeStudentRefreshTokens(user.id(), instance.id());
            repository.markStopped(instance.id());
        } catch (RuntimeException exception) {
            repository.markError(instance.id());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DSH_STOP_FAILED", "DSH 停止失败，访问已被平台阻断");
        }
    }

    public int proxyPort(AuthenticatedUser user, String routeKey) {
        requireStudent(user);
        if (!enabled) throw new ApiException(HttpStatus.NOT_FOUND, "DSH_ROUTE_NOT_FOUND", "DSH 路由不存在");
        if (routeKey == null || !routeKey.matches("[0-9a-fA-F-]{36}")) {
            throw new ApiException(HttpStatus.NOT_FOUND, "DSH_ROUTE_NOT_FOUND", "DSH 路由不存在");
        }
        DshInstanceModels.Instance instance = repository.findByStudent(user.id())
                .filter(candidate -> candidate.routeKey().equalsIgnoreCase(routeKey))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DSH_ROUTE_NOT_FOUND", "DSH 路由不存在"));
        int port = repository.proxyPort(routeKey, user.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DSH_ROUTE_NOT_FOUND", "DSH 实例当前不可用"));
        repository.markActive(instance.id());
        return port;
    }

    @Scheduled(fixedDelayString = "${bm-hs.dsh.cleanup-interval:PT1M}")
    public void stopIdleInstances() {
        if (!enabled) return;
        Instant cutoff = Instant.now().minus(IDLE_TIMEOUT);
        for (DshInstanceModels.Instance instance : repository.findIdle(cutoff, CLEANUP_BATCH_SIZE)) {
            if (!repository.claimStop(instance.id(), cutoff)) continue;
            try {
                runtime.stop(dockerBinary, instance.containerId(), networkName(instance.routeKey()), startupTimeout);
                tokenRepository.revokeStudentTokens(instance.studentId(), instance.id());
                tokenRepository.revokeStudentRefreshTokens(instance.studentId(), instance.id());
                repository.markStopped(instance.id());
            } catch (RuntimeException exception) {
                repository.markError(instance.id());
            }
        }
    }

    private DshInstanceModels.InstanceView view(DshInstanceModels.Instance instance, boolean resolveLaunchUrl) {
        String launchUrl = null;
        if (resolveLaunchUrl && "running".equals(instance.status()) && instance.containerId() != null) {
            launchUrl = runtime.currentLaunchUrl(dockerBinary, publicUrl(instance.routeKey()), instance.containerId(), startupTimeout);
        }
        return new DshInstanceModels.InstanceView(instance.status(), launchUrl, instance.lastActiveAt());
    }

    private String publicUrl(String routeKey) {
        String base = publicBaseUrl.replaceAll("/+$", "");
        return base + "/" + routeKey + "/";
    }

    private void validateConfiguration() {
        if (image == null || !image.matches("[A-Za-z0-9._/:@-]{1,255}")) unavailable("DSH 镜像未配置");
        if (apiBaseUrl == null || !validOrigin(apiBaseUrl, insecureHttpEnabled)) unavailable("BM-HS API 地址未配置或不安全");
        if (publicBaseUrl == null || !validOrigin(publicBaseUrl, insecureHttpEnabled)) unavailable("DSH 安全访问地址未配置");
        if (!Double.isFinite(cpuLimit) || cpuLimit <= 0 || memoryLimitMb <= 0 || pidsLimit <= 0
                || startupTimeout == null || startupTimeout.isNegative() || startupTimeout.isZero()) {
            unavailable("DSH 资源或启动参数无效");
        }
    }

    private boolean validOrigin(String value, boolean allowInsecureHttp) {
        try {
            URI uri = URI.create(value);
            boolean secure = "https".equalsIgnoreCase(uri.getScheme());
            boolean loopbackHttp = "http".equalsIgnoreCase(uri.getScheme())
                    && (allowInsecureHttp || "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
            return (secure || loopbackHttp) && uri.getHost() != null && uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String networkName(String routeKey) { return "bmhs-dsh-net-" + routeKey.replace("-", ""); }
    private String containerName(String routeKey) { return "bmhs-dsh-" + routeKey.replace("-", ""); }

    private void requireStudent(AuthenticatedUser user) {
        AuthContext.requireRole(user, "student");
    }

    private void unavailable(String message) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DSH_NOT_CONFIGURED", message);
    }
}
