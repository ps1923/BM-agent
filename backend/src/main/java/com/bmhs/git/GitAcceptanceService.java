package com.bmhs.git;

import com.bmhs.auth.AuthContext;
import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.rag.BugService;
import com.bmhs.rag.RagModels;
import com.bmhs.rag.RagService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class GitAcceptanceService {
    private static final Logger log = LoggerFactory.getLogger(GitAcceptanceService.class);
    private static final int MAX_CONTEXT_CHARS = 120_000;
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)([\\w.-]*(?:password|passwd|secret|token|api[-_]?key|access[-_]?key|private[-_]?key|credential)[\\w.-]*\\s*[:=]\\s*)([^\\r\\n]+)");
    private static final Pattern COMMON_TOKEN = Pattern.compile(
            "(?i)\\b(?:gh[pousr]_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,}|glpat-[A-Za-z0-9_-]{20,}|sk-[A-Za-z0-9_-]{20,})\\b");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("java", "py", "js", "jsx", "ts", "tsx", "html", "css",
            "json", "xml", "yml", "yaml", "properties", "sql", "md", "txt", "toml", "gradle", "kts", "sh", "vue", "svelte");
    private static final Set<String> EXCLUDED_PARTS = Set.of(".git", "node_modules", "target", "build", "dist", "out",
            ".venv", "venv", "__pycache__", ".idea", ".vscode", "coverage", "vendor", ".gradle");
    private static final Set<String> SENSITIVE_NAMES = Set.of(".env", ".env.local", ".npmrc", ".pypirc", "id_rsa",
            "id_ed25519", "credentials", "secrets.yml", "secrets.yaml", "service-account.json");

    private final GitSubmissionRepository repository;
    private final GiteaGateway gitea;
    private final GitConfig config;
    private final GitCredentialCipher cipher;
    private final GitCodeAcceptanceHarness acceptanceHarness;
    private final RagService rag;
    private final BugService bugService;
    private final Gson gson = new Gson();
    private final ThreadPoolExecutor executor;
    private final Object[] repositoryProvisionLocks = java.util.stream.IntStream.range(0, 64)
            .mapToObj(ignored -> new Object()).toArray();

    public GitAcceptanceService(GitSubmissionRepository repository, GiteaGateway gitea, GitConfig config,
                                GitCredentialCipher cipher, RagService rag,
                                GitCodeAcceptanceHarness acceptanceHarness, BugService bugService,
                                @Value("${bm-hs.git.max-workers:2}") int maxWorkers,
                                @Value("${bm-hs.git.max-queue:16}") int maxQueue) {
        if (maxWorkers < 1 || maxWorkers > 8 || maxQueue < 1 || maxQueue > 100) {
            throw new IllegalArgumentException("Git 验收线程与队列配置超出允许范围");
        }
        this.repository = repository;
        this.gitea = gitea;
        this.config = config;
        this.cipher = cipher;
        this.acceptanceHarness = acceptanceHarness;
        this.rag = rag;
        this.bugService = bugService;
        this.executor = new ThreadPoolExecutor(maxWorkers, maxWorkers, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxQueue), runnable -> {
                    Thread thread = new Thread(runnable, "git-acceptance-worker");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    public GitModels.RunView createRun(AuthenticatedUser user, long experimentId) {
        requireStudent(user);
        GitModels.RegisteredRun run = repository.createOrFindRun(experimentId, user.id());
        return new GitModels.RunView(run.runId(), run.experimentId(), "git", run.status());
    }

    public GitModels.RunView getRun(AuthenticatedUser user, long runId) {
        requireStudent(user);
        GitModels.RegisteredRun run = repository.requireStudentRun(runId, user.id());
        return new GitModels.RunView(run.runId(), run.experimentId(), "git", run.status());
    }

    public GitModels.RepositoryView repository(AuthenticatedUser user, long runId) {
        requireStudent(user);
        GitModels.RegisteredRun run = repository.requireStudentRun(runId, user.id());
        GitModels.RepositoryRecord repo = ensureRepository(run);
        if (!"ready".equals(repo.status()) || repo.giteaRepoId() == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GIT_REPOSITORY_NOT_READY", "Git 仓库尚未就绪，请稍后重试");
        }
        return new GitModels.RepositoryView(repo.repoUrl(), repo.cloneUrl(), repo.branch(),
                "在本地终端执行 git clone <cloneUrl>；首次 push 时按提示输入仓库用户名和 Git 凭据。"
                        .replace("<cloneUrl>", repo.cloneUrl())
                        + " Windows 可使用 Git Credential Manager 保存凭据。不要把凭据拼入 URL、命令或日志。");
    }

    public GitModels.CredentialsView credentials(AuthenticatedUser user, long runId) {
        requireStudent(user);
        GitModels.RegisteredRun run = repository.requireStudentRun(runId, user.id());
        GitModels.RepositoryRecord repo = ensureRepository(run);
        if (!"ready".equals(repo.status()) || repo.credentialCiphertext() == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GIT_REPOSITORY_NOT_READY", "Git 凭据暂不可用");
        }
        return new GitModels.CredentialsView(repo.credentialUsername(), cipher.decrypt(repo.credentialCiphertext()), "https-basic-token");
    }

    public GitModels.SubmitResponse submit(AuthenticatedUser user, long runId, GitModels.SubmitRequest request) {
        requireStudent(user);
        GitModels.RegisteredRun run = repository.requireStudentRun(runId, user.id());
        GitModels.RepositoryRecord repo = ensureRepository(run);
        String sha = request.commitSha().toLowerCase(Locale.ROOT);
        if (request.nodeIds().stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(request.nodeIds()).size() != request.nodeIds().size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_GIT_NODE_SELECTION", "节点选择无效或存在重复");
        }
        List<Long> nodeIds = request.nodeIds().stream().sorted().toList();
        String token = cipher.decrypt(repo.credentialCiphertext());
        GitModels.CommitInfo commit = gitea.commit(repo.owner(), repo.name(), sha, token);
        if (!sha.equalsIgnoreCase(commit.sha())) throw new ApiException(HttpStatus.BAD_GATEWAY, "GITEA_COMMIT_MISMATCH", "仓库返回的 commit 与请求不一致");
        GitModels.SubmissionRecord submission = repository.registerSubmission(run, sha, commit, nodeIds, request.description());
        if ("failed".equals(submission.status()) && repository.retryFailedSubmission(submission.id())) {
            enqueue(submission.id());
            return new GitModels.SubmitResponse(submission.id(), "queued");
        }
        enqueue(submission.id());
        return new GitModels.SubmitResponse(submission.id(), submission.status());
    }

    public GitModels.StudentSubmissionView studentSubmission(AuthenticatedUser user, long submissionId) {
        requireStudent(user);
        return repository.studentSubmission(submissionId, user.id());
    }

    public List<GitModels.StudentSubmissionSummary> studentSubmissions(AuthenticatedUser user, long runId) {
        requireStudent(user);
        return repository.studentSubmissions(runId, user.id());
    }

    public RagModels.BugCaseView confirmBugDraft(AuthenticatedUser user, long submissionId, long nodeId,
                                                 GitModels.BugDraftConfirmRequest request) {
        requireStudent(user);
        GitModels.SubmissionRecord submission = repository.findStudentSubmission(submissionId, user.id());
        GitModels.NodeResult result = repository.results(submissionId).stream()
                .filter(item -> item.nodeId() == nodeId).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BUG_DRAFT_NOT_FOUND", "该节点没有可确认的 Bug 草案"));
        if (result.bugDraft() == null) throw new ApiException(HttpStatus.CONFLICT, "BUG_DRAFT_NOT_FOUND", "该节点没有可确认的 Bug 草案");
        return bugService.create(user, submission.runId(), new RagModels.BugRequest(request.title(), request.problem(),
                request.solution(), request.technologyStack(), nodeId));
    }

    public GitModels.WebhookAck webhook(String event, String signature, byte[] payload) {
        if (!"push".equalsIgnoreCase(event) || payload == null || payload.length > 2_000_000 || !validSignature(signature, payload)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "GITEA_WEBHOOK_INVALID", "Gitea webhook 签名或事件无效");
        }
        try {
            JsonObject root = gson.fromJson(new String(payload, StandardCharsets.UTF_8), JsonObject.class);
            JsonObject repo = root == null ? null : root.getAsJsonObject("repository");
            if (repo == null || !repo.has("id") || !root.has("commits")) return new GitModels.WebhookAck(true, false);
            long repoId = repo.get("id").getAsLong();
            var commits = root.getAsJsonArray("commits");
            boolean matched = false;
            for (var entry : commits) {
                if (!entry.isJsonObject() || !entry.getAsJsonObject().has("id")) continue;
                String sha = entry.getAsJsonObject().get("id").getAsString();
                if (!sha.matches("(?i)[0-9a-f]{40,64}")) continue;
                GitModels.SubmissionRecord registered = repository.findByRepoIdAndSha(repoId, sha.toLowerCase(Locale.ROOT));
                if (registered != null && repository.markWebhookSeen(repoId, sha.toLowerCase(Locale.ROOT))) {
                    matched = true;
                    if ("queued".equals(registered.status())) enqueue(registered.id());
                }
            }
            return new GitModels.WebhookAck(true, matched);
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GITEA_WEBHOOK_INVALID", "Gitea webhook 内容无效");
        }
    }

    public List<GitModels.TeacherSubmissionSummary> teacherSubmissions(AuthenticatedUser user, long runId) {
        AuthContext.requireRole(user, "teacher");
        return repository.teacherSubmissions(user.id(), runId);
    }

    public GitModels.TeacherSubmissionDetail teacherSubmission(AuthenticatedUser user, long submissionId) {
        AuthContext.requireRole(user, "teacher");
        GitModels.SubmissionRecord submission = repository.teacherSubmission(user.id(), submissionId);
        GitModels.RepositoryRecord repo = repository.repositoryForRun(submission.runId());
        if (!"ready".equals(repo.status()) || repo.credentialCiphertext() == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GIT_REPOSITORY_NOT_READY", "提交仓库当前不可读取");
        }
        String token = cipher.decrypt(repo.credentialCiphertext());
        GitModels.CommitInfo current = gitea.commit(repo.owner(), repo.name(), submission.commitSha(), token);
        List<GitModels.CommitFile> newFiles = safeFiles(gitea.filesAtCommit(repo.owner(), repo.name(), current, token));
        Map<String, GitModels.CommitFile> before = new HashMap<>();
        if (submission.parentSha() != null) {
            GitModels.CommitInfo parent = gitea.commit(repo.owner(), repo.name(), submission.parentSha(), token);
            safeFiles(gitea.filesAtCommit(repo.owner(), repo.name(), parent, token)).forEach(file -> before.put(file.path(), file));
        }
        List<GitModels.DiffFile> diff = buildDiff(before, newFiles);
        GitModels.CommitMetadata metadata = new GitModels.CommitMetadata(submission.commitSha(), current.message(),
                current.author(), current.committedAt(), current.parentShas());
        return new GitModels.TeacherSubmissionDetail(submission.id(), submission.runId(), submission.experimentId(),
                submission.studentId(), submission.commitSha(), submission.status(), submission.description(),
                metadata, diff, repository.results(submission.id()), submission.createdAt());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumeQueuedJobs() {
        repository.recoverStaleRunningSubmissions();
        repository.queuedSubmissionIds().forEach(this::enqueue);
    }

    @PreDestroy
    public void shutdown() { executor.shutdownNow(); }

    private GitModels.RepositoryRecord ensureRepository(GitModels.RegisteredRun run) {
        Object lock = repositoryProvisionLocks[Math.floorMod(Long.hashCode(run.runId()), repositoryProvisionLocks.length)];
        synchronized (lock) {
            return provisionRepository(run);
        }
    }

    private GitModels.RepositoryRecord provisionRepository(GitModels.RegisteredRun run) {
        requireGitConfig();
        cipher.encrypt("credential-encryption-preflight");
        String owner = "bmhs-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String name = "experiment-" + run.experimentId() + "-run-" + run.runId();
        String base = config.publicBaseUrl().replaceAll("/+$", "");
        String repoUrl = base + "/" + owner + "/" + name;
        String cloneUrl = repoUrl + ".git";
        GitModels.RepositoryRecord stored = repository.createRepositoryRecord(run, owner, name, repoUrl, cloneUrl);
        try {
            boolean prepareCredential = stored.credentialCiphertext() == null;
            GitModels.GiteaRepository remote = gitea.ensureRepository(run.runId(), run.studentId(), stored.owner(), stored.name(),
                    config.webhookTargetUrl(), config.webhookSecret(), prepareCredential);
            if (prepareCredential) {
                String rawToken = gitea.createGitToken(stored.credentialUsername(), run.runId(), remote.bootstrapPassword());
                repository.saveCredential(run.runId(), cipher.encrypt(rawToken));
            }
            repository.markRepositoryReady(run.runId(), remote.id());
        } catch (ApiException exception) {
            repository.markRepositoryError(run.runId(), exception.code());
            throw exception;
        } catch (RuntimeException exception) {
            repository.markRepositoryError(run.runId(), "GITEA_REQUEST_FAILED");
            throw new ApiException(HttpStatus.BAD_GATEWAY, "GITEA_REQUEST_FAILED", "代码仓库初始化失败，请稍后重试");
        }
        return repository.repositoryForStudent(run.runId(), run.studentId());
    }

    private void enqueue(long submissionId) {
        try { executor.execute(() -> analyze(submissionId)); }
        catch (java.util.concurrent.RejectedExecutionException exception) { repository.fail(submissionId, "GIT_ACCEPTANCE_QUEUE_FULL"); }
    }

    private void analyze(long submissionId) {
        if (!repository.claim(submissionId)) return;
        try {
            GitModels.SubmissionRecord submission = repository.findSubmission(submissionId);
            GitModels.RepositoryRecord repo = repository.repositoryForRun(submission.runId());
            String token = cipher.decrypt(repo.credentialCiphertext());
            GitModels.CommitInfo commit = gitea.commit(repo.owner(), repo.name(), submission.commitSha(), token);
            List<GitModels.CommitFile> files = safeFiles(gitea.filesAtCommit(repo.owner(), repo.name(), commit, token));
            List<GitModels.NodeSpec> nodes = repository.nodeSpecs(submissionId);
            if (nodes.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "GIT_NODES_REQUIRED", "提交未关联可验收节点");
            List<GitModels.RawResult> results = analyzeNodes(submission, nodes, files);
            repository.saveResults(submissionId, submission.experimentId(), nodes, results);
        } catch (RuntimeException exception) {
            String code = exception instanceof ApiException api ? api.code() : "GIT_ACCEPTANCE_FAILED";
            repository.fail(submissionId, code);
            log.warn("Git commit acceptance failed (submissionId={}, code={})", submissionId, code);
        }
    }

    List<GitModels.RawResult> analyzeNodes(GitModels.SubmissionRecord submission,
                                            List<GitModels.NodeSpec> nodes,
                                            List<GitModels.CommitFile> files) {
        Map<Long, List<RagModels.RagReference>> memoriesByNode = new HashMap<>();
        for (GitModels.NodeSpec node : nodes) {
            String query = node.name() + "\n" + node.description() + "\n"
                    + String.join("\n", node.tasks() == null ? List.of() : node.tasks()) + "\n"
                    + (node.acceptanceCriteria() == null ? "" : node.acceptanceCriteria().stream()
                    .map(criterion -> criterion.criterion() + " " + criterion.expectedEvidence())
                    .reduce("", (left, right) -> left + "\n" + right));
            RagModels.RagSearchResult memories = rag.searchForExperiment(submission.studentId(),
                    submission.experimentId(), node.nodeId(), null, query, 3);
            memoriesByNode.put(node.nodeId(), memories.available() ? memories.references() : List.of());
        }
        return acceptanceHarness.evaluate(submission.commitSha(), nodes, files, memoriesByNode);
    }

    private List<GitModels.CommitFile> safeFiles(List<GitModels.CommitFile> input) {
        List<GitModels.CommitFile> result = new ArrayList<>();
        int budget = MAX_CONTEXT_CHARS;
        for (GitModels.CommitFile file : input) {
            String path = normalizeRepoPath(file.path());
            if (path == null || excludedPath(path) || !indexable(path)) continue;
            String content = redact(file.content());
            if (content.isBlank()) continue;
            if (content.length() > budget) {
                throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "GIT_ACCEPTANCE_CONTEXT_TOO_LARGE",
                        "可分析源码超过验收上下文上限；系统未截断后继续判断");
            }
            result.add(new GitModels.CommitFile(path, file.blobSha(), content));
            budget -= content.length();
            if (budget <= 0) break;
        }
        return List.copyOf(result);
    }

    private String normalizeRepoPath(String path) {
        if (path == null || path.isBlank() || path.startsWith("/") || path.indexOf('\\') >= 0 || path.contains("\0")) return null;
        for (String part : path.split("/")) if (part.equals("..") || part.equals(".")) return null;
        return path;
    }
    private boolean excludedPath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        String name = lower.substring(lower.lastIndexOf('/') + 1);
        if (SENSITIVE_NAMES.contains(name) || name.startsWith(".env.")) return true;
        for (String part : lower.split("/")) if (EXCLUDED_PARTS.contains(part)) return true;
        return false;
    }
    private boolean indexable(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        String name = lower.substring(lower.lastIndexOf('/') + 1);
        if (Set.of("pom.xml", "package.json", "requirements.txt", "pyproject.toml", "makefile", "dockerfile").contains(name)) return true;
        int dot = name.lastIndexOf('.');
        return dot > 0 && ALLOWED_EXTENSIONS.contains(name.substring(dot + 1));
    }
    private String redact(String value) {
        String withoutKeys = value.replaceAll("-----BEGIN [A-Z0-9 ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z0-9 ]*PRIVATE KEY-----", "[REDACTED_PRIVATE_KEY]");
        return COMMON_TOKEN.matcher(SECRET_ASSIGNMENT.matcher(withoutKeys).replaceAll("$1[REDACTED]"))
                .replaceAll("[REDACTED_TOKEN]");
    }

    private List<GitModels.DiffFile> buildDiff(Map<String, GitModels.CommitFile> before, List<GitModels.CommitFile> after) {
        Map<String, GitModels.CommitFile> now = new HashMap<>();
        after.forEach(file -> now.put(file.path(), file));
        Set<String> paths = new HashSet<>(before.keySet()); paths.addAll(now.keySet());
        List<String> sorted = paths.stream().sorted().limit(200).toList();
        List<GitModels.DiffFile> diffs = new ArrayList<>();
        for (String path : sorted) {
            String oldText = before.containsKey(path) ? before.get(path).content() : "";
            String newText = now.containsKey(path) ? now.get(path).content() : "";
            if (oldText.equals(newText)) continue;
            String status = !before.containsKey(path) ? "added" : !now.containsKey(path) ? "deleted" : "modified";
            List<String> oldLines = List.of(oldText.split("\\R", -1));
            List<String> newLines = List.of(newText.split("\\R", -1));
            String header = "--- a/" + path + "\n+++ b/" + path + "\n";
            long cells = (long) (oldLines.size() + 1) * (newLines.size() + 1);
            String patch;
            int additions;
            int deletions;
            if (cells <= 250_000) {
                LineDiff lineDiff = lineDiff(oldLines, newLines);
                patch = header + lineDiff.patch();
                additions = lineDiff.additions();
                deletions = lineDiff.deletions();
            } else {
                patch = header + "差异体量较大，以下为旧/新版本完整文本快照；本文件未计算逐行变更统计。\n"
                        + "--- 旧版本\n" + oldLines.stream().map(line -> "-" + line).collect(java.util.stream.Collectors.joining("\n"))
                        + "\n--- 新版本\n" + newLines.stream().map(line -> "+" + line).collect(java.util.stream.Collectors.joining("\n"));
                additions = -1;
                deletions = -1;
            }
            if (patch.length() > 20_000) patch = patch.substring(0, 20_000) + "\n[显示内容已截断；文件完整版本仍保存在仓库中]";
            diffs.add(new GitModels.DiffFile(path, status, additions, deletions, patch));
        }
        if (paths.size() > sorted.size()) {
            diffs.add(new GitModels.DiffFile("其余 " + (paths.size() - sorted.size()) + " 个文件", "truncated", -1, -1,
                    "变更文件超过教师端展示上限；本提交差异未完整展示。请通过已授权的 Gitea 仓库查看其余文件。"));
        }
        return List.copyOf(diffs);
    }
    LineDiff lineDiff(List<String> oldLines, List<String> newLines) {
        int[][] lcs = new int[oldLines.size() + 1][newLines.size() + 1];
        for (int i = oldLines.size() - 1; i >= 0; i--) {
            for (int j = newLines.size() - 1; j >= 0; j--) {
                lcs[i][j] = oldLines.get(i).equals(newLines.get(j))
                        ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        StringBuilder patch = new StringBuilder();
        int additions = 0;
        int deletions = 0;
        int i = 0;
        int j = 0;
        while (i < oldLines.size() || j < newLines.size()) {
            if (i < oldLines.size() && j < newLines.size() && oldLines.get(i).equals(newLines.get(j))) {
                patch.append(' ').append(oldLines.get(i++)).append('\n');
                j++;
            } else if (i < oldLines.size() && (j == newLines.size() || lcs[i + 1][j] >= lcs[i][j + 1])) {
                patch.append('-').append(oldLines.get(i++)).append('\n');
                deletions++;
            } else {
                patch.append('+').append(newLines.get(j++)).append('\n');
                additions++;
            }
        }
        return new LineDiff(additions, deletions, patch.toString());
    }
    record LineDiff(int additions, int deletions, String patch) {}

    private boolean validSignature(String signature, byte[] payload) {
        if (signature == null || signature.length() != 64 || config.webhookSecret().isBlank()) return false;
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(config.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = java.util.HexFormat.of().parseHex(signature);
            return MessageDigest.isEqual(mac.doFinal(payload), expected);
        } catch (Exception exception) { return false; }
    }

    private void requireGitConfig() {
        if (!config.configured()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GIT_REPOSITORY_NOT_CONFIGURED",
                "Gitea 尚未安全配置；未生成或返回仓库地址");
    }
    private void requireStudent(AuthenticatedUser user) { AuthContext.requireRole(user, "student"); }
}
