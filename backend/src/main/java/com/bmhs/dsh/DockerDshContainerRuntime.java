package com.bmhs.dsh;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DockerDshContainerRuntime implements DshContainerRuntime {
    private static final Logger logger = LoggerFactory.getLogger(DockerDshContainerRuntime.class);
    private static final int MAX_OUTPUT_BYTES = 32_768;
    private static final Pattern PUBLISHED_PORT = Pattern.compile("127\\.0\\.0\\.1:(\\d+)");
    @Override
    public DshInstanceModels.RuntimeHandle start(DshContainerSpec spec) {
        String containerId = null;
        String failureStage = "create_volume";
        try {
            createNamedResource(spec.dockerBinary(), "volume", spec.volumeName(), spec.startupTimeout());
            failureStage = "create_network";
            createNamedResource(spec.dockerBinary(), "network", spec.networkName(), spec.startupTimeout());
            failureStage = "create_container";
            containerId = createContainer(spec);
            failureStage = "inject_credentials";
            writeCredentials(spec, containerId);
            failureStage = "resolve_proxy_port";
            int port = resolvePublishedPort(spec.dockerBinary(), containerId, spec.startupTimeout());
            failureStage = "await_launch_url";
            String launchUrl = awaitLaunchUrl(spec, containerId);
            return new DshInstanceModels.RuntimeHandle(containerId, port, launchUrl);
        } catch (RuntimeException exception) {
            logger.warn("DSH container provisioning failed at {} ({})", failureStage, exception.getClass().getSimpleName());
            try {
                removeContainer(spec.dockerBinary(), containerId == null ? spec.containerName() : containerId,
                        spec.startupTimeout());
            } catch (RuntimeException ignored) { }
            try {
                removeNetwork(spec.dockerBinary(), spec.networkName(), spec.startupTimeout());
            } catch (RuntimeException ignored) { }
            throw exception;
        }
    }

    @Override
    public String currentLaunchUrl(String dockerBinary, String publicUrl, String containerId, Duration timeout) {
        CommandResult result = command(List.of(dockerBinary, "exec", containerId,
                "cat", "/run/bmhs-dsh/launch-url"), timeout);
        return result.exitCode() == 0 ? extractLaunchUrl(publicUrl, result.output()) : null;
    }

    @Override
    public void stop(String dockerBinary, String containerId, String networkName, Duration timeout) {
        removeContainer(dockerBinary, containerId, timeout);
        removeNetwork(dockerBinary, networkName, timeout);
    }

    private String createContainer(DshContainerSpec spec) {
        List<String> args = List.of(spec.dockerBinary(), "run", "--detach",
                "--name", spec.containerName(), "--network", spec.networkName(),
                "--label", "bm-hs.dsh.instance=" + spec.workspaceRef(),
                "--restart", "no", "--cpus", Double.toString(spec.cpuLimit()),
                "--memory", spec.memoryLimitMb() + "m", "--pids-limit", Integer.toString(spec.pidsLimit()),
                "--cap-drop", "ALL", "--security-opt", "no-new-privileges",
                "--env", "DSH_PUBLIC_URL=" + spec.publicUrl(),
                "--env", "DSH_TRUSTED_HOST=" + spec.trustedHost(),
                "--env", "BMHS_DSH_ALLOW_INSECURE_HTTP=" + spec.insecureHttpEnabled(),
                "--publish", "127.0.0.1::3080", "--volume", spec.volumeName() + ":/workspace",
                "--tmpfs", "/run/bmhs-dsh:rw,noexec,nosuid,nodev,size=64k,mode=0700,uid=10001,gid=10001",
                "--workdir", "/workspace",
                "--entrypoint", "/bin/sh", spec.image(), "-lc",
                "exec node /opt/bm-hs-dsh-plugin/container-entrypoint.mjs --profile web "
                        + "--host \"$(hostname -i | cut -d ' ' -f1)\" --port 3080 --no-open "
                        + "--public-url \"$DSH_PUBLIC_URL\" --trusted-host \"$DSH_TRUSTED_HOST\"");
        CommandResult result = command(args, spec.startupTimeout());
        if (result.exitCode() != 0 || result.truncated()) throw new IllegalStateException("DSH 容器创建失败");
        return DockerWorkspaceId.parse(result.output());
    }

    private void writeCredentials(DshContainerSpec spec, String containerId) {
        String contents = "BMHS_API_BASE_URL=" + envValue("BMHS_API_BASE_URL", spec.apiBaseUrl()) + "\n"
                + "BMHS_DSH_CREDENTIAL=" + envValue("BMHS_DSH_CREDENTIAL", spec.accessCredential()) + "\n"
                + "BMHS_DSH_REFRESH_CREDENTIAL=" + envValue("BMHS_DSH_REFRESH_CREDENTIAL", spec.refreshCredential()) + "\n"
                + "BMHS_DSH_WORKSPACE_ID=" + envValue("BMHS_DSH_WORKSPACE_ID", spec.workspaceRef()) + "\n";
        CommandResult result = command(List.of(spec.dockerBinary(), "exec", "--interactive", containerId,
                "sh", "-lc", "umask 077 && cat > /run/bmhs-dsh/credentials"), spec.startupTimeout(),
                contents.getBytes(StandardCharsets.UTF_8));
        if (result.exitCode() != 0 || result.truncated()) {
            logger.warn("DSH credential injection returned exitCode={} outputBytes={} truncated={}",
                    result.exitCode(), result.output().length(), result.truncated());
            throw new IllegalStateException("DSH 凭据安全注入失败");
        }
    }

    private String envValue(String name, String value) {
        if (value == null || value.isBlank() || value.contains("\n") || value.contains("\r")) {
            logger.warn("DSH credential input is invalid for {}", name);
            throw new IllegalArgumentException("DSH 容器环境配置无效");
        }
        return value;
    }

    private void createNamedResource(String docker, String type, String name, Duration timeout) {
        CommandResult inspect = command(List.of(docker, type, "inspect", name), timeout);
        if (inspect.exitCode() == 0) return;
        CommandResult create = command(List.of(docker, type, "create", name), timeout);
        if (create.exitCode() != 0 && !create.output().toLowerCase(java.util.Locale.ROOT).contains("already exists")) {
            throw new IllegalStateException("DSH 持久资源创建失败");
        }
    }

    private int resolvePublishedPort(String docker, String containerId, Duration timeout) {
        CommandResult result = command(List.of(docker, "port", containerId, "3080/tcp"), timeout);
        Matcher matcher = PUBLISHED_PORT.matcher(result.output());
        if (result.exitCode() != 0 || !matcher.find()) throw new IllegalStateException("DSH 反向代理端口绑定失败");
        int port = Integer.parseInt(matcher.group(1));
        if (port < 1 || port > 65535) throw new IllegalStateException("DSH 反向代理端口无效");
        return port;
    }

    private String awaitLaunchUrl(DshContainerSpec spec, String containerId) {
        long deadline = System.nanoTime() + spec.startupTimeout().toNanos();
        while (System.nanoTime() < deadline) {
            CommandResult state = command(List.of(spec.dockerBinary(), "inspect", "--format", "{{.State.Running}}", containerId), Duration.ofSeconds(3));
            if (state.exitCode() != 0 || !state.output().trim().equals("true")) throw new IllegalStateException("DSH 服务未能保持运行");
            String url = currentLaunchUrl(spec.dockerBinary(), spec.publicUrl(), containerId, Duration.ofSeconds(3));
            if (url != null) return url;
            try { Thread.sleep(500); } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待 DSH 服务启动时被中断", exception);
            }
        }
        throw new IllegalStateException("等待 DSH Web 服务启动超时");
    }

    private String extractLaunchUrl(String publicUrl, String logs) {
        Matcher matcher = Pattern.compile("https?://[^\\s\\\"'<>]+", Pattern.CASE_INSENSITIVE).matcher(logs);
        while (matcher.find()) {
            String candidate = matcher.group().replaceAll("[),.;]+$", "");
            try {
                java.net.URI candidateUri = java.net.URI.create(candidate);
                java.net.URI publicUri = java.net.URI.create(publicUrl);
                if (candidateUri.getScheme().equals(publicUri.getScheme())
                        && candidateUri.getAuthority().equalsIgnoreCase(publicUri.getAuthority())
                        && candidateUri.getPath().equals(publicUri.getPath())
                        && (candidateUri.getRawQuery() != null || candidateUri.getRawFragment() != null)) return candidate;
            } catch (RuntimeException ignored) { }
        }
        return null;
    }

    private CommandResult command(List<String> args, Duration timeout, byte[] stdin) {
        Process process;
        try {
            process = new ProcessBuilder(args).redirectErrorStream(true).start();
        } catch (IOException exception) {
            throw new IllegalStateException("Docker 运行时不可用", exception);
        }
        AtomicReference<CommandResult> captured = new AtomicReference<>();
        Thread reader = new Thread(() -> {
            try { captured.set(readBounded(process.getInputStream(), MAX_OUTPUT_BYTES)); }
            catch (IOException ignored) { }
        }, "bmhs-dsh-docker-output");
        reader.setDaemon(true);
        reader.start();
        try {
            if (stdin != null) {
                try (OutputStream output = process.getOutputStream()) {
                    output.write(stdin);
                }
            } else {
                process.getOutputStream().close();
            }
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Docker 操作超时");
            }
            reader.join(1500);
            CommandResult result = captured.get();
            if (result == null) throw new IllegalStateException("Docker 命令输出读取失败");
            return new CommandResult(process.exitValue(), result.output(), result.truncated());
        } catch (IOException exception) {
            process.destroyForcibly();
            throw new IllegalStateException("DSH 凭据安全注入失败", exception);
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Docker 操作被中断", exception);
        }
    }

    private void removeContainer(String docker, String containerId, Duration timeout) {
        if (containerId == null) return;
        CommandResult result = command(List.of(docker, "rm", "--force", containerId), timeout);
        if (result.exitCode() != 0 && !result.output().toLowerCase(java.util.Locale.ROOT).contains("no such container")) {
            throw new IllegalStateException("DSH 容器停止失败");
        }
    }

    private void removeNetwork(String docker, String networkName, Duration timeout) {
        CommandResult result = command(List.of(docker, "network", "rm", networkName), timeout);
        if (result.exitCode() != 0 && !result.output().toLowerCase(java.util.Locale.ROOT).contains("no such network")) {
            throw new IllegalStateException("DSH 网络清理失败");
        }
    }

    private CommandResult command(List<String> args, Duration timeout) {
        return command(args, timeout, null);
    }

    private CommandResult readBounded(InputStream input, int maxBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, 4096));
        byte[] buffer = new byte[4096];
        boolean truncated = false;
        int count;
        while ((count = input.read(buffer)) >= 0) {
            int available = maxBytes - output.size();
            if (available > 0) output.write(buffer, 0, Math.min(available, count));
            if (count > available) truncated = true;
        }
        return new CommandResult(-1, output.toString(StandardCharsets.UTF_8), truncated);
    }

    private record CommandResult(int exitCode, String output, boolean truncated) {}

    private static final class DockerWorkspaceId {
        private static String parse(String output) {
            String id = output == null ? "" : output.strip();
            if (!id.matches("[0-9a-f]{12,64}")) throw new IllegalStateException("Docker 未返回有效的 DSH 容器标识");
            return id;
        }
    }
}
