package com.bmhs.experimentcreation;

import com.bmhs.experimentcreation.CreationModels.StudentIntentEnvelope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreationTransactionsTest {
    @Test
    void staleConcurrentDirectionTurnCannotOverwriteNewerSessionState() {
        CreationRepository repository = mock(CreationRepository.class);
        CreationTransactions transactions = new CreationTransactions(repository);
        var snapshot = session("direction_discussion", 1);
        var current = session("direction_ready", 2);
        StudentIntentEnvelope result = new StudentIntentEnvelope(
                "0.1", "ready_for_confirmation", "请确认", Map.of("raw_request", "目标"),
                Map.of(), List.of(), List.of(), null);
        when(repository.lock("session", 9L)).thenReturn(current);

        assertThrows(ApiException.class,
                () -> transactions.saveDirectionTurn(snapshot, "补充", result));

        verify(repository, never()).appendMessage(any(), any(), any());
        verify(repository, never()).saveDirection(any(), any());
    }

    private CreationRepository.SessionRecord session(String status, int version) {
        return new CreationRepository.SessionRecord(
                "session", 9L, status, null, null, version,
                null, null, null, null, null, null);
    }
}
