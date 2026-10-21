package com.bmhs.dsh;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DshInstanceServiceTest {
    private final DshInstanceRepository repository = mock(DshInstanceRepository.class);
    private final DshTokenRepository tokenRepository = mock(DshTokenRepository.class);
    private final DshTokenService tokenService = mock(DshTokenService.class);
    private final DshContainerRuntime runtime = mock(DshContainerRuntime.class);
    private final DshInstanceService service = new DshInstanceService(repository, tokenRepository, tokenService, runtime,
            "docker", "dsh:test", "https://bmhs.example.test", "https://bmhs.example.test/dsh",
            true, 1.0, 2048, 256, Duration.ofSeconds(20));
    private final AuthenticatedUser student = new AuthenticatedUser(7, "s@example.test", "Student", "student");

    @Test
    void launchCreatesIsolatedLogicalInstanceAndReturnsOnlyItsLaunchUrl() {
        DshInstanceModels.Instance stopped = instance("stopped", null, null);
        DshTokenService.CredentialPair credentials = credentials();
        when(repository.getOrCreate(eq(7L), anyString(), anyString(), eq(1.0), eq(2048), eq(256))).thenReturn(stopped);
        when(repository.claimStart(12)).thenReturn(true);
        when(tokenService.issuePair(7, 12)).thenReturn(credentials);
        when(runtime.start(any())).thenReturn(new DshInstanceModels.RuntimeHandle("abcdef123456", 49152,
                "https://bmhs.example.test/dsh/12345678-1234-1234-1234-123456789012/?token=private"));
        when(repository.markRunning(12, "abcdef123456", 49152)).thenReturn(true);
        when(repository.findByStudent(7)).thenReturn(Optional.of(instance("running", "abcdef123456", 49152)));

        DshInstanceModels.InstanceView result = service.launch(student);

        assertEquals("running", result.status());
        assertTrue(result.launchUrl().contains("?token="));
        verify(runtime).start(argThat(spec -> spec.cpuLimit() == 1.0 && spec.memoryLimitMb() == 2048
                && spec.pidsLimit() == 256 && spec.workspaceRef().equals(stopped.routeKey())));
        verify(repository, never()).markError(12);
    }

    @Test
    void launchFailureRevokesBothCredentialsAndMarksInstanceError() {
        when(repository.getOrCreate(eq(7L), anyString(), anyString(), anyDouble(), anyInt(), anyInt())).thenReturn(instance("stopped", null, null));
        when(repository.claimStart(12)).thenReturn(true);
        when(tokenService.issuePair(7, 12)).thenReturn(credentials());
        when(runtime.start(any())).thenThrow(new IllegalStateException("docker unavailable"));

        ApiException error = assertThrows(ApiException.class, () -> service.launch(student));

        assertEquals("DSH_START_FAILED", error.code());
        verify(tokenRepository).revokeStudentTokens(7, 12);
        verify(tokenRepository).revokeStudentRefreshTokens(7, 12);
        verify(repository).markError(12);
    }

    @Test
    void reconnectReusesTheRunningContainerAndPersistentWorkspace() {
        DshInstanceModels.Instance running = instance("running", "abcdef123456", 49152);
        when(repository.getOrCreate(eq(7L), anyString(), anyString(), eq(1.0), eq(2048), eq(256))).thenReturn(running);
        when(runtime.currentLaunchUrl(eq("docker"), contains(running.routeKey()), eq("abcdef123456"), any()))
                .thenReturn("https://bmhs.example.test/dsh/12345678-1234-1234-1234-123456789012/?token=once");

        DshInstanceModels.InstanceView result = service.launch(student);

        assertEquals("running", result.status());
        verify(runtime, never()).start(any());
        verify(repository, never()).claimStart(anyLong());
        verify(tokenService, never()).issuePair(anyLong(), anyLong());
    }

    @Test
    void concurrentLaunchDoesNotCreateASecondContainer() {
        DshInstanceModels.Instance stopped = instance("stopped", null, null);
        when(repository.getOrCreate(eq(7L), anyString(), anyString(), eq(1.0), eq(2048), eq(256))).thenReturn(stopped);
        when(repository.claimStart(12)).thenReturn(false);
        when(repository.findByStudent(7)).thenReturn(Optional.of(instance("provisioning", null, null)));

        DshInstanceModels.InstanceView result = service.launch(student);

        assertEquals("provisioning", result.status());
        verify(runtime, never()).start(any());
        verify(tokenService, never()).issuePair(anyLong(), anyLong());
    }

    @Test
    void proxyTargetOnlyResolvesTheAuthenticatedStudentsOpaqueRoute() {
        DshInstanceModels.Instance running = instance("running", "abcdef123456", 49152);
        when(repository.findByStudent(7)).thenReturn(Optional.of(running));
        when(repository.proxyPort(running.routeKey(), 7)).thenReturn(Optional.of(49152));

        assertEquals(49152, service.proxyPort(student, running.routeKey()));
        verify(repository).markActive(12);
        assertThrows(ApiException.class, () -> service.proxyPort(student, "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    }

    @Test
    void idleCleanupStopsContainerRevokesCredentialsAndKeepsPersistentVolumeRecord() {
        DshInstanceModels.Instance idle = instance("running", "abcdef123456", 49152);
        when(repository.findIdle(any(), eq(50))).thenReturn(List.of(idle));
        when(repository.claimStop(eq(12L), any())).thenReturn(true);

        service.stopIdleInstances();

        verify(runtime).stop(eq("docker"), eq("abcdef123456"), contains("bmhs-dsh-net-"), any());
        verify(tokenRepository).revokeStudentTokens(7, 12);
        verify(tokenRepository).revokeStudentRefreshTokens(7, 12);
        verify(repository).markStopped(12);
    }

    @Test
    void nonStudentCannotLaunchAndMissingRuntimeConfigFailsClosed() {
        AuthenticatedUser teacher = new AuthenticatedUser(8, "t@example.test", "Teacher", "teacher");
        assertThrows(ApiException.class, () -> service.launch(teacher));

        DshInstanceService unconfigured = new DshInstanceService(repository, tokenRepository, tokenService, runtime,
                "docker", "", "", "", true, 1, 2048, 256, Duration.ofSeconds(20));
        ApiException error = assertThrows(ApiException.class, () -> unconfigured.launch(student));
        assertEquals("DSH_NOT_CONFIGURED", error.code());
        verifyNoInteractions(runtime);
    }

    private DshTokenService.CredentialPair credentials() {
        return new DshTokenService.CredentialPair(new DshTokenService.IssuedToken("a".repeat(43), Instant.now().plusSeconds(900)),
                "b".repeat(43));
    }

    private DshInstanceModels.Instance instance(String status, String containerId, Integer port) {
        return new DshInstanceModels.Instance(12, 7, "12345678-1234-1234-1234-123456789012", containerId,
                "bmhs-dsh-data-1234567890", port, status, 1.0, 2048, 256, Instant.now().minusSeconds(3600));
    }
}
