package com.bmhs.teacher;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeacherEvidenceServiceTest {
    private final AuthenticatedUser teacher = new AuthenticatedUser(3L, "teacher@example.com", "教师", "teacher");
    private static final String SNAPSHOT_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SNAPSHOT_KEY = ".node-evidence/" + SNAPSHOT_ID;

    @Test
    void listsAndReadsAuthorizedSnapshotAsPlainText() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-teacher-evidence");
        Path workspace = Files.createDirectories(root.resolve("run-3"));
        Path evidenceRoot = Files.createDirectories(workspace.resolveSibling(workspace.getFileName() + ".node-evidence"));
        Path snapshot = Files.createDirectories(evidenceRoot.resolve(SNAPSHOT_ID));
        Files.createDirectories(snapshot.resolve("src"));
        Files.writeString(snapshot.resolve("src/Main.java"), "class Main {}\n");
        TeacherRepository repository = mock(TeacherRepository.class);
        when(repository.assertSnapshotAccess(3L, 8L, 12L)).thenReturn(
                new TeacherRepository.SnapshotRecord(12L, 31L, workspace.toString(), SNAPSHOT_KEY));
        TeacherEvidenceService service = new TeacherEvidenceService(repository, root.toString(), 1024, 4096);

        List<TeacherModels.SnapshotFile> files = service.files(teacher, 8L, 12L);
        TeacherModels.SnapshotFileContent content = service.readFile(teacher, 8L, 12L, "src/Main.java");

        assertEquals(1, files.size());
        assertEquals("src/Main.java", files.get(0).path());
        assertEquals(true, files.get(0).text());
        assertEquals("class Main {}\n", content.content());
        verify(repository, times(2)).assertSnapshotAccess(3L, 8L, 12L);
    }

    @Test
    void rejectsTraversalBeforeReadingSnapshotFiles() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-teacher-evidence-path");
        Path workspace = Files.createDirectories(root.resolve("run-3"));
        Files.createDirectories(workspace.resolveSibling(workspace.getFileName() + ".node-evidence").resolve(SNAPSHOT_ID));
        TeacherRepository repository = mock(TeacherRepository.class);
        when(repository.assertSnapshotAccess(3L, 8L, 12L)).thenReturn(
                new TeacherRepository.SnapshotRecord(12L, 31L, workspace.toString(), SNAPSHOT_KEY));
        TeacherEvidenceService service = new TeacherEvidenceService(repository, root.toString(), 1024, 4096);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.readFile(teacher, 8L, 12L, "../outside.txt"));

        assertEquals("INVALID_SNAPSHOT_PATH", exception.code());
        verify(repository).assertSnapshotAccess(3L, 8L, 12L);
    }

    @Test
    void rejectsSnapshotFileOverTheConfiguredLimit() throws Exception {
        Path root = Files.createTempDirectory("bm-hs-teacher-evidence-limit");
        Path workspace = Files.createDirectories(root.resolve("run-3"));
        Path evidenceRoot = Files.createDirectories(workspace.resolveSibling(workspace.getFileName() + ".node-evidence"));
        Path snapshot = Files.createDirectories(evidenceRoot.resolve(SNAPSHOT_ID));
        Files.write(snapshot.resolve("large.txt"), new byte[1025]);
        TeacherRepository repository = mock(TeacherRepository.class);
        when(repository.assertSnapshotAccess(3L, 8L, 12L)).thenReturn(
                new TeacherRepository.SnapshotRecord(12L, 31L, workspace.toString(), SNAPSHOT_KEY));
        TeacherEvidenceService service = new TeacherEvidenceService(repository, root.toString(), 1024, 4096);

        ApiException exception = assertThrows(ApiException.class, () -> service.files(teacher, 8L, 12L));

        assertEquals("SNAPSHOT_TOO_LARGE", exception.code());
    }
}
