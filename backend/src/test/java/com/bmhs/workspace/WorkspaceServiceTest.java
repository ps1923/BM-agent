package com.bmhs.workspace;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.WorkspaceModels.WorkspaceFile;
import com.bmhs.workspace.WorkspaceModels.WorkspaceRecord;
import com.bmhs.workspace.WorkspaceModels.RunRecord;
import com.bmhs.workspace.WorkspaceModels.SnapshotView;
import com.bmhs.workspace.WorkspaceModels.WriteFileRequest;
import com.bmhs.workspace.WorkspaceModels.RunProgressView;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceServiceTest {
    private final AuthenticatedUser student = new AuthenticatedUser(7L, "student@example.com", "学生", "student");

    @Test
    void commonSecretRedactorRemovesCredentialShapedValuesBeforeModelUse() {
        String content = "spring.datasource.password=do-not-copy\n"
                + "service.url=https://student:do-not-copy@example.test\n"
                + "Authorization: Bearer abc.def.ghi\n"
                + "cloud.key=AKIA1234567890ABCDEF";

        String redacted = WorkspaceService.redactCommonSecrets(content);

        assertFalse(redacted.contains("do-not-copy"));
        assertFalse(redacted.contains("abc.def.ghi"));
        assertFalse(redacted.contains("AKIA1234567890ABCDEF"));
        assertTrue(redacted.contains("[REDACTED]"));
    }

    @Test
    void teacherCannotCreateAStudentRun() {
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        var teacher = new AuthenticatedUser(3L, "teacher@example.com", "教师", "teacher");
        WorkspaceService service = new WorkspaceService(repository, runtime, "unused-root", "test-image",
                BigDecimal.ONE, 512, 100, 1000);

        ApiException exception = assertThrows(ApiException.class, () -> service.createRun(teacher, 31L));

        assertEquals("ROLE_FORBIDDEN", exception.code());
        verify(repository, never()).createOrFindRun(anyLong(), anyLong(), any());
    }

    @Test
    void teacherCannotUpdateStudentTaskProgress() {
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        var teacher = new AuthenticatedUser(3L, "teacher@example.com", "教师", "teacher");
        WorkspaceService service = new WorkspaceService(repository, runtime, "unused-root", "test-image",
                BigDecimal.ONE, 512, 100, 1000);

        ApiException exception = assertThrows(ApiException.class, () -> service.updateTask(teacher, 8L, 20L,
                new WorkspaceModels.UpdateTaskRequest("completed")));

        assertEquals("ROLE_FORBIDDEN", exception.code());
        verify(repository, never()).updateTaskProgress(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void studentTaskProgressUpdateIsScopedToTheAuthenticatedStudent() {
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceService service = new WorkspaceService(repository, runtime, "unused-root", "test-image",
                BigDecimal.ONE, 512, 100, 1000);
        RunProgressView progress = new RunProgressView(8L, 5L, "in_progress", 12L, List.of(), List.of());
        when(repository.updateTaskProgress(8L, 7L, 22L, "completed")).thenReturn(progress);

        RunProgressView result = service.updateTask(student, 8L, 22L,
                new WorkspaceModels.UpdateTaskRequest("completed"));

        assertEquals(progress, result);
        verify(repository).updateTaskProgress(8L, 7L, 22L, "completed");
    }

    @Test
    void writesUtf8FileAndPersistsOnlyMetadata() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-workspace-test");
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findWorkspace(9L, 7L)).thenReturn(workspace);
        WorkspaceFile saved = new WorkspaceFile(9L, "src/Main.java", "Main.java", "java", 5,
                "hash", false, Instant.now());
        when(repository.saveFile(anyLong(), any(), any(), any(), anyLong(), any(), anyBoolean())).thenReturn(saved);

        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);
        WorkspaceFile result = service.writeFile(student, 9L, "src/Main.java", new WriteFileRequest("hello", null));

        assertEquals("src/Main.java", result.path());
        assertEquals("hello", Files.readString(root.resolve("src/Main.java")));
        var order = org.mockito.Mockito.inOrder(runtime, repository);
        order.verify(repository).lockWorkspaceForMutation(9L);
        order.verify(runtime).pause("container");
        order.verify(repository).saveFile(anyLong(), any(), any(), any(), anyLong(), any(), anyBoolean());
        order.verify(runtime).resume("container");
    }

    @Test
    void refusesFileChangesWhenWorkspaceProcessesCannotBePaused() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-workspace-pause-failure-test");
        Path source = root.resolve("Main.java");
        Files.writeString(source, "class Old {}\n");
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findWorkspace(9L, 7L)).thenReturn(workspace);
        org.mockito.Mockito.doThrow(new IllegalStateException("container is busy"))
                .when(runtime).pause("container");
        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.writeFile(student, 9L, "Main.java", new WriteFileRequest("class New {}\n", null)));

        assertEquals("WORKSPACE_QUIESCE_FAILED", exception.code());
        assertEquals("class Old {}\n", Files.readString(source));
        verify(repository, never()).saveFile(anyLong(), any(), any(), any(), anyLong(), any(), anyBoolean());
        verify(runtime, never()).resume("container");
    }

    @Test
    void staleFileHashIsRejectedBeforeWriting() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-workspace-hash-test");
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findWorkspace(9L, 7L)).thenReturn(workspace);
        when(repository.findFile(9L, "Main.java")).thenReturn(new WorkspaceFile(9L, "Main.java", "Main.java",
                "java", 4, "current-hash", false, Instant.now()));
        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.writeFile(student, 9L, "Main.java", new WriteFileRequest("new", "old-hash")));

        assertEquals("FILE_HASH_MISMATCH", exception.code());
        assertTrue(Files.notExists(root.resolve("Main.java")));
        verify(repository, never()).saveFile(anyLong(), any(), any(), any(), anyLong(), any(), anyBoolean());
    }

    @Test
    void restoresPreviousFileWhenMetadataPersistenceFails() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-workspace-write-rollback-test");
        Path source = root.resolve("Main.java");
        Files.writeString(source, "class Old {}\n");
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findWorkspace(9L, 7L)).thenReturn(workspace);
        when(repository.findFile(9L, "Main.java")).thenReturn(new WorkspaceFile(9L, "Main.java", "Main.java",
                "java", 12, "old-hash", true, Instant.now()));
        when(repository.saveFile(anyLong(), any(), any(), any(), anyLong(), any(), anyBoolean()))
                .thenThrow(new IllegalStateException("metadata write failed"));
        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);

        assertThrows(IllegalStateException.class,
                () -> service.writeFile(student, 9L, "Main.java", new WriteFileRequest("class New {}\n", null)));

        assertEquals("class Old {}\n", Files.readString(source));
    }

    @Test
    void fileReplacementPreservesExecutablePermissionsWhenSupported() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-workspace-permissions-test");
        if (!Files.getFileStore(root).supportsFileAttributeView("posix")) return;
        Path source = root.resolve("run.sh");
        Files.writeString(source, "#!/bin/sh\necho old\n");
        var executable = java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-x---");
        Files.setPosixFilePermissions(source, executable);
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findWorkspace(9L, 7L)).thenReturn(workspace);
        when(repository.saveFile(anyLong(), any(), any(), any(), anyLong(), any(), anyBoolean()))
                .thenReturn(new WorkspaceFile(9L, "run.sh", "run.sh", "sh", 16, "hash", false, Instant.now()));
        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);

        service.writeFile(student, 9L, "run.sh", new WriteFileRequest("#!/bin/sh\necho new\n", null));

        assertEquals(executable, Files.getPosixFilePermissions(source));
    }

    @Test
    void restoresTheOriginalFileWhenTheOwningTransactionRollsBack() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-workspace-rollback-test");
        Path source = root.resolve("Main.java");
        Files.writeString(source, "class Old {}\n");
        var originalPermissions = Files.getFileStore(root).supportsFileAttributeView("posix")
                ? java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-----") : null;
        if (originalPermissions != null) Files.setPosixFilePermissions(source, originalPermissions);
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findWorkspace(9L, 7L)).thenReturn(workspace);
        when(repository.findFile(9L, "Main.java")).thenReturn(new WorkspaceFile(9L, "Main.java", "Main.java",
                "java", 12, "old-hash", true, Instant.now()));
        org.mockito.Mockito.doAnswer(invocation -> {
            assertEquals("class Old {}\n", Files.readString(source));
            return null;
        }).when(runtime).resume("container");
        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.writeFile(student, 9L, "Main.java", new WriteFileRequest("class New {}\n", null));
            service.writeFile(student, 9L, "Main.java", new WriteFileRequest("class Latest {}\n", null));
            assertEquals("class Latest {}\n", Files.readString(source));
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCompletion(
                            org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertEquals("class Old {}\n", Files.readString(source));
        if (originalPermissions != null) assertEquals(originalPermissions, Files.getPosixFilePermissions(source));
        verify(runtime).pause("container");
        verify(runtime).resume("container");
    }

    @Test
    void transactionRollbackDoesNotOverwriteAFileChangedByAnotherProcess() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-workspace-external-change-test");
        Path source = root.resolve("Main.java");
        Files.writeString(source, "class Old {}\n");
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findWorkspace(9L, 7L)).thenReturn(workspace);
        when(repository.findFile(9L, "Main.java")).thenReturn(new WorkspaceFile(9L, "Main.java", "Main.java",
                "java", 12, "old-hash", true, Instant.now()));
        when(repository.saveFile(anyLong(), any(), any(), any(), anyLong(), any(), anyBoolean()))
                .thenReturn(new WorkspaceFile(9L, "Main.java", "Main.java", "java", 13, "new-hash", true, Instant.now()));
        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.writeFile(student, 9L, "Main.java", new WriteFileRequest("class New {}\n", null));
            Files.writeString(source, "class External {}\n");
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCompletion(
                            org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertEquals("class External {}\n", Files.readString(source));
    }

    @Test
    void multiFilePatchKeepsContainerPausedUntilEveryFileAndTransactionComplete() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-workspace-patch-pause-test");
        Path mainFile = root.resolve("Main.java");
        Path testFile = root.resolve("MainTest.java");
        String originalMain = "class Main {}\n";
        String originalTest = "class MainTest {}\n";
        Files.writeString(mainFile, originalMain);
        Files.writeString(testFile, originalTest);
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findWorkspace(9L, 7L)).thenReturn(workspace);
        when(repository.findFile(anyLong(), eq("Main.java"))).thenReturn(new WorkspaceFile(9L, "Main.java",
                "Main.java", "java", originalMain.length(), sha256ForTest(originalMain), true, Instant.now()));
        when(repository.findFile(anyLong(), eq("MainTest.java"))).thenReturn(new WorkspaceFile(9L, "MainTest.java",
                "MainTest.java", "java", originalTest.length(), sha256ForTest(originalTest), false, Instant.now()));
        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);
        List<com.bmhs.patch.PatchModels.PatchFile> files = List.of(
                new com.bmhs.patch.PatchModels.PatchFile("Main.java", sha256ForTest(originalMain), "class Main { int value; }\n"),
                new com.bmhs.patch.PatchModels.PatchFile("MainTest.java", sha256ForTest(originalTest), "class MainTest { int value; }\n"));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.applyPatch(student, 9L, files);

            assertEquals("class Main { int value; }\n", Files.readString(mainFile));
            assertEquals("class MainTest { int value; }\n", Files.readString(testFile));
            verify(runtime).pause("container");
            verify(runtime, never()).resume("container");
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCompletion(
                            org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(runtime).resume("container");
    }

    private static String sha256ForTest(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void pathTraversalIsRejected() {
        ApiException exception = assertThrows(ApiException.class,
                () -> WorkspaceService.normalizePath("src/../../secret.txt"));
        assertEquals("INVALID_FILE_PATH", exception.code());
    }

    @Test
    void refreshesFileHashesAndIndexesTerminalCreatedSourcesButDropsSecretsAndBuildOutputs() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-workspace-index-refresh-test");
        Path sourceDirectory = Files.createDirectories(root.resolve("src/main/java"));
        Path targetDirectory = Files.createDirectories(root.resolve("target/generated"));
        String changedJava = "class Main { int value = 2; }\n";
        String newPython = "print('created in terminal')\n";
        Files.writeString(sourceDirectory.resolve("Main.java"), changedJava);
        Files.writeString(root.resolve("main.py"), newPython);
        Files.writeString(root.resolve(".env"), "API_KEY=must-not-index");
        Files.writeString(targetDirectory.resolve("Generated.java"), "class Generated {}\n");
        Files.write(root.resolve("image.png"), new byte[]{0, 1, 2, 3});

        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        List<WorkspaceFile> previouslyIndexed = List.of(
                new WorkspaceFile(9L, "src/main/java/Main.java", "Main.java", "java", 4,
                        "stale-hash", true, Instant.now()),
                new WorkspaceFile(9L, "deleted.py", "deleted.py", "py", 0,
                        "old-hash", false, Instant.now()),
                new WorkspaceFile(9L, ".env", ".env", null, 20,
                        "secret-hash", false, Instant.now()),
                new WorkspaceFile(9L, "target/generated/Generated.java", "Generated.java", "java", 20,
                        "build-hash", false, Instant.now()));
        List<WorkspaceFile> refreshed = List.of(
                new WorkspaceFile(9L, "src/main/java/Main.java", "Main.java", "java",
                        changedJava.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                        WorkspaceService.sha256Text(changedJava), true, Instant.now()),
                new WorkspaceFile(9L, "main.py", "main.py", "py",
                        newPython.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                        WorkspaceService.sha256Text(newPython), false, Instant.now()));
        when(repository.findWorkspace(9L, 7L)).thenReturn(workspace);
        when(repository.listFiles(9L)).thenReturn(previouslyIndexed, refreshed);
        when(repository.nextWorkspaceFileSortOrder(9L)).thenReturn(12);

        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);
        List<WorkspaceFile> result = service.listFiles(student, 9L);

        assertEquals(refreshed, result);
        verify(repository).lockWorkspaceForMutation(9L);
        verify(repository).upsertIndexedFile(9L, "src/main/java/Main.java", "Main.java", "java",
                changedJava.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                WorkspaceService.sha256Text(changedJava), 0);
        verify(repository).upsertIndexedFile(9L, "main.py", "main.py", "py",
                newPython.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                WorkspaceService.sha256Text(newPython), 12);
        verify(repository).deleteIndexedFile(9L, "deleted.py");
        verify(repository).deleteIndexedFile(9L, ".env");
        verify(repository).deleteIndexedFile(9L, "target/generated/Generated.java");
        verify(repository, never()).upsertIndexedFile(eq(9L), eq("image.png"), any(), any(), anyLong(), any(), anyInt());
    }

    @Test
    void workspaceFileIndexRejectsProjectsThatExceedItsAggregateByteBudget() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-workspace-index-budget-test");
        Files.writeString(root.resolve("main.py"), "print('too much')\n");
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findWorkspace(9L, 7L)).thenReturn(workspace);
        when(repository.listFiles(9L)).thenReturn(List.of());
        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 4);

        ApiException exception = assertThrows(ApiException.class, () -> service.listFiles(student, 9L));

        assertEquals("WORKSPACE_INDEX_LIMIT", exception.code());
        verify(repository, never()).deleteIndexedFile(anyLong(), any());
        verify(repository, never()).upsertIndexedFile(anyLong(), any(), any(), any(), anyLong(), any(), anyInt());
    }

    @Test
    void nullByteAndEmptyNormalizedPathsAreRejected() {
        ApiException nullByte = assertThrows(ApiException.class,
                () -> WorkspaceService.normalizePath("src/Main.java" + (char) 0 + ".txt"));
        ApiException empty = assertThrows(ApiException.class,
                () -> WorkspaceService.normalizePath("./"));
        assertEquals("INVALID_FILE_PATH", nullByte.code());
        assertEquals("INVALID_FILE_PATH", empty.code());
    }

    @Test
    void fileApiCannotMutateReservedSnapshotStorage() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-workspace-reserved-path-test");
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findWorkspace(9L, 7L)).thenReturn(workspace);
        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.writeFile(student, 9L, ".snapshots/old/Main.java", new WriteFileRequest("changed", null)));

        assertEquals("INVALID_FILE_PATH", exception.code());
        assertTrue(Files.notExists(root.resolve(".snapshots/old/Main.java")));
    }

    @Test
    void retriesOnlyAWorkspaceThatFailedBeforeReceivingAContainerId() {
        Path root = Path.of(System.getProperty("java.io.tmpdir"), "bm-hs-workspace-retry-test");
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord failedWorkspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", null, "error", BigDecimal.ONE, 512);
        WorkspaceRecord provisioningWorkspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", null, "provisioning", BigDecimal.ONE, 512);
        WorkspaceRecord runningWorkspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        RunRecord failed = new RunRecord(3L, 2L, 7L, "not_started", null, failedWorkspace);
        RunRecord running = new RunRecord(3L, 2L, 7L, "not_started", null, runningWorkspace);
        when(repository.createOrFindRun(anyLong(), anyLong(), any())).thenReturn(failed);
        when(repository.retryProvisioning(3L, 7L)).thenReturn(new RunRecord(3L, 2L, 7L,
                "not_started", null, provisioningWorkspace));
        when(repository.claimWorkspaceProvisioning(9L)).thenReturn(true);
        when(runtime.start(provisioningWorkspace)).thenReturn("container");
        when(repository.findRun(3L, 7L)).thenReturn(running);

        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);
        service.createRun(student, 2L);

        verify(repository).retryProvisioning(3L, 7L);
        verify(runtime).start(provisioningWorkspace);
        verify(repository).updateRuntime(9L, "running", "container");
    }

    @Test
    void recordsProvisioningFailureAfterTheWorkspaceCreationTransactionHasCommitted() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-workspace-provision-failure-test");
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", null, "provisioning", BigDecimal.ONE, 512);
        RunRecord run = new RunRecord(3L, 2L, 7L, "not_started", null, workspace);
        when(repository.createOrFindRun(2L, 7L, new WorkspaceModels.WorkspaceConfig(root.toString(),
                "test-image", BigDecimal.ONE, 512, 100))).thenReturn(run);
        when(repository.claimWorkspaceProvisioning(9L)).thenReturn(true);
        when(repository.findExperimentName(2L)).thenReturn(null);
        org.mockito.Mockito.doThrow(new IllegalStateException("runtime unavailable"))
                .when(runtime).start(workspace);
        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);

        ApiException exception = assertThrows(ApiException.class, () -> service.createRun(student, 2L));

        assertEquals("WORKSPACE_PROVISION_FAILED", exception.code());
        verify(repository).updateRuntime(9L, "error", null);
    }

    @Test
    void removesStartedContainerIfItsRuntimeIdCannotBePersisted() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-workspace-runtime-id-test");
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", null, "provisioning", BigDecimal.ONE, 512);
        RunRecord run = new RunRecord(3L, 2L, 7L, "not_started", null, workspace);
        when(repository.createOrFindRun(2L, 7L, new WorkspaceModels.WorkspaceConfig(root.toString(),
                "test-image", BigDecimal.ONE, 512, 100))).thenReturn(run);
        when(repository.claimWorkspaceProvisioning(9L)).thenReturn(true);
        when(repository.findExperimentName(2L)).thenReturn(null);
        when(runtime.start(workspace)).thenReturn("0123456789abcdef0123456789abcdef");
        org.mockito.Mockito.doThrow(new IllegalStateException("database unavailable"))
                .when(repository).updateRuntime(9L, "running", "0123456789abcdef0123456789abcdef");
        WorkspaceService service = new WorkspaceService(repository, runtime, root.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);

        assertThrows(ApiException.class, () -> service.createRun(student, 2L));

        verify(runtime).stop("0123456789abcdef0123456789abcdef");
        verify(repository).updateRuntime(9L, "error", null);
    }

    @Test
    void createsSnapshotFromIndexedWorkspaceFiles() throws Exception {
        Path configuredRoot = Files.createTempDirectory("bm-hs-workspace-snapshot-test");
        Path root = configuredRoot.resolve("run-9");
        Files.createDirectories(root);
        Files.createDirectories(root.resolve("src"));
        Files.writeString(root.resolve("src/Main.java"), "class Main {}");
        Files.writeString(root.resolve(".env"), "API_TOKEN=do-not-copy");
        Files.writeString(root.resolve("application-secret.yaml"), "token: do-not-copy");
        Files.writeString(root.resolve("application.properties"),
                "spring.datasource.password=do-not-copy\nremote.url=https://student:do-not-copy@example.test\n");
        Files.writeString(root.resolve("README.md"),
                "password: do-not-copy\nservice: https://student:do-not-copy@example.test\n");
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findWorkspace(9L, 7L)).thenReturn(workspace);
        when(repository.listFiles(9L)).thenReturn(List.of(new WorkspaceFile(9L, "src/Main.java",
                "Main.java", "java", 13, "hash", true, Instant.now())));
        when(repository.createSnapshot(eq(9L), any(), eq("before node completion")))
                .thenReturn(new SnapshotView(11L, 9L, ".manual-snapshots/generated", "manual",
                        "before node completion", Instant.now()));

        WorkspaceService service = new WorkspaceService(repository, runtime, configuredRoot.toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);
        SnapshotView snapshot = service.createSnapshot(student, 9L,
                new WorkspaceModels.SnapshotRequest("before node completion"));

        assertEquals(".manual-snapshots/generated", snapshot.storageKey());
        Path snapshotParent = root.resolveSibling(root.getFileName() + ".manual-snapshots");
        try (var snapshots = Files.list(snapshotParent)) {
            Path snapshotDirectory = snapshots.findFirst().orElseThrow();
            assertEquals("class Main {}", Files.readString(snapshotDirectory.resolve("src/Main.java")));
            assertTrue(Files.notExists(snapshotDirectory.resolve(".env")));
            assertTrue(Files.notExists(snapshotDirectory.resolve("application-secret.yaml")));
            assertTrue(Files.notExists(snapshotDirectory.resolve("application.properties")));
            String redactedDocumentation = Files.readString(snapshotDirectory.resolve("README.md"));
            assertTrue(redactedDocumentation.contains("[REDACTED]"));
            assertFalse(redactedDocumentation.contains("do-not-copy"));
        }
        verify(repository).createSnapshot(eq(9L), any(), eq("before node completion"));
    }

    @Test
    void nodeCompletionCapturesWorkspaceCodeNotPresentInTheFileIndex() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-node-evidence-test");
        Files.createDirectories(root.resolve("src"));
        Files.writeString(root.resolve("src/Main.java"), "class Main {}\n");
        Files.createDirectories(root.resolve("target/classes"));
        Files.writeString(root.resolve("target/classes/Generated.java"), "should not be evidence");
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        RunRecord run = new RunRecord(3L, 2L, 7L, "in_progress", 31L, workspace);
        when(repository.findRun(3L, 7L)).thenReturn(run);
        RunProgressView before = new RunProgressView(3L, 2L, "in_progress", 31L,
                List.of(new WorkspaceModels.NodeProgressView(31L, "in_progress", BigDecimal.valueOf(90), null, null)), List.of());
        RunProgressView after = new RunProgressView(3L, 2L, "in_progress", null,
                List.of(new WorkspaceModels.NodeProgressView(31L, "completed", BigDecimal.valueOf(100), null, Instant.now())), List.of());
        when(repository.findProgress(3L, 7L)).thenReturn(before);
        when(repository.updateNodeProgress(3L, 7L, 31L, "completed", BigDecimal.valueOf(100))).thenReturn(after);
        when(repository.findNodeCompletionSnapshots(9L, 31L)).thenReturn(List.of());
        when(repository.createNodeCompletionSnapshot(eq(9L), eq(2L), eq(31L), any()))
                .thenAnswer(invocation -> new SnapshotView(11L, 9L, 31L, invocation.getArgument(3),
                        "node_completion", "evidence", Instant.now()));

        WorkspaceService service = new WorkspaceService(repository, runtime, root.getParent().toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);
        RunProgressView result = service.updateNode(student, 3L, 31L,
                new WorkspaceModels.UpdateNodeRequest("completed", BigDecimal.valueOf(100)));

        assertEquals(after, result);
        org.mockito.ArgumentCaptor<String> key = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository).createNodeCompletionSnapshot(eq(9L), eq(2L), eq(31L), key.capture());
        Path evidenceRoot = root.resolveSibling(root.getFileName() + ".node-evidence");
        String evidenceId = key.getValue().substring(".node-evidence/".length());
        assertEquals("class Main {}\n", Files.readString(evidenceRoot.resolve(evidenceId).resolve("src/Main.java")));
        assertTrue(Files.notExists(evidenceRoot.resolve(evidenceId).resolve("target/classes/Generated.java")));
    }

    @Test
    void rejectsNonUtf8SourceAsUnsupportedAndCleansIncompleteEvidence() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-node-evidence-encoding-test");
        Files.createDirectories(root.resolve("src"));
        Files.write(root.resolve("src/Main.java"), new byte[]{(byte) 0xc3, (byte) 0x28});
        WorkspaceRepository repository = mock(WorkspaceRepository.class);
        WorkspaceRuntime runtime = mock(WorkspaceRuntime.class);
        WorkspaceRecord workspace = new WorkspaceRecord(9L, 3L, 2L, 7L, root.toString(),
                "test-image", "container", "running", BigDecimal.ONE, 512);
        when(repository.findRun(3L, 7L)).thenReturn(new RunRecord(3L, 2L, 7L, "in_progress", 31L, workspace));
        RunProgressView before = new RunProgressView(3L, 2L, "in_progress", 31L,
                List.of(new WorkspaceModels.NodeProgressView(31L, "in_progress", BigDecimal.ZERO, null, null)), List.of());
        when(repository.findProgress(3L, 7L)).thenReturn(before);
        when(repository.updateNodeProgress(3L, 7L, 31L, "completed", BigDecimal.valueOf(100))).thenReturn(before);
        when(repository.findNodeCompletionSnapshots(9L, 31L)).thenReturn(List.of());
        WorkspaceService service = new WorkspaceService(repository, runtime, root.getParent().toString(),
                "test-image", BigDecimal.ONE, 512, 100, 1000);

        ApiException exception = assertThrows(ApiException.class, () -> service.updateNode(student, 3L, 31L,
                new WorkspaceModels.UpdateNodeRequest("completed", BigDecimal.valueOf(100))));

        assertEquals("SNAPSHOT_FILE_NOT_TEXT", exception.code());
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.status());
        Path evidenceRoot = root.resolveSibling(root.getFileName() + ".node-evidence");
        try (var evidence = Files.list(evidenceRoot)) {
            assertEquals(0, evidence.count());
        }
    }
}
