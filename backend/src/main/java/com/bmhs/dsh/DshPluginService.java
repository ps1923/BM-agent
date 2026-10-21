package com.bmhs.dsh;

import com.bmhs.auth.AuthContext;
import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.rag.BugService;
import com.bmhs.rag.RagModels;
import com.bmhs.rag.RagService;
import com.bmhs.teacher.TeacherModels;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class DshPluginService {
    private static final int MAX_FILES = 50;
    private static final long MAX_FILE_BYTES = 2_000_000L;
    private static final long MAX_TOTAL_BYTES = 10_000_000L;
    private static final Pattern PRIVATE_KEY = Pattern.compile("-----BEGIN [A-Z0-9 ]*PRIVATE KEY-----", Pattern.CASE_INSENSITIVE);
    private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/-]{20,}={0,2}");
    private static final Pattern COMMON_SECRET = Pattern.compile("(?i)\\b(?:gh[pousr]_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,}|glpat-[A-Za-z0-9_-]{20,}|sk-[A-Za-z0-9_-]{20,})\\b");
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile("(?i)(?:api[_-]?key|password|secret|access[_-]?token)\\s*[:=]\\s*['\\\"]?[^'\\\"\\s]{12,}");

    private final DshPluginRepository repository;
    private final BugService bugService;
    private final RagService ragService;

    public DshPluginService(DshPluginRepository repository, BugService bugService, RagService ragService) {
        this.repository = repository;
        this.bugService = bugService;
        this.ragService = ragService;
    }

    public List<DshPluginModels.ExperimentView> experiments(DshPrincipal principal) {
        return repository.experiments(principal.studentId());
    }

    public DshPluginModels.RunView bindRun(DshPrincipal principal, DshPluginModels.RunBindRequest request) {
        return repository.bindRun(principal, request.experimentId(), request.workspaceRef(), request.sessionRef());
    }

    public DshPluginModels.LearningPath learningPath(DshPrincipal principal, long runId) {
        return repository.learningPath(principal, runId);
    }

    public void updateNode(DshPrincipal principal, long runId, long nodeId,
                           DshPluginModels.ProgressUpdateRequest request) {
        requireStudentConfirmation(request.studentConfirmed());
        repository.updateNode(principal, runId, nodeId, request.status(), true);
    }

    public void updateTask(DshPrincipal principal, long runId, long taskId,
                           DshPluginModels.ProgressUpdateRequest request) {
        requireStudentConfirmation(request.studentConfirmed());
        repository.updateTask(principal, runId, taskId, request.status(), true);
    }

    public RagModels.RagSearchResult searchMemory(DshPrincipal principal, long experimentId,
                                                   MemorySearchRequest request) {
        long boundExperimentId = repository.assertBoundRun(principal, request.runId());
        if (boundExperimentId != experimentId) throw new ApiException(HttpStatus.FORBIDDEN, "EXPERIMENT_FORBIDDEN", "当前运行未绑定该实验");
        return ragService.searchForExperiment(principal.studentId(), experimentId, request.nodeId(),
                request.technologyStack(), request.query(), request.limit() == null ? 3 : request.limit());
    }

    public RagModels.BugCaseView submitBug(DshPrincipal principal, long runId, RagModels.BugRequest request) {
        repository.assertBoundRun(principal, runId);
        return bugService.createFromDsh(principal, runId, request);
    }

    @Transactional
    public DshPluginModels.CodeSubmissionView submitCode(DshPrincipal principal, long runId,
                                                          DshPluginModels.CodeSubmissionRequest request) {
        repository.assertBoundRun(principal, runId);
        if (request.files().isEmpty() || request.files().size() > MAX_FILES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "SUBMISSION_FILE_LIMIT", "请选择 1 到 50 个文件");
        }
        Set<String> seenPaths = new HashSet<>();
        List<DshPluginRepository.CleanFile> files = request.files().stream().map(this::cleanFile).toList();
        long total = files.stream().mapToLong(DshPluginRepository.CleanFile::sizeBytes).sum();
        if (total > MAX_TOTAL_BYTES) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "SUBMISSION_TOO_LARGE", "代码提交总大小超过 10 MB");
        for (DshPluginRepository.CleanFile file : files) {
            if (!seenPaths.add(file.path())) throw new ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_SUBMISSION_PATH", "提交文件路径重复");
        }
        return repository.submitCode(principal, runId, request.nodeId(), request.description(), files);
    }

    public List<DshPluginModels.ExperimentView> teacherExperiments(AuthenticatedUser user) {
        AuthContext.requireRole(user, "teacher");
        return repository.teacherExperiments(user.id());
    }

    public List<DshPluginModels.TeacherRunView> teacherRuns(AuthenticatedUser user, long experimentId) {
        AuthContext.requireRole(user, "teacher");
        return repository.runsForTeacherExperiment(user.id(), experimentId);
    }

    public List<DshPluginModels.CodeSubmissionView> teacherSubmissions(AuthenticatedUser user, long runId) {
        AuthContext.requireRole(user, "teacher");
        return repository.submissionsForTeacher(user.id(), runId);
    }

    public DshPluginModels.SubmissionDetail teacherSubmission(AuthenticatedUser user, long runId, long submissionId) {
        AuthContext.requireRole(user, "teacher");
        return repository.submissionForTeacher(user.id(), runId, submissionId);
    }

    public TeacherModels.ReviewView teacherDshReview(AuthenticatedUser user, long runId) {
        AuthContext.requireRole(user, "teacher");
        return repository.findDshReview(user.id(), runId);
    }

    public TeacherModels.ReviewView saveTeacherDshReview(AuthenticatedUser user, long runId,
                                                           TeacherModels.ReviewRequest request) {
        AuthContext.requireRole(user, "teacher");
        return repository.reviewDshRun(user.id(), runId, request);
    }

    private DshPluginRepository.CleanFile cleanFile(DshPluginModels.SubmittedFile input) {
        String path = normalizePath(input.path());
        byte[] contentBytes = input.content().getBytes(StandardCharsets.UTF_8);
        if (contentBytes.length == 0 || contentBytes.length > MAX_FILE_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "SUBMISSION_FILE_TOO_LARGE", "代码文件必须为非空文本且不超过 2 MB");
        }
        String lowerPath = path.toLowerCase(java.util.Locale.ROOT);
        if (path.split("/").length > 20 || path.split("/").length == 0
                || java.util.Arrays.stream(path.split("/")).anyMatch(part -> part.equalsIgnoreCase(".git"))) {
            throw invalidPath();
        }
        String name = path.substring(path.lastIndexOf('/') + 1).toLowerCase(java.util.Locale.ROOT);
        if (name.startsWith(".env") || name.contains("credential") || name.contains("secret")
                || name.equals("id_rsa") || name.equals("id_ed25519")
                || lowerPath.endsWith(".pem") || lowerPath.endsWith(".key")
                || lowerPath.endsWith(".p12") || lowerPath.endsWith(".pfx")) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "SENSITIVE_FILE_REJECTED", "密钥和凭据文件不能提交给教师查看");
        }
        String content = input.content();
        if (PRIVATE_KEY.matcher(content).find() || BEARER.matcher(content).find()
                || COMMON_SECRET.matcher(content).find() || SECRET_ASSIGNMENT.matcher(content).find()) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "SENSITIVE_CONTENT_REJECTED", "检测到疑似密钥或凭据，请移除后再提交");
        }
        String actualHash = sha256(contentBytes);
        if (!actualHash.equalsIgnoreCase(input.sha256())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SUBMISSION_HASH_MISMATCH", "文件内容哈希校验失败");
        }
        return new DshPluginRepository.CleanFile(path, content, actualHash, contentBytes.length);
    }

    private String normalizePath(String raw) {
        if (raw == null || raw.isBlank()) throw invalidPath();
        String path = raw.replace('\\', '/');
        if (path.startsWith("/") || path.matches("^[A-Za-z]:.*") || path.indexOf('\0') >= 0
                || path.contains("://") || path.length() > 700) throw invalidPath();
        String[] parts = path.split("/", -1);
        for (String part : parts) if (part.isBlank() || part.equals(".") || part.equals("..")) throw invalidPath();
        return String.join("/", parts);
    }

    private String sha256(byte[] content) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private ApiException invalidPath() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SUBMISSION_PATH", "代码提交仅接受工作区内的相对文件路径");
    }

    private void requireStudentConfirmation(Boolean confirmed) {
        if (!Boolean.TRUE.equals(confirmed)) {
            throw new ApiException(HttpStatus.CONFLICT, "STUDENT_CONFIRMATION_REQUIRED", "需要学生明确确认后才能更新学习进度");
        }
    }

    public record MemorySearchRequest(@jakarta.validation.constraints.Positive long runId, Long nodeId,
                                      @jakarta.validation.constraints.Size(max = 100) String technologyStack,
                                      @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(10) Integer limit,
                                      @org.springframework.lang.NonNull @jakarta.validation.constraints.NotBlank
                                      @jakarta.validation.constraints.Size(max = 20000) String query) {}
}
