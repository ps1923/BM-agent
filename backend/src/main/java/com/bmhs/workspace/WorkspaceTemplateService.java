package com.bmhs.workspace;

import com.bmhs.workspace.WorkspaceModels.WorkspaceRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Component
public class WorkspaceTemplateService {
    private final WorkspaceRepository repository;

    public WorkspaceTemplateService(WorkspaceRepository repository) {
        this.repository = repository;
    }

    public void materializeIfEmpty(WorkspaceRecord workspace, String experimentName, int maxFileBytes) {
        List<WorkspaceModels.WorkspaceFile> indexedFiles = repository.listFiles(workspace.id());
        if (indexedFiles != null && !indexedFiles.isEmpty()) return;
        String template = templateFor(experimentName);
        for (TemplateFile file : files(template)) {
            materialize(workspace, file, maxFileBytes);
        }
    }

    private void materialize(WorkspaceRecord workspace, TemplateFile file, int maxFileBytes) {
        Path root = Path.of(workspace.storagePath()).toAbsolutePath().normalize();
        Path target = root.resolve(file.path()).normalize();
        if (!target.startsWith(root) || file.path().isBlank()) {
            throw new IllegalStateException("工作区模板路径无效");
        }
        try {
            byte[] content = readResource(file.resource());
            if (content.length > maxFileBytes) throw new IllegalStateException("工作区模板文件超过大小限制");
            Files.createDirectories(target.getParent());
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(target)) {
                throw new IllegalStateException("工作区模板目标不允许为符号链接");
            }
            Files.write(target, content);
            repository.saveFile(workspace.id(), file.path(), displayName(file.path()), language(file.path()),
                    content.length, sha256(content), file.entry(), file.order());
        } catch (IOException exception) {
            throw new IllegalStateException("工作区模板初始化失败", exception);
        }
    }

    private byte[] readResource(String resource) throws IOException {
        ClassPathResource source = new ClassPathResource(resource);
        try (InputStream input = source.getInputStream()) {
            return input.readAllBytes();
        }
    }

    private List<TemplateFile> files(String template) {
        return switch (template) {
            case "spring-boot" -> List.of(
                    new TemplateFile("src/main/java/com/example/demo/DemoApplication.java",
                            "workspace-templates/spring-boot/src/main/java/com/example/demo/DemoApplication.java", true, 0),
                    new TemplateFile("pom.xml", "workspace-templates/spring-boot/pom.xml", false, 1),
                    new TemplateFile("README.md", "workspace-templates/spring-boot/README.md", false, 2));
            case "python" -> List.of(
                    new TemplateFile("main.py", "workspace-templates/python/main.py", true, 0),
                    new TemplateFile("README.md", "workspace-templates/python/README.md", false, 1));
            default -> List.of(new TemplateFile("README.md", "workspace-templates/generic/README.md", true, 0));
        };
    }

    private String templateFor(String experimentName) {
        String value = experimentName == null ? "" : experimentName.toLowerCase(Locale.ROOT);
        if (value.contains("python")) return "python";
        if (value.contains("spring") || value.contains("java")) return "spring-boot";
        return "generic";
    }

    private String displayName(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private String language(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? null : path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private record TemplateFile(String path, String resource, boolean entry, int order) {}
}
