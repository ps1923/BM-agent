package com.bmhs.rag;

import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagServiceTest {
    @Test
    void approvedBugFlowsThroughChromaIndexNodeScopedSearchAndDatabaseAuthorization() throws Exception {
        AtomicReference<String> upsertBody = new AtomicReference<>();
        AtomicReference<String> queryBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/collections", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String response;
            if (path.endsWith("/upsert")) {
                upsertBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                response = "null";
            } else if (path.endsWith("/query")) {
                queryBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                response = "{\"ids\":[[\"bug-21\"]],\"distances\":[[0.2]]}";
            } else {
                response = "{\"id\":\"collection-1\"}";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            BugRepository repository = mock(BugRepository.class);
            EmbeddingClient embedding = mock(EmbeddingClient.class);
            ChromaClient chroma = new ChromaClient("http://127.0.0.1:" + server.getAddress().getPort(),
                    "rag_e2e", "", Duration.ofSeconds(2));
            RagService service = new RagService(repository, embedding, chroma, 0.5);
            java.time.Instant now = java.time.Instant.now();
            RagModels.BugCaseView approved = new RagModels.BugCaseView(21L, 3L, 10L, 9L, 4L, 8L,
                    "Django 路由未注册", "页面返回 404", "在项目 urls.py 注册应用路由", "Django",
                    "approved", "course", "not_indexed", null, now, now);
            when(embedding.embed("Django 路由未注册\n问题：页面返回 404\n解决：在项目 urls.py 注册应用路由"))
                    .thenReturn(List.of(0.1, 0.2));
            when(embedding.embed("节点 3.1 Django 页面路由"))
                    .thenReturn(List.of(0.3, 0.4));
            when(repository.findApprovedForExperiment(21L, 7L, 3L)).thenReturn(approved);
            when(repository.markVectorIndexSucceeded(21L, "bug-21")).thenReturn(true);

            service.indexApproved(approved);
            RagModels.RagSearchResult result = service.searchForExperiment(7L, 3L, 9L, "Django",
                    "节点 3.1 Django 页面路由");

            assertTrue(result.available());
            assertEquals(List.of(21L), result.references().stream()
                    .map(RagModels.RagReference::bugCaseId).toList());
            assertTrue(upsertBody.get().contains("\"experiment_id\":\"3\""));
            assertTrue(upsertBody.get().contains("\"node_id\":\"9\""));
            assertTrue(queryBody.get().contains("\"experiment_id\""));
            assertTrue(queryBody.get().contains("\"node_id\""));
            verify(repository).markVectorIndexSucceeded(21L, "bug-21");
            verify(repository).findApprovedForExperiment(21L, 7L, 3L);
        } finally {
            server.stop(0);
        }
    }

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
    void personalExperimentSearchUsesExperimentOnlyAndDatabaseAuthorization() {
        BugRepository repository = mock(BugRepository.class);
        EmbeddingClient embedding = mock(EmbeddingClient.class);
        ChromaClient chroma = mock(ChromaClient.class);
        when(embedding.embed("依赖错误")).thenReturn(List.of(0.1));
        when(chroma.queryByExperiment(anyList(), isNull(), eq(3L), isNull(), isNull(), eq(100)))
                .thenReturn(List.of(new RagModels.ChromaHit("bug-8", 0.1)));
        java.time.Instant now = java.time.Instant.now();
        when(repository.findApprovedForExperiment(8L, 7L, 3L)).thenReturn(
                new RagModels.BugCaseView(8L, 3L, 10L, null, null, 9L, "依赖", "缺少依赖", "安装后解决",
                        "Python", "approved", "course", "indexed", "bug-8", now, now));

        RagModels.RagSearchResult result = new RagService(repository, embedding, chroma)
                .searchForExperiment(7L, 3L, null, null, "依赖错误");

        assertTrue(result.available());
        assertEquals(List.of(8L), result.references().stream().map(RagModels.RagReference::bugCaseId).toList());
        verify(chroma).queryByExperiment(anyList(), isNull(), eq(3L), isNull(), isNull(), eq(100));
        verify(repository).findApprovedForExperiment(8L, 7L, 3L);
    }

    @Test
    void searchScopesResultsToCourseExperimentAndCompatibleNode() {
        BugRepository repository = mock(BugRepository.class);
        EmbeddingClient embedding = mock(EmbeddingClient.class);
        ChromaClient chroma = mock(ChromaClient.class);
        when(embedding.embed("接口错误")).thenReturn(List.of(0.1));
        when(chroma.query(anyList(), eq(4L), eq(2L), eq(9L), isNull(), eq(100))).thenReturn(List.of(
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
        verify(chroma).query(anyList(), eq(4L), eq(2L), eq(9L), isNull(), eq(100));
    }

    @Test
    void searchScopesTechnologyStackAndAllowsGenericCases() {
        BugRepository repository = mock(BugRepository.class);
        EmbeddingClient embedding = mock(EmbeddingClient.class);
        ChromaClient chroma = mock(ChromaClient.class);
        when(embedding.embed("启动失败")).thenReturn(List.of(0.2));
        when(chroma.query(anyList(), eq(4L), eq(2L), eq(9L), eq("Python"), eq(100))).thenReturn(List.of(
                new RagModels.ChromaHit("bug-4", 0.1), new RagModels.ChromaHit("bug-5", 0.2)));
        java.time.Instant now = java.time.Instant.now();
        when(repository.findVisibleByStudent(4L, 7L)).thenReturn(
                new RagModels.BugCaseView(4L, 2L, 10L, 9L, 4L, 7L, "Python", "现象", "修复", "Python", "approved", "course", "indexed", "bug-4", now, now));
        when(repository.findVisibleByStudent(5L, 7L)).thenReturn(
                new RagModels.BugCaseView(5L, 2L, 10L, 9L, 4L, 7L, "通用", "现象", "修复", null, "approved", "course", "indexed", "bug-5", now, now));

        RagModels.RagSearchResult result = new RagService(repository, embedding, chroma)
                .searchForStudent(7L, 4L, 2L, 9L, "Python", "启动失败");

        assertEquals(List.of(4L, 5L), result.references().stream().map(RagModels.RagReference::bugCaseId).toList());
        verify(chroma).query(anyList(), eq(4L), eq(2L), eq(9L), eq("Python"), eq(100));
    }

    @Test
    void experimentSearchFiltersWeakMatchesAndRequestsOnlyNodeCompatibleMemories() {
        BugRepository repository = mock(BugRepository.class);
        EmbeddingClient embedding = mock(EmbeddingClient.class);
        ChromaClient chroma = mock(ChromaClient.class);
        when(embedding.embed("Django 首页路由")).thenReturn(List.of(0.1));
        when(chroma.queryByExperiment(anyList(), isNull(), eq(3L), eq(9L), isNull(), eq(100)))
                .thenReturn(List.of(new RagModels.ChromaHit("bug-1", 0.2),
                        new RagModels.ChromaHit("bug-2", 0.9)));
        java.time.Instant now = java.time.Instant.now();
        when(repository.findApprovedForExperiment(1L, 7L, 3L)).thenReturn(
                new RagModels.BugCaseView(1L, 3L, 10L, 9L, 4L, 8L, "路由缺失", "现象", "修复",
                        "Django", "approved", "course", "indexed", "bug-1", now, now));

        RagModels.RagSearchResult result = new RagService(repository, embedding, chroma, 0.5)
                .searchForExperiment(7L, 3L, 9L, null, "Django 首页路由");

        assertTrue(result.available());
        assertEquals(List.of(1L), result.references().stream().map(RagModels.RagReference::bugCaseId).toList());
        verify(chroma).queryByExperiment(anyList(), isNull(), eq(3L), eq(9L), isNull(), eq(100));
        verify(repository).findApprovedForExperiment(1L, 7L, 3L);
        org.mockito.Mockito.verifyNoMoreInteractions(repository);
    }

    @Test
    void experimentDatabaseCheckRejectsAnUnexpectedCrossExperimentVectorHit() {
        BugRepository repository = mock(BugRepository.class);
        EmbeddingClient embedding = mock(EmbeddingClient.class);
        ChromaClient chroma = mock(ChromaClient.class);
        when(embedding.embed("同名目标")).thenReturn(List.of(0.1));
        when(chroma.queryByExperiment(anyList(), isNull(), eq(3L), isNull(), isNull(), eq(100)))
                .thenReturn(List.of(new RagModels.ChromaHit("bug-99", 0.1)));
        when(repository.findApprovedForExperiment(99L, 7L, 3L)).thenThrow(
                new com.bmhs.experimentcreation.ApiException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "BUG_CASE_NOT_FOUND", "实验记忆不存在"));

        RagModels.RagSearchResult result = new RagService(repository, embedding, chroma)
                .searchForExperiment(7L, 3L, null, null, "同名目标");

        assertTrue(result.available());
        assertEquals(List.of(), result.references());
        verify(repository).findApprovedForExperiment(99L, 7L, 3L);
    }

    @Test
    void failedIndexingIsRecordedAndScheduledRetryReprocessesApprovedCases() {
        BugRepository repository = mock(BugRepository.class);
        EmbeddingClient embedding = mock(EmbeddingClient.class);
        ChromaClient chroma = mock(ChromaClient.class);
        java.time.Instant now = java.time.Instant.now();
        RagModels.BugCaseView approved = new RagModels.BugCaseView(1L, 3L, 10L, 9L, 4L, 8L,
                "标题", "问题", "修复", "Django", "approved", "course", "failed", null, now, now);
        when(embedding.embed("标题\n问题：问题\n解决：修复")).thenReturn(List.of(0.1));
        org.mockito.Mockito.doThrow(new com.bmhs.experimentcreation.ApiException(
                org.springframework.http.HttpStatus.BAD_GATEWAY, "CHROMA_REQUEST_FAILED", "Chroma unavailable"))
                .when(chroma).upsert(org.mockito.ArgumentMatchers.any(), anyList());
        when(repository.markVectorIndexFailed(1L)).thenReturn(true);

        RagService service = new RagService(repository, embedding, chroma);
        service.indexApproved(approved);
        verify(repository).markVectorIndexFailed(1L);

        when(repository.findApprovedNeedingIndex(10)).thenReturn(List.of(approved));
        org.mockito.Mockito.doNothing().when(chroma).upsert(org.mockito.ArgumentMatchers.any(), anyList());
        when(repository.markVectorIndexSucceeded(1L, "bug-1")).thenReturn(true);
        service.retryApprovedIndexes();

        verify(repository).markVectorIndexSucceeded(1L, "bug-1");
    }

    @Test
    void timedOutUpsertThatRacesWithRejectionSchedulesVectorCleanup() {
        BugRepository repository = mock(BugRepository.class);
        EmbeddingClient embedding = mock(EmbeddingClient.class);
        ChromaClient chroma = mock(ChromaClient.class);
        java.time.Instant now = java.time.Instant.now();
        RagModels.BugCaseView approved = new RagModels.BugCaseView(6L, 3L, 10L, 9L, 4L, 8L,
                "案例", "问题", "修复", "Django", "approved", "course", "not_indexed", null, now, now);
        RagModels.BugCaseView rejected = new RagModels.BugCaseView(6L, 3L, 10L, 9L, 4L, 8L,
                "案例", "问题", "修复", "Django", "rejected", "personal", "not_indexed", null, now, now);
        RagModels.BugCaseView cleanupPending = new RagModels.BugCaseView(6L, 3L, 10L, 9L, 4L, 8L,
                "案例", "问题", "修复", "Django", "rejected", "personal", "failed", "bug-6", now, now);
        when(embedding.embed("案例\n问题：问题\n解决：修复")).thenReturn(List.of(0.1));
        org.mockito.Mockito.doThrow(new com.bmhs.experimentcreation.ApiException(
                org.springframework.http.HttpStatus.GATEWAY_TIMEOUT, "CHROMA_TIMEOUT", "Chroma timeout"))
                .when(chroma).upsert(org.mockito.ArgumentMatchers.any(), anyList());
        when(repository.markVectorIndexFailed(6L)).thenReturn(false);
        when(repository.findById(6L)).thenReturn(rejected, cleanupPending);
        when(repository.markRejectedVectorCleanupSucceeded(6L)).thenReturn(true);

        new RagService(repository, embedding, chroma).indexApproved(approved);

        verify(repository).markRejectedVectorPendingCleanup(6L);
        verify(chroma).delete("bug-6");
        verify(repository).markRejectedVectorCleanupSucceeded(6L);
    }

    @Test
    void rejectedVectorCleanupIsRetriedWhenChromaDeleteFails() {
        BugRepository repository = mock(BugRepository.class);
        EmbeddingClient embedding = mock(EmbeddingClient.class);
        ChromaClient chroma = mock(ChromaClient.class);
        java.time.Instant now = java.time.Instant.now();
        RagModels.BugCaseView rejected = new RagModels.BugCaseView(6L, 3L, 10L, 9L, 4L, 8L,
                "已拒绝案例", "问题", "解决", "Django", "rejected", "personal", "failed", "bug-6", now, now);
        org.mockito.Mockito.doThrow(new com.bmhs.experimentcreation.ApiException(
                org.springframework.http.HttpStatus.BAD_GATEWAY, "CHROMA_REQUEST_FAILED", "Chroma unavailable"))
                .when(chroma).delete("bug-6");
        when(repository.findRejectedNeedingVectorCleanup(10)).thenReturn(List.of(rejected));

        new RagService(repository, embedding, chroma).retryApprovedIndexes();

        verify(chroma).delete("bug-6");
        verify(repository).markRejectedVectorCleanupFailed(6L);
    }
}
