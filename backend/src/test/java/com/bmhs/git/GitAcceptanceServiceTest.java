package com.bmhs.git;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.rag.BugService;
import com.bmhs.rag.RagModels;
import com.bmhs.rag.RagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class GitAcceptanceServiceTest {
    private final GitSubmissionRepository repository = mock(GitSubmissionRepository.class);
    private final GiteaGateway gitea = mock(GiteaGateway.class);
    private final BugService bugService = mock(BugService.class);
    private final RagService rag = mock(RagService.class);
    private final GitCodeAcceptanceHarness acceptanceHarness = mock(GitCodeAcceptanceHarness.class);
    private final GitConfig config = new GitConfig("http://127.0.0.1:3000", "https://git.example.test",
            "admin-secret", "webhook-secret-that-is-long-enough", "https://api.example.test/api/integrations/gitea/webhook",
            Base64.getEncoder().encodeToString(new byte[32]));
    private final GitCredentialCipher cipher = new GitCredentialCipher(config);
    private final GitAcceptanceService service = new GitAcceptanceService(repository, gitea, config, cipher,
            rag, acceptanceHarness, bugService, 1, 2);

    @AfterEach
    void closeWorker() { service.shutdown(); }

    @Test
    void gitRunCreationDoesNotProvisionOrTouchWorkspace() {
        AuthenticatedUser student = student();
        when(repository.createOrFindRun(25L, student.id()))
                .thenReturn(new GitModels.RegisteredRun(88L, 25L, student.id(), "not_started"));

        GitModels.RunView result = service.createRun(student, 25L);

        assertEquals(new GitModels.RunView(88L, 25L, "git", "not_started"), result);
        verify(gitea, never()).ensureRepository(anyLong(), anyLong(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void rejectsNonStudentBeforeRunLookup() {
        AuthenticatedUser teacher = new AuthenticatedUser(3L, "t@example.test", "Teacher", "teacher");

        ApiException exception = assertThrows(ApiException.class, () -> service.getRun(teacher, 44L));

        assertEquals("ROLE_FORBIDDEN", exception.code());
        verify(repository, never()).requireStudentRun(anyLong(), anyLong());
    }

    @Test
    void studentHistoryIsScopedToAuthenticatedStudentAndGitRun() {
        AuthenticatedUser student = student();
        List<GitModels.StudentSubmissionSummary> history = List.of(new GitModels.StudentSubmissionSummary(
                17L, "a".repeat(40), "completed", "已修复问题", List.of(4L), List.of(), Instant.now()));
        when(repository.studentSubmissions(88L, student.id())).thenReturn(history);

        assertEquals(history, service.studentSubmissions(student, 88L));

        verify(repository).studentSubmissions(88L, student.id());
    }

    @Test
    void refusesCredentialsWhenGiteaIsNotConfiguredAndNeverInventsCloneUrl() {
        GitConfig disabled = new GitConfig("", "", "", "", "", "");
        GitAcceptanceService disabledService = new GitAcceptanceService(repository, gitea, disabled,
                new GitCredentialCipher(disabled), mock(RagService.class), mock(GitCodeAcceptanceHarness.class), bugService, 1, 1);
        AuthenticatedUser student = student();
        when(repository.requireStudentRun(88L, student.id()))
                .thenReturn(new GitModels.RegisteredRun(88L, 25L, student.id(), "not_started"));

        ApiException exception = assertThrows(ApiException.class, () -> disabledService.repository(student, 88L));

        assertEquals("GIT_REPOSITORY_NOT_CONFIGURED", exception.code());
        verify(gitea, never()).ensureRepository(anyLong(), anyLong(), any(), any(), any(), any(), anyBoolean());
        disabledService.shutdown();
    }

    @Test
    void webhookRequiresHmacAndCannotRegisterSubmissionsFromPayload() {
        byte[] payload = "{\"repository\":{\"id\":42},\"commits\":[{\"id\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}]}"
                .getBytes(StandardCharsets.UTF_8);

        ApiException exception = assertThrows(ApiException.class, () -> service.webhook("push", "0".repeat(64), payload));

        assertEquals("GITEA_WEBHOOK_INVALID", exception.code());
        verify(repository, never()).findByRepoIdAndSha(anyLong(), any());
        verify(repository, never()).registerSubmission(any(), any(), any(), any(), any());
    }

    @Test
    void validWebhookOnlyMatchesPreviouslyRegisteredRepositorySha() throws Exception {
        byte[] payload = "{\"repository\":{\"id\":42},\"commits\":[{\"id\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}]}"
                .getBytes(StandardCharsets.UTF_8);
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(config.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = java.util.HexFormat.of().formatHex(mac.doFinal(payload));

        GitModels.WebhookAck result = service.webhook("push", signature, payload);

        assertEquals(new GitModels.WebhookAck(true, false), result);
        verify(repository).findByRepoIdAndSha(42L, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @Test
    void submissionRegistersOnlyStudentSelectedNodesAgainstFixedPushedSha() {
        AuthenticatedUser student = student();
        String sha = "b".repeat(40);
        GitModels.RegisteredRun run = new GitModels.RegisteredRun(88L, 25L, student.id(), "not_started");
        String encryptedToken = cipher.encrypt("git-push-token");
        GitModels.RepositoryRecord repo = new GitModels.RepositoryRecord(88L, 25L, student.id(), 42L,
                "bmhs-random-owner", "experiment-25-run-88", "https://git.example.test/bmhs-random-owner/experiment-25-run-88",
                "https://git.example.test/bmhs-random-owner/experiment-25-run-88.git", "main", "bmhs-random-owner",
                encryptedToken, "ready");
        when(repository.requireStudentRun(88L, student.id())).thenReturn(run);
        when(repository.createRepositoryRecord(eq(run), any(), any(), any(), any())).thenReturn(repo);
        when(gitea.ensureRepository(eq(88L), eq(student.id()), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new GitModels.GiteaRepository(42L, repo.owner(), repo.name()));
        when(repository.repositoryForStudent(88L, student.id())).thenReturn(repo);
        GitModels.CommitInfo commit = new GitModels.CommitInfo(sha, "tree", "Add feature", "Student", Instant.now(), List.of());
        when(gitea.commit(repo.owner(), repo.name(), sha, "git-push-token")).thenReturn(commit);
        GitModels.SubmissionRecord submission = new GitModels.SubmissionRecord(101L, 88L, 25L, student.id(),
                sha, null, "Add feature", "Student", commit.committedAt(), "check selected nodes", "queued", Instant.now());
        when(repository.registerSubmission(eq(run), eq(sha), eq(commit), eq(List.of(4L, 9L)), eq("check selected nodes")))
                .thenReturn(submission);
        when(repository.claim(101L)).thenReturn(false);

        GitModels.SubmitResponse response = service.submit(student, 88L,
                new GitModels.SubmitRequest(sha, List.of(9L, 4L), "check selected nodes"));

        assertEquals(new GitModels.SubmitResponse(101L, "queued"), response);
        verify(gitea).commit(repo.owner(), repo.name(), sha, "git-push-token");
        verify(repository).registerSubmission(eq(run), eq(sha), eq(commit), eq(List.of(4L, 9L)), eq("check selected nodes"));
    }

    @Test
    void repositoryResponseUsesHttpsCloneUrlWithoutEmbeddedCredentials() {
        AuthenticatedUser student = student();
        GitModels.RegisteredRun run = new GitModels.RegisteredRun(88L, 25L, student.id(), "not_started");
        GitModels.RepositoryRecord repo = new GitModels.RepositoryRecord(88L, 25L, student.id(), 42L,
                "bmhs-random-owner", "experiment-25-run-88", "https://git.example.test/bmhs-random-owner/experiment-25-run-88",
                "https://git.example.test/bmhs-random-owner/experiment-25-run-88.git", "main", "bmhs-random-owner",
                cipher.encrypt("secret-push-token"), "ready");
        when(repository.requireStudentRun(88L, student.id())).thenReturn(run);
        when(repository.createRepositoryRecord(eq(run), any(), any(), any(), any())).thenReturn(repo);
        when(gitea.ensureRepository(eq(88L), eq(student.id()), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new GitModels.GiteaRepository(42L, repo.owner(), repo.name()));
        when(repository.repositoryForStudent(88L, student.id())).thenReturn(repo);

        GitModels.RepositoryView view = service.repository(student, 88L);

        assertEquals("https://git.example.test/bmhs-random-owner/experiment-25-run-88.git", view.cloneUrl());
        org.junit.jupiter.api.Assertions.assertFalse(view.cloneUrl().contains("secret-push-token"));
        org.junit.jupiter.api.Assertions.assertTrue(view.setupInstructions().contains("Git Credential Manager"));
    }

    @Test
    void bugDraftRequiresStudentOwnedSubmissionAndExistingDraftBeforeCallingBugService() {
        AuthenticatedUser student = student();
        when(repository.findStudentSubmission(9L, student.id())).thenReturn(new GitModels.SubmissionRecord(
                9L, 88L, 25L, student.id(), "a".repeat(40), null, "commit", "student", Instant.now(), null, "completed", Instant.now()));
        when(repository.results(9L)).thenReturn(List.of(new GitModels.NodeResult(5L, "partially_meets", "发现错误",
                List.of(), List.of("修复"), new GitModels.BugDraft("标题", "问题", "修复", "Python"))));
        Instant createdAt = Instant.now();
        RagModels.BugCaseView created = new RagModels.BugCaseView(51L, 25L, 88L, 5L, null, student.id(),
                "学生确认的标题", "学生确认的问题", "学生确认的修复", "Python", "pending", "private", "not_indexed",
                null, createdAt, createdAt);
        when(bugService.create(eq(student), eq(88L), any(RagModels.BugRequest.class))).thenReturn(created);
        GitModels.BugDraftConfirmRequest request = new GitModels.BugDraftConfirmRequest("学生确认的标题",
                "学生确认的问题", "学生确认的修复", "Python");

        assertEquals(created, service.confirmBugDraft(student, 9L, 5L, request));
        verify(bugService).create(eq(student), eq(88L), any(RagModels.BugRequest.class));
    }

    @Test
    void bugConfirmationCannotCreateCaseWithoutModelDraft() {
        AuthenticatedUser student = student();
        when(repository.findStudentSubmission(9L, student.id())).thenReturn(new GitModels.SubmissionRecord(
                9L, 88L, 25L, student.id(), "a".repeat(40), null, "commit", "student", Instant.now(), null, "completed", Instant.now()));
        when(repository.results(9L)).thenReturn(List.of(new GitModels.NodeResult(5L, "meets", "通过",
                List.of(), List.of(), null)));

        ApiException exception = assertThrows(ApiException.class, () -> service.confirmBugDraft(student, 9L, 5L,
                new GitModels.BugDraftConfirmRequest("标题", "问题", "解决", "Python")));

        assertEquals("BUG_DRAFT_NOT_FOUND", exception.code());
        verifyNoInteractions(bugService);
    }

    @Test
    void acceptanceRetrievesBugMemoriesSeparatelyForEachSelectedNode() {
        RagModels.RagReference firstMemory = new RagModels.RagReference(51L, "节点一问题", "现象一", "修复一", 0.2);
        RagModels.RagReference secondMemory = new RagModels.RagReference(52L, "节点二问题", "现象二", "修复二", 0.3);
        when(rag.searchForExperiment(eq(7L), eq(25L), eq(4L), eq((String) null), anyString(), eq(3)))
                .thenReturn(new RagModels.RagSearchResult(true, List.of(firstMemory)));
        when(rag.searchForExperiment(eq(7L), eq(25L), eq(9L), eq((String) null), anyString(), eq(3)))
                .thenReturn(new RagModels.RagSearchResult(true, List.of(secondMemory)));
        when(acceptanceHarness.evaluate(anyString(), anyList(), anyList(), anyMap())).thenReturn(List.of());
        GitModels.SubmissionRecord submission = new GitModels.SubmissionRecord(101L, 88L, 25L, 7L,
                "a".repeat(40), null, "提交", "学生", Instant.now(), null, "queued", Instant.now());
        List<GitModels.NodeSpec> nodes = List.of(
                new GitModels.NodeSpec(4L, "1", "节点一", "搭建页面", List.of("写页面")),
                new GitModels.NodeSpec(9L, "2", "节点二", "接入接口", List.of("调用 API")));

        service.analyzeNodes(submission, nodes, List.of());

        verify(rag).searchForExperiment(eq(7L), eq(25L), eq(4L), eq((String) null), anyString(), eq(3));
        verify(rag).searchForExperiment(eq(7L), eq(25L), eq(9L), eq((String) null), anyString(), eq(3));
        verify(acceptanceHarness).evaluate(eq(submission.commitSha()), eq(nodes), eq(List.of()), org.mockito.ArgumentMatchers.argThat(map ->
                map.get(4L).equals(List.of(firstMemory)) && map.get(9L).equals(List.of(secondMemory))));
    }

    @Test
    void lineDiffCountsSeparatedEditsWithoutCountingUnchangedMiddleLines() {
        GitAcceptanceService.LineDiff diff = service.lineDiff(
                List.of("before", "unchanged one", "old", "unchanged two", "after"),
                List.of("before", "unchanged one", "new", "unchanged two", "after"));

        assertEquals(1, diff.additions());
        assertEquals(1, diff.deletions());
        org.junit.jupiter.api.Assertions.assertTrue(diff.patch().contains(" unchanged two\n"));
        org.junit.jupiter.api.Assertions.assertTrue(diff.patch().contains("-old\n+new\n"));
    }

    private AuthenticatedUser student() {
        return new AuthenticatedUser(7L, "student@example.test", "Student", "student");
    }
}
