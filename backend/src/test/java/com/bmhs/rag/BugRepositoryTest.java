package com.bmhs.rag;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class BugRepositoryTest {
    @Test
    void failedIndexUpdateUsesBoundedAtomicBackoff() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), eq(14L))).thenReturn(1);
        BugRepository repository = new BugRepository(jdbc);

        repository.markVectorIndexFailed(14L);

        ArgumentCaptor<String> statement = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(statement.capture(), eq(14L));
        String sql = statement.getValue().replaceAll("\\s+", " ");
        assertTrue(sql.contains("vector_index_attempts < 6"));
        assertTrue(sql.contains("WHEN vector_index_attempts < 5"));
        assertTrue(sql.contains("300 * POW(2, vector_index_attempts)"));
        assertTrue(sql.contains("ELSE NULL END"));
    }

    @Test
    void retrySelectionRequiresApprovedDueCaseBelowAttemptLimit() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.any(org.springframework.jdbc.core.RowMapper.class), eq(50)))
                .thenReturn(java.util.List.of());
        BugRepository repository = new BugRepository(jdbc);

        repository.findApprovedNeedingIndex(500);

        ArgumentCaptor<String> statement = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(statement.capture(), org.mockito.ArgumentMatchers.any(org.springframework.jdbc.core.RowMapper.class), eq(50));
        String sql = statement.getValue().replaceAll("\\s+", " ");
        assertTrue(sql.contains("status = 'approved'"));
        assertTrue(sql.contains("vector_index_attempts < 6"));
        assertTrue(sql.contains("vector_index_retry_at IS NULL OR vector_index_retry_at <= UTC_TIMESTAMP(6)"));
        assertTrue(sql.contains("LIMIT ?"));
    }

    @Test
    void teacherReapprovalResetsRetryState() {
        java.time.Instant now = java.time.Instant.now();
        RagModels.BugCaseView pending = new RagModels.BugCaseView(14L, 5L, 11L, 8L, 4L, 7L,
                "依赖冲突", "启动时报错", "固定版本后启动", "Python", "pending", "personal",
                "failed", "bug-14", now, now);
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate(pending);
        BugRepository repository = new BugRepository(jdbc);

        repository.review(14L, 3L, new RagModels.BugReviewRequest("approved", "再次校验"));

        assertTrue(jdbc.updates.stream().anyMatch(sql -> sql.contains("vector_index_attempts = 0")
                && sql.contains("vector_index_retry_at = NULL")));
    }

    @Test
    void rejectedVectorCleanupRetryIsLimitedToRejectedCasesWithIndexedDocuments() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.any(RowMapper.class), eq(50)))
                .thenReturn(List.of());
        BugRepository repository = new BugRepository(jdbc);

        repository.findRejectedNeedingVectorCleanup(500);

        ArgumentCaptor<String> statement = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(statement.capture(), org.mockito.ArgumentMatchers.any(RowMapper.class), eq(50));
        String sql = statement.getValue().replaceAll("\\s+", " ");
        assertTrue(sql.contains("status = 'rejected'"));
        assertTrue(sql.contains("vector_status = 'failed'"));
        assertTrue(sql.contains("chroma_document_id IS NOT NULL"));
        assertTrue(sql.contains("vector_index_attempts < 6"));
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private final RagModels.BugCaseView bug;
        private final List<String> updates = new ArrayList<>();

        private RecordingJdbcTemplate(RagModels.BugCaseView bug) { this.bug = bug; }

        @Override
        @SuppressWarnings("unchecked")
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            return (List<T>) List.of(bug);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T query(String sql, ResultSetExtractor<T> extractor, Object... args) {
            return (T) Integer.valueOf(1);
        }

        @Override
        public int update(String sql, Object... args) {
            updates.add(sql);
            return 1;
        }
    }
}
