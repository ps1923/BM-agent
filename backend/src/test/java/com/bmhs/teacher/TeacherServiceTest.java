package com.bmhs.teacher;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.guidance.GuidanceModelClient;
import com.bmhs.guidance.GuidanceModels;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeacherServiceTest {
    private final AuthenticatedUser teacher = new AuthenticatedUser(3L, "teacher@example.com", "教师", "teacher");

    @Test
    void generatesSummaryFromStatisticsAndEventMetadata() {
        TeacherRepository repository = mock(TeacherRepository.class);
        GuidanceModelClient model = mock(GuidanceModelClient.class);
        TeacherModels.RunSummary base = summary();
        when(repository.summary(3L, 8L)).thenReturn(base);
        when(repository.timeline(3L, 8L)).thenReturn(List.of(
                new TeacherModels.TimelineEvent("node", "接口实现", "completed", Instant.now())));
        when(model.complete(any(), contains("接口实现")))
                .thenReturn(new GuidanceModels.ModelReply("已完成接口节点，建议继续运行测试。", "deepseek-chat"));

        TeacherModels.RunSummary result = new TeacherService(repository, model).summary(teacher, 8L);

        assertEquals(true, result.aiSummaryAvailable());
        assertEquals("已完成接口节点，建议继续运行测试。", result.aiSummary());
        assertEquals(4, result.completedTasks());
        assertEquals(9, result.totalTasks());
        verify(repository).timeline(3L, 8L);
    }

    @Test
    void modelUnavailableKeepsRealStatisticsWithoutFakeSummary() {
        TeacherRepository repository = mock(TeacherRepository.class);
        GuidanceModelClient model = mock(GuidanceModelClient.class);
        TeacherModels.RunSummary base = summary();
        when(repository.summary(3L, 8L)).thenReturn(base);
        when(repository.timeline(3L, 8L)).thenReturn(List.of());
        when(model.complete(any(), any())).thenThrow(new ApiException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "MODEL_NOT_CONFIGURED", "DeepSeek 尚未配置"));

        TeacherModels.RunSummary result = new TeacherService(repository, model).summary(teacher, 8L);

        assertEquals(false, result.aiSummaryAvailable());
        assertEquals(null, result.aiSummary());
        assertEquals(2, result.completedNodes());
        assertEquals(4, result.completedTasks());
        assertEquals(9, result.totalTasks());
    }

    private TeacherModels.RunSummary summary() {
        return new TeacherModels.RunSummary(8L, 5L, "Java 实验", "in_progress", 2, 4,
                4, 9, 3, 5, 1, false, null, null);
    }
}
