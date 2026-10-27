package com.bmhs.git;

import com.bmhs.guidance.GuidanceModelClient;
import com.bmhs.guidance.GuidanceModels;
import com.bmhs.rag.RagModels;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import com.bmhs.experimentcreation.CreationModels.AcceptanceCriterion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitCodeAcceptanceHarnessTest {
    private final GuidanceModelClient model = mock(GuidanceModelClient.class);
    private final GitCodeAcceptanceHarness harness = new GitCodeAcceptanceHarness(model);

    @Test
    void positiveDecisionWithoutVerifiableEvidenceIsDowngraded() {
        when(model.completeJson(anyString(), anyString())).thenReturn(new GuidanceModels.ModelReply("""
                {"results":[{"nodeId":7,"decision":"meets","summary":"符合","evidence":[],"suggestions":[],"criterionResults":[{"criterionIndex":0,"decision":"meets","summary":"符合","evidence":[]}],"bugDraft":null}]}
                """, "test-model"));

        List<GitModels.RawResult> results = harness.evaluate("a".repeat(40),
                List.of(new GitModels.NodeSpec(7L, "1", "创建页面", "页面应可用", List.of("渲染首页"))),
                List.of(new GitModels.CommitFile("src/page.js", "blob", "export default function Page() {}")), Map.of());

        assertEquals("unable_to_verify", results.get(0).decision());
        assertEquals("至少一项验收准则缺少可核验的提交证据，暂不能判定整体符合。", results.get(0).summary());
        assertEquals(List.of(), results.get(0).evidence());
    }

    @Test
    void unverifiableEvidenceIsDiscardedAndPositiveDecisionIsDowngraded() {
        when(model.completeJson(anyString(), anyString())).thenReturn(new GuidanceModels.ModelReply("""
                {"results":[{"nodeId":7,"decision":"meets","summary":"符合","evidence":[{"path":"src/page.js","lineStart":1,"lineEnd":1,"excerpt":"missing code"}],"suggestions":[],"criterionResults":[{"criterionIndex":0,"decision":"meets","summary":"符合","evidence":[{"path":"src/page.js","lineStart":1,"lineEnd":1,"excerpt":"missing code"}]}],"bugDraft":null}]}
                """, "test-model"));

        List<GitModels.RawResult> results = harness.evaluate("a".repeat(40),
                List.of(new GitModels.NodeSpec(7L, "1", "创建页面", "页面应可用", List.of())),
                List.of(new GitModels.CommitFile("src/page.js", "blob", "export default function Page() {}")), Map.of());

        assertEquals("unable_to_verify", results.get(0).decision());
        assertEquals(List.of(), results.get(0).evidence());
    }

    @Test
    void negativeDecisionWithoutVerifiableCriterionEvidenceIsDowngraded() {
        when(model.completeJson(anyString(), anyString())).thenReturn(new GuidanceModels.ModelReply("""
                {"results":[{"nodeId":7,"decision":"does_not_meet","summary":"未通过","evidence":[],"suggestions":[],"criterionResults":[{"criterionIndex":0,"decision":"does_not_meet","summary":"缺少页面","evidence":[]}],"bugDraft":null}]}
                """, "test-model"));

        List<GitModels.RawResult> results = harness.evaluate("a".repeat(40),
                List.of(new GitModels.NodeSpec(7L, "1", "创建页面", "页面应可用", List.of(),
                        List.of(new AcceptanceCriterion("页面包含标题", "static_code", "页面组件源码")), 1, "generated")),
                List.of(new GitModels.CommitFile("src/page.js", "blob", "export default function Page() {}")), Map.of());

        assertEquals("unable_to_verify", results.get(0).decision());
        assertEquals("unable_to_verify", results.get(0).criterionResults().get(0).decision());
        assertEquals("提交中没有支持未满足结论的可核验证据，暂不能判定。", results.get(0).summary());
    }

    @Test
    void promptCarriesBugMemoriesOnlyUnderTheirNodeIds() {
        java.time.Instant now = java.time.Instant.now();
        RagModels.RagReference memory = new RagModels.RagReference(2L, "节点案例", "问题", "解决", 0.2);
        when(model.completeJson(anyString(), anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(1);
            org.junit.jupiter.api.Assertions.assertTrue(prompt.contains("approvedBugMemoryByNode"));
            org.junit.jupiter.api.Assertions.assertTrue(prompt.contains("\"7\":[{\"bugCaseId\":2"));
            org.junit.jupiter.api.Assertions.assertTrue(prompt.contains("\"8\":[]"));
            return new GuidanceModels.ModelReply("""
                    {"results":[
                      {"nodeId":7,"decision":"unable_to_verify","summary":"待核验","evidence":[],"suggestions":[],"criterionResults":[{"criterionIndex":0,"decision":"unable_to_verify","summary":"待核验","evidence":[]}],"bugDraft":null},
                      {"nodeId":8,"decision":"unable_to_verify","summary":"待核验","evidence":[],"suggestions":[],"criterionResults":[{"criterionIndex":0,"decision":"unable_to_verify","summary":"待核验","evidence":[]}],"bugDraft":null}
                    ]}
                    """, "test-model");
        });

        List<GitModels.RawResult> results = harness.evaluate("a".repeat(40), List.of(
                        new GitModels.NodeSpec(7L, "1", "节点一", "目标一", List.of(),
                                List.of(new AcceptanceCriterion("条件一", "static_code", "文件一")), 1, "generated"),
                        new GitModels.NodeSpec(8L, "2", "节点二", "目标二", List.of(),
                                List.of(new AcceptanceCriterion("条件二", "static_code", "文件二")), 1, "generated")),
                List.of(new GitModels.CommitFile("README.md", "blob", "说明")), Map.of(7L, List.of(memory), 8L, List.of()));

        assertEquals(List.of(7L, 8L), results.stream().map(GitModels.RawResult::nodeId).toList());
    }
}
