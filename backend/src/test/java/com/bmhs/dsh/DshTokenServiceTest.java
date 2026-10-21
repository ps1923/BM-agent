package com.bmhs.dsh;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DshTokenServiceTest {
    private final DshTokenRepository repository = mock(DshTokenRepository.class);
    private final DshTokenService service = new DshTokenService(repository);

    @Test
    void issuePairPersistsOnlyHashesAndReturnsShortLivedAccessCredential() {
        DshTokenService.CredentialPair pair = service.issuePair(7, 12);

        assertEquals(43, pair.access().token().length());
        assertEquals(43, pair.refreshCredential().length());
        assertTrue(pair.access().expiresAt().isAfter(Instant.now()));
        verify(repository).save(argThat(hash -> hash.matches("[0-9a-f]{64}")), eq(7L), eq(12L), any());
        verify(repository).saveRefresh(argThat(hash -> hash.matches("[0-9a-f]{64}")), eq(7L), eq(12L), any());
    }

    @Test
    void refreshConsumesOldCredentialAndIssuesAReplacementPair() {
        when(repository.consumeRefresh(anyString(), any())).thenReturn(new DshPrincipal(7, 12, Instant.now().plusSeconds(60)));

        DshTokenService.CredentialPair pair = service.refresh("b".repeat(43));

        assertEquals(43, pair.access().token().length());
        assertEquals(43, pair.refreshCredential().length());
        verify(repository).consumeRefresh(argThat(hash -> hash.matches("[0-9a-f]{64}")), any());
        verify(repository).save(anyString(), eq(7L), eq(12L), any());
        verify(repository).saveRefresh(anyString(), eq(7L), eq(12L), any());
    }

    @Test
    void invalidOrConsumedRefreshCredentialIsRejected() {
        assertThrows(RuntimeException.class, () -> service.refresh("short"));
        when(repository.consumeRefresh(anyString(), any())).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.refresh("b".repeat(43)));
        verify(repository, never()).saveRefresh(anyString(), anyLong(), anyLong(), any());
    }
}
