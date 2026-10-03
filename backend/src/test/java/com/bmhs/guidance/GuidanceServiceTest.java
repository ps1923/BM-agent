package com.bmhs.guidance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.WorkspaceModels;
import com.bmhs.workspace.WorkspaceRepository;
import com.bmhs.workspace.WorkspaceService;
import com.bmhs.rag.BugRepository;
import com.bmhs.rag.RagModels;
import com.bmhs.rag.RagService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuidanceServiceTest {
    private final AuthenticatedUser student = new AuthenticatedUser(7L, "student@example.com", "学生", "student");

    @Test
    void sendsOnlyOwnedRunContextToModelAndPersistsConversation() {
        GuidanceRepository repository = mock(GuidanceRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        WorkspaceService workspaceService = mock(WorkspaceService.class);
        GuidanceModelClient model = mock(GuidanceModelClient.class);
        BugRepository bugRepository = mock(BugRepository.class);
        RagService ragService = mock(RagService.class);
        WorkspaceModels.WorkspaceRecord workspace = new WorkspaceModels.WorkspaceRecord(
                9L, 3L, 2L, 7L, "C:/workspace/run-3", "test-image", "container",
                "running", BigDecimal.ONE, 512);
        WorkspaceModels.RunRecord run = new WorkspaceModels.RunRecord(3L, 2L, 7L,
                "in_progress", 21L, workspace);
        WorkspaceModels.WorkspaceFile file = new WorkspaceModels.WorkspaceFile(9L, "src/Main.java",
                "Main.java", "java", 20L, "hash", true, Instant.now());
        when(workspaceRepository.findRun(3L, 7L)).thenReturn(run);
        when(repository.findContext(3L, 7L)).thenReturn(new GuidanceModels.ExperimentContext(
                2L, 3L, "Java 实验", "完成接口", "说明", 21L, "实现接口", "编写代码"));
        when(repository.recentMessages(3L, 2L, 12)).thenReturn(List.of(
                new GuidanceModels.ChatMessage("user", "上次的问题")));
        when(bugRepository.findCourseIdForRun(3L, 7L)).thenReturn(15L);
        when(ragService.searchForStudent(7L, 15L, 2L, 21L, "Java/Spring Boot", "学生问题"))
                .thenReturn(new RagModels.RagSearchResult(true, List.of(new RagModels.RagReference(
                        12L, "依赖问题", "依赖冲突", "重新安装依赖", 0.1))));
        when(workspaceService.listFiles(student, 9L)).thenReturn(List.of(file));
        when(workspaceService.readFile(student, 9L, "src/Main.java"))
                .thenReturn(new WorkspaceModels.WorkspaceFileContent(file, "class Main {}"));
        when(model.complete(any(), any())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(1, String.class);
            JsonObject json = JsonParser.parseString(prompt).getAsJsonObject();
            assertEquals("学生问题", json.get("studentQuestion").getAsString());
            assertEquals("class Main {}", json.getAsJsonArray("projectFiles").get(0)
                    .getAsJsonObject().get("content").getAsString());
            assertTrue(invocation.getArgument(0, String.class).contains("不可信数据"));
            return new GuidanceModels.ModelReply("先运行测试。", "deepseek-chat");
        });
        GuidanceService service = new GuidanceService(repository, workspaceRepository, workspaceService, model,
                bugRepository, ragService, 120000);

        GuidanceModels.GuidanceResponse response = service.guide(student, 3L,
                new GuidanceModels.GuidanceRequest("学生问题"));

        assertEquals("先运行测试。", response.content());
        assertEquals(true, response.ragAvailable());
        assertEquals(List.of(12L), response.ragReferences().stream()
                .map(RagModels.RagReference::bugCaseId).toList());
        assertEquals(List.of("src/Main.java"), response.referencedFiles());
        verify(repository).appendMessage(3L, 2L, 21L, "user", "normal", "学生问题");
        verify(repository).appendMessage(3L, 2L, 21L, "assistant", "normal", "先运行测试。");
    }

    @Test
    void keepsSerializedPromptWithinTotalCharacterBudgetForLargeMixedContext() {
        GuidanceRepository repository = mock(GuidanceRepository.class);
        WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
        WorkspaceService workspaceService = mock(WorkspaceService.class);
        GuidanceModelClient model = mock(GuidanceModelClient.class);
        BugRepository bugRepository = mock(BugRepository.class);
        RagService ragService = mock(RagService.class);
        WorkspaceModels.WorkspaceRecord workspace = new WorkspaceModels.WorkspaceRecord(
                9L, 3L, 2L, 7L, "C:/workspace/run-3", "test-image", "container", "running", BigDecimal.ONE, 512);
        WorkspaceModels.RunRecord run = new WorkspaceModels.RunRecord(3L, 2L, 7L, "in_progress", 21L, workspace);
        WorkspaceModels.WorkspaceFile file = new WorkspaceModels.WorkspaceFile(9L, "src/Main.java",
                "Main.java", "java", 20000L, "hash", true, Instant.now());
        GuidanceModels.ExperimentContext context = new GuidanceModels.ExperimentContext(
                2L, 3L, "实验".repeat(8000), "目标".repeat(8000), "说明".repeat(8000),
                21L, "节点".repeat(8000), "任务".repeat(8000));
        List<GuidanceModels.ChatMessage> history = new ArrayList<>();
        for (int i = 0; i < 12; i++) history.add(new GuidanceModels.ChatMessage("user", "旧对话".repeat(3000)));
        List<RagModels.RagReference> references = List.of(
                new RagModels.RagReference(1L, "标题".repeat(200), "问题".repeat(3000), "方案".repeat(3000), 0.1),
                new RagModels.RagReference(2L, "标题".repeat(200), "问题".repeat(3000), "方案".repeat(3000), 0.2),
                new RagModels.RagReference(3L, "标题".repeat(200), "问题".repeat(3000), "方案".repeat(3000), 0.3));
        when(workspaceRepository.findRun(3L, 7L)).thenReturn(run);
        when(repository.findContext(3L, 7L)).thenReturn(context);
        when(repository.recentMessages(3L, 2L, 12)).thenReturn(history);
        when(bugRepository.findCourseIdForRun(3L, 7L)).thenReturn(15L);
        when(ragService.searchForStudent(eq(7L), eq(15L), eq(2L), eq(21L), eq("Java/Spring Boot"), any()))
                .thenReturn(new RagModels.RagSearchResult(true, references));
        when(workspaceService.listFiles(student, 9L)).thenReturn(List.of(file));
        when(workspaceService.readFile(student, 9L, "src/Main.java"))
                .thenReturn(new WorkspaceModels.WorkspaceFileContent(file, "源码".repeat(10000)));
        AtomicReference<String> system = new AtomicReference<>();
        AtomicReference<String> prompt = new AtomicReference<>();
        when(model.complete(any(), any())).thenAnswer(invocation -> {
            system.set(invocation.getArgument(0, String.class));
            prompt.set(invocation.getArgument(1, String.class));
            return new GuidanceModels.ModelReply("先运行测试。", "deepseek-chat");
        });
        GuidanceService service = new GuidanceService(repository, workspaceRepository, workspaceService, model,
                bugRepository, ragService, 10000);

        service.guide(student, 3L, new GuidanceModels.GuidanceRequest("问题".repeat(3000)));

        assertTrue(system.get().length() + prompt.get().length() <= 10000);
        JsonObject json = JsonParser.parseString(prompt.get()).getAsJsonObject();
        assertEquals(6000, json.get("studentQuestion").getAsString().length());
    }

    @Test
    void teacherCannotUseStudentGuidanceEndpoint() {
        GuidanceService service = new GuidanceService(mock(GuidanceRepository.class),
                mock(WorkspaceRepository.class), mock(WorkspaceService.class), mock(GuidanceModelClient.class),
                mock(BugRepository.class), mock(RagService.class), 120000);
        AuthenticatedUser teacher = new AuthenticatedUser(3L, "teacher@example.com", "教师", "teacher");

        ApiException exception = assertThrows(ApiException.class,
                () -> service.guide(teacher, 3L, new GuidanceModels.GuidanceRequest("问题")));

        assertEquals("ROLE_FORBIDDEN", exception.code());
    }
}
