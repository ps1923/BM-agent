package com.bmhs.experimentcreation;

import com.bmhs.experimentcreation.CreationModels.ConfirmedIntent;
import com.bmhs.experimentcreation.CreationModels.Difficulty;
import com.bmhs.experimentcreation.CreationModels.ExperimentTree;
import com.bmhs.experimentcreation.CreationModels.PlanResponse;
import com.bmhs.experimentcreation.CreationModels.PositionedNode;
import com.bmhs.experimentcreation.CreationModels.PlanTask;
import com.bmhs.experimentcreation.CreationModels.StudentIntentEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExperimentCreationServiceTest {
    private CreationRepository repository;
    private HarnessGateway harness;
    private ExperimentTreeValidator validator;
    private TreeLayoutService layout;
    private CreationTransactions transactions;
    private ExperimentCreationService service;

    @BeforeEach
    void setUp() {
        repository = mock(CreationRepository.class);
        harness = mock(HarnessGateway.class);
        validator = mock(ExperimentTreeValidator.class);
        layout = mock(TreeLayoutService.class);
        transactions = mock(CreationTransactions.class);
        service = new ExperimentCreationService(repository, harness, validator, layout, transactions);
    }

    @Test
    void plannerReceivesOnlyTheConfirmedIntentContract() {
        ConfirmedIntent confirmed = new ConfirmedIntent(
                "0.1", "a".repeat(64), "2026-09-11T00:00:00Z",
                Map.of("raw_request", "原始需求", "observable_outcome", "可验收成果"));
        var session = new CreationRepository.SessionRecord(
                "session-1", 7L, "configured", null, confirmed, 2,
                confirmed.intentHash(), 180, Difficulty.NORMAL, null, null, null);
        ExperimentTree tree = new ExperimentTree("0.1", "实验", List.of("完成成果"), List.of());
        List<PositionedNode> positioned = List.of();
        PlanResponse stored = new PlanResponse("plan-1", 1, confirmed.intentHash(), "实验", List.of("完成成果"), positioned);

        when(transactions.snapshot("session-1", 7L)).thenReturn(session);
        when(harness.generateTree(confirmed, 180, Difficulty.NORMAL)).thenReturn(tree);
        when(layout.layout(tree)).thenReturn(positioned);
        when(transactions.savePlan(eq(session), eq("实验"), eq(List.of("完成成果")), eq(positioned)))
                .thenReturn(stored);

        service.generatePlan("session-1", 7L);

        verify(harness).generateTree(confirmed, 180, Difficulty.NORMAL);
        verify(harness, never()).clarify(any(), any(), anyBoolean());
    }

    @Test
    void requirementHarnessCannotConfirmWithoutTheConfirmationEndpoint() {
        var session = new CreationRepository.SessionRecord(
                "session-2", 7L, "direction_discussion", null, null, 0,
                null, null, null, null, null, null);
        StudentIntentEnvelope roleViolation = new StudentIntentEnvelope(
                "0.1", "confirmed", "已确认", Map.of("raw_request", "目标"),
                Map.of(), List.of(), List.of(), null);

        when(transactions.snapshot("session-2", 7L)).thenReturn(session);
        when(harness.clarify("目标", null, false)).thenReturn(roleViolation);

        assertThrows(ApiException.class, () -> service.sendDirectionMessage("session-2", 7L, "目标"));
        verify(transactions, never()).saveDirectionTurn(any(), any(), any());
    }

    @Test
    void materializationFailureNeverMarksTheDraftAccepted() {
        ConfirmedIntent confirmed = new ConfirmedIntent(
                "0.1", "b".repeat(64), "2026-09-11T00:00:00Z",
                Map.of("raw_request", "目标", "observable_outcome", "成果"));
        PlanTask task = new PlanTask("创建结构", "建立结构", "manual", Map.of());
        PositionedNode node = new PositionedNode("root", null, 0, "阶段一", "根节点", "节点说明",
                30, List.of(task, task, task), java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO);
        PlanResponse plan = new PlanResponse("plan-1", 1, confirmed.intentHash(), "实验",
                List.of("实验目的"), List.of(node));
        var session = new CreationRepository.SessionRecord(
                "session-3", 7L, "plan_ready", null, confirmed, 1, confirmed.intentHash(),
                180, Difficulty.NORMAL, null, null, plan);

        when(repository.lock("session-3", 7L)).thenReturn(session);
        when(repository.insertExperiment(anyLong(), any(), any(), any(), any(), anyInt(), any()))
                .thenReturn(10L);
        when(repository.insertStage(10L, "阶段一", 0)).thenReturn(20L);
        when(repository.insertNode(eq(10L), eq(20L), eq(null), eq(node), eq(0))).thenReturn(30L);
        doThrow(new RuntimeException("simulated write failure"))
                .when(repository).insertTask(eq(10L), eq(30L), any(), eq(0));

        assertThrows(RuntimeException.class,
                () -> service.materialize("session-3", 7L, "stable-key"));
        verify(repository, never()).finishMaterialization(any(), any(), any(), anyLong());
    }

    @Test
    void confirmationSealsTheVisibleIntentWithoutCallingHarnessAgain() {
        Map<String, Object> visibleIntent = Map.of(
                "raw_request", "创建学习画布", "observable_outcome", "可拖动并查看节点详情");
        StudentIntentEnvelope ready = new StudentIntentEnvelope(
                "0.1", "ready_for_confirmation", "请确认", visibleIntent,
                Map.of("observable_outcome", "explicit"), List.of(), List.of(), null);
        var session = new CreationRepository.SessionRecord(
                "session-4", 7L, "direction_ready", ready, null, 1,
                null, null, null, null, null, null);
        when(repository.lock("session-4", 7L)).thenReturn(session);

        ConfirmedIntent confirmed = service.confirmDirection("session-4", 7L);

        assertEquals(visibleIntent, confirmed.intent());
        verify(harness, never()).clarify(any(), any(), anyBoolean());
        ArgumentCaptor<ConfirmedIntent> saved = ArgumentCaptor.forClass(ConfirmedIntent.class);
        verify(repository).confirmDirection(eq("session-4"), any(), saved.capture(), eq(confirmed.intentHash()));
        assertEquals(visibleIntent, saved.getValue().intent());
    }
}
