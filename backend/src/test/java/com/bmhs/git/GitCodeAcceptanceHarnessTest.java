package com.bmhs.git;

import com.bmhs.guidance.GuidanceModelClient;
import com.bmhs.guidance.GuidanceModels;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitCodeAcceptanceHarnessTest {
    private final GuidanceModelClient model = mock(GuidanceModelClient.class);
    private final GitCodeAcceptanceHarness harness = new GitCodeAcceptanceHarness(model);

    @Test
    void positiveDecisionWithoutVerifiableEvidenceIsDowngraded() {
        when(model.complete(anyString(), anyString())).thenReturn(new GuidanceModels.ModelReply("""
                {"results":[{"nodeId":7,"decision":"meets","summary":"符合","evidence":[],"suggestions":[],"bugDraft":null}]}
                """, "test-model"));

        List<GitModels.RawResult> results = harness.evaluate("a".repeat(40),
                List.of(new GitModels.NodeSpec(7L, "1", "创建页面", "页面应可用", List.of("渲染首页"))),
                List.of(new GitModels.CommitFile("src/page.js", "blob", "export default function Page() {}")), List.of());

        assertEquals("unable_to_verify", results.get(0).decision());
        assertEquals("模型未提供可核对的代码证据，暂不能判定该节点符合。", results.get(0).summary());
        assertEquals(List.of(), results.get(0).evidence());
    }

    @Test
    void evidenceMustMatchFileTextAtClaimedLine() {
        when(model.complete(anyString(), anyString())).thenReturn(new GuidanceModels.ModelReply("""
                {"results":[{"nodeId":7,"decision":"meets","summary":"符合","evidence":[{"path":"src/page.js","lineStart":1,"lineEnd":1,"excerpt":"missing code"}],"suggestions":[],"bugDraft":null}]}
                """, "test-model"));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> harness.evaluate("a".repeat(40),
                List.of(new GitModels.NodeSpec(7L, "1", "创建页面", "页面应可用", List.of())),
                List.of(new GitModels.CommitFile("src/page.js", "blob", "export default function Page() {}")), List.of()));
    }
}
