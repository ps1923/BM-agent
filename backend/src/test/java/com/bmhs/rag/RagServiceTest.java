package com.bmhs.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagServiceTest {
    @Test
    void personalRunWithoutCourseDoesNotQueryVectorStore() {
        BugRepository repository = mock(BugRepository.class);
        EmbeddingClient embedding = mock(EmbeddingClient.class);
        ChromaClient chroma = mock(ChromaClient.class);
        RagModels.RagSearchResult result = new RagService(repository, embedding, chroma)
                .searchForStudent(7L, null, "依赖错误");

        assertFalse(result.available());
        assertEquals(List.of(), result.references());
        org.mockito.Mockito.verifyNoInteractions(embedding, chroma);
    }

    @Test
    void indexingNonApprovedCaseDoesNothing() {
        BugRepository repository = mock(BugRepository.class);
        EmbeddingClient embedding = mock(EmbeddingClient.class);
        ChromaClient chroma = mock(ChromaClient.class);
        RagModels.BugCaseView pending = new RagModels.BugCaseView(1L, 2L, 3L, null, 4L, 7L,
                "标题", "问题", "解决", "pending", "personal", "not_indexed", null,
                java.time.Instant.now(), java.time.Instant.now());

        new RagService(repository, embedding, chroma).indexApproved(pending);

        org.mockito.Mockito.verifyNoInteractions(embedding, chroma);
    }

    @Test
    void searchScopesResultsToCourseExperimentAndCompatibleNode() {
        BugRepository repository = mock(BugRepository.class);
        EmbeddingClient embedding = mock(EmbeddingClient.class);
        ChromaClient chroma = mock(ChromaClient.class);
        when(embedding.embed("接口错误")).thenReturn(List.of(0.1));
        when(chroma.query(anyList(), eq(4L), eq(2L), isNull(), eq(8))).thenReturn(List.of(
                new RagModels.ChromaHit("bug-1", 0.1),
                new RagModels.ChromaHit("bug-2", 0.2),
                new RagModels.ChromaHit("bug-3", 0.3)));
        java.time.Instant now = java.time.Instant.now();
        when(repository.findVisibleByStudent(1L, 7L)).thenReturn(
                new RagModels.BugCaseView(1L, 2L, 10L, 9L, 4L, 7L, "接口错误", "现象", "修复", "approved", "course", "indexed", "bug-1", now, now));
        when(repository.findVisibleByStudent(2L, 7L)).thenReturn(
                new RagModels.BugCaseView(2L, 3L, 11L, 9L, 4L, 7L, "其他实验", "现象", "修复", "approved", "course", "indexed", "bug-2", now, now));
        when(repository.findVisibleByStudent(3L, 7L)).thenReturn(
                new RagModels.BugCaseView(3L, 2L, 12L, 10L, 4L, 7L, "其他节点", "现象", "修复", "approved", "course", "indexed", "bug-3", now, now));

        RagModels.RagSearchResult result = new RagService(repository, embedding, chroma)
                .searchForStudent(7L, 4L, 2L, 9L, "接口错误");

        assertEquals(List.of(1L), result.references().stream().map(RagModels.RagReference::bugCaseId).toList());
        assertEquals(true, result.available());
        verify(chroma).query(anyList(), eq(4L), eq(2L), isNull(), eq(8));
    }

    @Test
    void searchScopesTechnologyStackAndAllowsGenericCases() {
        BugRepository repository = mock(BugRepository.class);
        EmbeddingClient embedding = mock(EmbeddingClient.class);
        ChromaClient chroma = mock(ChromaClient.class);
        when(embedding.embed("启动失败")).thenReturn(List.of(0.2));
        when(chroma.query(anyList(), eq(4L), eq(2L), eq("Python"), eq(8))).thenReturn(List.of(
                new RagModels.ChromaHit("bug-4", 0.1), new RagModels.ChromaHit("bug-5", 0.2)));
        java.time.Instant now = java.time.Instant.now();
        when(repository.findVisibleByStudent(4L, 7L)).thenReturn(
                new RagModels.BugCaseView(4L, 2L, 10L, 9L, 4L, 7L, "Python", "现象", "修复", "Python", "approved", "course", "indexed", "bug-4", now, now));
        when(repository.findVisibleByStudent(5L, 7L)).thenReturn(
                new RagModels.BugCaseView(5L, 2L, 10L, 9L, 4L, 7L, "通用", "现象", "修复", null, "approved", "course", "indexed", "bug-5", now, now));

        RagModels.RagSearchResult result = new RagService(repository, embedding, chroma)
                .searchForStudent(7L, 4L, 2L, 9L, "Python", "启动失败");

        assertEquals(List.of(4L, 5L), result.references().stream().map(RagModels.RagReference::bugCaseId).toList());
        verify(chroma).query(anyList(), eq(4L), eq(2L), eq("Python"), eq(8));
    }
}
