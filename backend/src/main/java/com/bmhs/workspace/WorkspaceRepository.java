package com.bmhs.workspace;

import com.bmhs.experimentcreation.ApiException;
import com.bmhs.workspace.WorkspaceModels.RunRecord;
import com.bmhs.workspace.WorkspaceModels.RunView;
import com.bmhs.workspace.WorkspaceModels.WorkspaceFile;
import com.bmhs.workspace.WorkspaceModels.WorkspaceRecord;
import com.bmhs.workspace.WorkspaceModels.NodeProgressView;
import com.bmhs.workspace.WorkspaceModels.RunProgressView;
import com.bmhs.workspace.WorkspaceModels.TaskProgressView;
import com.bmhs.workspace.WorkspaceModels.SnapshotView;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class WorkspaceRepository {
    private final JdbcTemplate jdbc;

    public WorkspaceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public RunRecord createOrFindRun(long experimentId, long studentId, WorkspaceModels.WorkspaceConfig config) {
        if (!hasRunnableAccess(experimentId, studentId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EXPERIMENT_FORBIDDEN", "当前学生不能运行该实验");
        }
        List<RunRecord> existing = findRunByExperimentStudent(experimentId, studentId);
        if (!existing.isEmpty()) {
            ensureTaskProgress(existing.get(0).id(), experimentId);
            return existing.get(0);
        }
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO experiment_runs (experiment_id, student_id, status, last_opened_at)
                    VALUES (?, ?, 'not_started', CURRENT_TIMESTAMP(6))
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, experimentId);
            statement.setLong(2, studentId);
            return statement;
        }, keys);
        long runId = requiredKey(keys);
        jdbc.update("""
                INSERT INTO node_progress (experiment_id, run_id, node_id, status, progress_percent)
                SELECT experiment_id, ?, id,
                       CASE WHEN parent_node_id IS NULL THEN 'pending' ELSE 'locked' END, 0
                FROM experiment_nodes WHERE experiment_id = ?
                """, runId, experimentId);
        ensureTaskProgress(runId, experimentId);
        String storagePath = Path.of(config.rootPath()).resolve("run-" + runId).normalize().toString();
        jdbc.update("""
                INSERT INTO coding_workspaces
                  (run_id, storage_path, runtime_image, status, cpu_limit, memory_limit_mb)
                VALUES (?, ?, ?, 'provisioning', ?, ?)
                """, runId, storagePath, config.runtimeImage(), config.cpuLimit(), config.memoryLimitMb());
        return findRun(runId, studentId);
    }

    public RunRecord findRun(long runId, long studentId) {
        List<RunRecord> records = jdbc.query("""
                SELECT r.id AS run_id, r.experiment_id, r.student_id, r.status AS run_status,
                       r.current_node_id, w.id AS workspace_id, w.storage_path, w.runtime_image, w.runtime_instance_id,
                       w.status AS workspace_status, w.cpu_limit, w.memory_limit_mb
                FROM experiment_runs r JOIN coding_workspaces w ON w.run_id = r.id
                WHERE r.id = ? AND r.student_id = ?
                """, (rs, rowNum) -> toRunRecord(rs.getLong("run_id"), rs.getLong("experiment_id"),
                rs.getLong("student_id"), rs.getString("run_status"), rs.getObject("current_node_id", Long.class),
                new WorkspaceRecord(rs.getLong("workspace_id"), rs.getLong("run_id"), rs.getLong("experiment_id"),
                        rs.getLong("student_id"), rs.getString("storage_path"), rs.getString("runtime_image"),
                        rs.getString("runtime_instance_id"), rs.getString("workspace_status"),
                        rs.getBigDecimal("cpu_limit"), rs.getInt("memory_limit_mb"))),
                runId, studentId);
        if (records.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "运行实例不存在");
        return records.get(0);
    }

    public WorkspaceRecord findWorkspace(long workspaceId, long studentId) {
        List<WorkspaceRecord> records = jdbc.query("""
                SELECT w.id, w.run_id, r.experiment_id, r.student_id, w.storage_path, w.runtime_image,
                       w.runtime_instance_id, w.status, w.cpu_limit, w.memory_limit_mb
                FROM coding_workspaces w JOIN experiment_runs r ON r.id = w.run_id
                WHERE w.id = ? AND r.student_id = ?
                """, (rs, rowNum) -> new WorkspaceRecord(rs.getLong("id"), rs.getLong("run_id"),
                rs.getLong("experiment_id"), rs.getLong("student_id"), rs.getString("storage_path"),
                rs.getString("runtime_image"), rs.getString("runtime_instance_id"), rs.getString("status"),
                rs.getBigDecimal("cpu_limit"),
                rs.getInt("memory_limit_mb")), workspaceId, studentId);
        if (records.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "WORKSPACE_NOT_FOUND", "工作区不存在");
        return records.get(0);
    }

    public String findExperimentName(long experimentId) {
        List<String> names = jdbc.query("SELECT name FROM experiments WHERE id = ?",
                (rs, rowNum) -> rs.getString("name"), experimentId);
        if (names.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "EXPERIMENT_NOT_FOUND", "实验不存在");
        return names.get(0);
    }

    public void updateRuntime(long workspaceId, String status, String runtimeInstanceId) {
        jdbc.update("UPDATE coding_workspaces SET status = ?, runtime_instance_id = ?, last_active_at = CURRENT_TIMESTAMP(6) "
                + "WHERE id = ?", status, runtimeInstanceId, workspaceId);
    }

    public boolean claimWorkspaceProvisioning(long workspaceId) {
        return jdbc.update("UPDATE coding_workspaces SET status = 'starting', last_active_at = CURRENT_TIMESTAMP(6) "
                + "WHERE id = ? AND status = 'provisioning' AND runtime_instance_id IS NULL", workspaceId) == 1;
    }

    public RunRecord retryProvisioning(long runId, long studentId) {
        int changed = jdbc.update("""
                UPDATE coding_workspaces w
                JOIN experiment_runs r ON r.id = w.run_id
                SET w.status = 'provisioning', w.runtime_instance_id = NULL, w.last_active_at = NULL
                WHERE r.id = ? AND r.student_id = ? AND w.status = 'error' AND w.runtime_instance_id IS NULL
                """, runId, studentId);
        if (changed == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "WORKSPACE_RETRY_UNAVAILABLE",
                    "工作区当前不可安全重试，请先清理旧运行实例");
        }
        return findRun(runId, studentId);
    }

    @Transactional
    public RunProgressView findProgress(long runId, long studentId) {
        RunRecord run = findRun(runId, studentId);
        ensureTaskProgress(runId, run.experimentId());
        List<NodeProgressView> nodes = jdbc.query("""
                SELECT node_id, status, progress_percent, started_at, completed_at
                FROM node_progress
                WHERE run_id = ? AND experiment_id = ?
                ORDER BY id
                """, (rs, rowNum) -> new NodeProgressView(rs.getLong("node_id"), rs.getString("status"),
                rs.getBigDecimal("progress_percent"), nullableInstant(rs.getTimestamp("started_at")),
                nullableInstant(rs.getTimestamp("completed_at"))), runId, run.experimentId());
        List<TaskProgressView> tasks = jdbc.query("""
                SELECT tp.task_id, nt.node_id, tp.status, tp.completed_at
                FROM task_progress tp
                JOIN node_tasks nt ON nt.id = tp.task_id AND nt.experiment_id = tp.experiment_id
                WHERE tp.run_id = ? AND tp.experiment_id = ?
                ORDER BY nt.sort_order, tp.task_id
                """, (rs, rowNum) -> new TaskProgressView(rs.getLong("task_id"), rs.getLong("node_id"),
                rs.getString("status"), nullableInstant(rs.getTimestamp("completed_at"))), runId, run.experimentId());
        return new RunProgressView(run.id(), run.experimentId(), run.status(), run.currentNodeId(), nodes, tasks);
    }

    @Transactional
    public RunProgressView updateNodeProgress(long runId, long studentId, long nodeId,
                                              String status, BigDecimal progressPercent) {
        RunRecord run = findRun(runId, studentId);
        ensureTaskProgress(runId, run.experimentId());
        List<NodeProgressView> current = jdbc.query("""
                SELECT node_id, status, progress_percent, started_at, completed_at
                FROM node_progress
                WHERE run_id = ? AND experiment_id = ? AND node_id = ?
                FOR UPDATE
                """, (rs, rowNum) -> new NodeProgressView(rs.getLong("node_id"), rs.getString("status"),
                rs.getBigDecimal("progress_percent"), nullableInstant(rs.getTimestamp("started_at")),
                nullableInstant(rs.getTimestamp("completed_at"))), runId, run.experimentId(), nodeId);
        if (current.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "NODE_PROGRESS_NOT_FOUND", "节点进度不存在");
        NodeProgressView before = current.get(0);
        if ("locked".equals(before.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "NODE_LOCKED", "当前节点尚未解锁");
        }
        if ("completed".equals(before.status())) {
            if ("completed".equals(status) && progressPercent.compareTo(BigDecimal.valueOf(100)) == 0) {
                return findProgress(runId, studentId);
            }
            throw new ApiException(HttpStatus.CONFLICT, "NODE_ALREADY_COMPLETED", "节点已经完成");
        }
        if ("completed".equals(status) && progressPercent.compareTo(BigDecimal.valueOf(100)) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NODE_COMPLETION_REQUIRES_100", "完成节点时进度必须为 100");
        }
        List<String> taskStatuses = jdbc.query("""
                    SELECT COALESCE(tp.status, 'missing')
                    FROM node_tasks nt
                    LEFT JOIN task_progress tp ON tp.task_id = nt.id AND tp.experiment_id = nt.experiment_id
                                             AND tp.run_id = ?
                    WHERE nt.experiment_id = ? AND nt.node_id = ?
                    FOR UPDATE
                    """, (rs, rowNum) -> rs.getString(1), runId, run.experimentId(), nodeId);
        if ("completed".equals(status) && taskStatuses.stream().anyMatch(taskStatus -> !"completed".equals(taskStatus))) {
                throw new ApiException(HttpStatus.CONFLICT, "NODE_TASKS_INCOMPLETE", "请先完成当前节点的所有任务");
        }
        BigDecimal effectiveProgressPercent = progressPercent;
        if ("in_progress".equals(status) && !taskStatuses.isEmpty()) {
            long completedTasks = taskStatuses.stream().filter("completed"::equals).count();
            effectiveProgressPercent = BigDecimal.valueOf(completedTasks).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(taskStatuses.size()), 2, RoundingMode.HALF_UP);
        }
        if ("in_progress".equals(status)) {
            jdbc.update("""
                    UPDATE node_progress
                    SET status = 'in_progress', progress_percent = ?, started_at = COALESCE(started_at, CURRENT_TIMESTAMP(6)),
                        completed_at = NULL
                    WHERE run_id = ? AND experiment_id = ? AND node_id = ?
                    """, effectiveProgressPercent, runId, run.experimentId(), nodeId);
            jdbc.update("""
                    UPDATE experiment_runs
                    SET status = 'in_progress', current_node_id = ?, started_at = COALESCE(started_at, CURRENT_TIMESTAMP(6)),
                        last_opened_at = CURRENT_TIMESTAMP(6)
                    WHERE id = ? AND student_id = ?
                    """, nodeId, runId, studentId);
        } else {
            jdbc.update("""
                    UPDATE node_progress
                    SET status = 'completed', progress_percent = 100,
                        started_at = COALESCE(started_at, CURRENT_TIMESTAMP(6)), completed_at = CURRENT_TIMESTAMP(6)
                    WHERE run_id = ? AND experiment_id = ? AND node_id = ?
                    """, runId, run.experimentId(), nodeId);
            jdbc.update("""
                    UPDATE node_progress child
                    JOIN experiment_nodes node ON node.id = child.node_id AND node.experiment_id = child.experiment_id
                    SET child.status = 'pending'
                    WHERE child.run_id = ? AND child.experiment_id = ? AND node.parent_node_id = ? AND child.status = 'locked'
                    """, runId, run.experimentId(), nodeId);
            Integer remaining = jdbc.query("""
                    SELECT COUNT(*) FROM node_progress
                    WHERE run_id = ? AND experiment_id = ? AND status <> 'completed'
                    """, rs -> rs.next() ? rs.getInt(1) : 0, runId, run.experimentId());
            if (remaining == 0) {
                jdbc.update("""
                        UPDATE experiment_runs
                        SET status = 'completed', current_node_id = NULL,
                            started_at = COALESCE(started_at, CURRENT_TIMESTAMP(6)),
                            completed_at = CURRENT_TIMESTAMP(6), last_opened_at = CURRENT_TIMESTAMP(6)
                        WHERE id = ? AND student_id = ?
                        """, runId, studentId);
            } else {
                jdbc.update("""
                        UPDATE experiment_runs
                        SET status = 'in_progress', current_node_id = NULL,
                            started_at = COALESCE(started_at, CURRENT_TIMESTAMP(6)), last_opened_at = CURRENT_TIMESTAMP(6)
                        WHERE id = ? AND student_id = ?
                        """, runId, studentId);
            }
        }
        return findProgress(runId, studentId);
    }

    @Transactional
    public RunProgressView updateTaskProgress(long runId, long studentId, long taskId, String status) {
        RunRecord run = findRun(runId, studentId);
        ensureTaskProgress(runId, run.experimentId());
        List<NodeProgressView> nodeRows = jdbc.query("""
                SELECT np.node_id, np.status, np.progress_percent, np.started_at, np.completed_at
                FROM node_tasks nt
                JOIN node_progress np ON np.node_id = nt.node_id AND np.experiment_id = nt.experiment_id
                                    AND np.run_id = ?
                WHERE nt.id = ? AND nt.experiment_id = ?
                FOR UPDATE
                """, (rs, rowNum) -> new NodeProgressView(rs.getLong("node_id"), rs.getString("status"),
                rs.getBigDecimal("progress_percent"), nullableInstant(rs.getTimestamp("started_at")),
                nullableInstant(rs.getTimestamp("completed_at"))), runId, taskId, run.experimentId());
        if (nodeRows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TASK_PROGRESS_NOT_FOUND", "任务不存在或当前运行实例无权访问");
        }
        NodeProgressView node = nodeRows.get(0);
        if ("locked".equals(node.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "NODE_LOCKED", "当前节点尚未解锁");
        }
        if ("completed".equals(node.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "NODE_ALREADY_COMPLETED", "节点已经完成，任务进度不可修改");
        }
        jdbc.update("""
                UPDATE task_progress
                SET status = ?, completed_at = CASE WHEN ? = 'completed' THEN CURRENT_TIMESTAMP(6) ELSE NULL END
                WHERE run_id = ? AND experiment_id = ? AND task_id = ?
                """, status, status, runId, run.experimentId(), taskId);
        Integer totalTasks = jdbc.queryForObject("""
                SELECT COUNT(*) FROM node_tasks WHERE experiment_id = ? AND node_id = ?
                """, Integer.class, run.experimentId(), node.nodeId());
        Integer completedTasks = jdbc.queryForObject("""
                SELECT COUNT(*) FROM task_progress tp
                JOIN node_tasks nt ON nt.id = tp.task_id AND nt.experiment_id = tp.experiment_id
                WHERE tp.run_id = ? AND tp.experiment_id = ? AND nt.node_id = ? AND tp.status = 'completed'
                """, Integer.class, runId, run.experimentId(), node.nodeId());
        int total = totalTasks == null ? 0 : totalTasks;
        int completed = completedTasks == null ? 0 : completedTasks;
        BigDecimal percent = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        jdbc.update("""
                UPDATE node_progress
                SET status = CASE WHEN status = 'pending' THEN 'in_progress' ELSE status END,
                    progress_percent = ?, started_at = COALESCE(started_at, CURRENT_TIMESTAMP(6)), completed_at = NULL
                WHERE run_id = ? AND experiment_id = ? AND node_id = ?
                """, percent, runId, run.experimentId(), node.nodeId());
        jdbc.update("""
                UPDATE experiment_runs
                SET status = 'in_progress', current_node_id = ?,
                    started_at = COALESCE(started_at, CURRENT_TIMESTAMP(6)), last_opened_at = CURRENT_TIMESTAMP(6)
                WHERE id = ? AND student_id = ?
                """, node.nodeId(), runId, studentId);
        return findProgress(runId, studentId);
    }

    private void ensureTaskProgress(long runId, long experimentId) {
        jdbc.update("""
                INSERT IGNORE INTO task_progress (experiment_id, run_id, task_id, status, completed_at)
                SELECT nt.experiment_id, np.run_id, nt.id,
                       CASE WHEN np.status = 'completed' THEN 'completed' ELSE 'pending' END,
                       CASE WHEN np.status = 'completed' THEN np.completed_at ELSE NULL END
                FROM node_tasks nt
                JOIN node_progress np ON np.node_id = nt.node_id AND np.experiment_id = nt.experiment_id
                                     AND np.run_id = ?
                WHERE nt.experiment_id = ?
                """, runId, experimentId);
    }

    public SnapshotView createSnapshot(long workspaceId, String storageKey, String description) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO workspace_snapshots (workspace_id, node_id, storage_key, snapshot_type, description)
                    VALUES (?, NULL, ?, 'manual', ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, workspaceId);
            statement.setString(2, storageKey);
            statement.setString(3, description);
            return statement;
        }, keys);
        long id = requiredKey(keys);
        return jdbc.queryForObject("""
                SELECT id, workspace_id, node_id, storage_key, snapshot_type, description, created_at
                FROM workspace_snapshots WHERE id = ?
                """, (rs, rowNum) -> new SnapshotView(rs.getLong("id"), rs.getLong("workspace_id"),
                rs.getObject("node_id", Long.class),
                rs.getString("storage_key"), rs.getString("snapshot_type"), rs.getString("description"),
                rs.getTimestamp("created_at").toInstant()), id);
    }

    @Transactional
    public SnapshotView createNodeCompletionSnapshot(long workspaceId, long experimentId, long nodeId,
                                                       String storageKey) {
        List<SnapshotView> existing = findNodeCompletionSnapshots(workspaceId, nodeId);
        if (!existing.isEmpty()) return existing.get(0);
        Integer belongsToExperiment = jdbc.queryForObject("""
                SELECT COUNT(*) FROM experiment_nodes WHERE id = ? AND experiment_id = ?
                """, Integer.class, nodeId, experimentId);
        if (belongsToExperiment == null || belongsToExperiment != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "SNAPSHOT_NODE_MISMATCH", "阶段快照节点与实验不匹配");
        }
        jdbc.update("""
                INSERT INTO workspace_snapshots (workspace_id, node_id, storage_key, snapshot_type, description)
                VALUES (?, ?, ?, 'node_completion', '节点完成时的只读代码证据')
                ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)
                """, workspaceId, nodeId, storageKey);
        return findNodeCompletionSnapshots(workspaceId, nodeId).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("节点代码证据未能持久化"));
    }

    public List<SnapshotView> findNodeCompletionSnapshots(long workspaceId, long nodeId) {
        return jdbc.query("""
                SELECT id, workspace_id, node_id, storage_key, snapshot_type, description, created_at
                FROM workspace_snapshots
                WHERE workspace_id = ? AND node_id = ? AND snapshot_type = 'node_completion'
                ORDER BY created_at ASC LIMIT 1 FOR UPDATE
                """, (rs, rowNum) -> new SnapshotView(rs.getLong("id"), rs.getLong("workspace_id"),
                rs.getObject("node_id", Long.class), rs.getString("storage_key"), rs.getString("snapshot_type"),
                rs.getString("description"), rs.getTimestamp("created_at").toInstant()), workspaceId, nodeId);
    }

    @Transactional
    public void lockWorkspaceForMutation(long workspaceId) {
        List<Long> rows = jdbc.query("""
                SELECT id FROM coding_workspaces WHERE id = ? FOR UPDATE
                """, (rs, rowNum) -> rs.getLong("id"), workspaceId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "WORKSPACE_NOT_FOUND", "工作区不存在");
    }

    public List<WorkspaceFile> listFiles(long workspaceId) {
        return jdbc.query("""
                SELECT workspace_id, relative_path, display_name, language, size_bytes, content_hash, is_entry, updated_at
                FROM workspace_files WHERE workspace_id = ? ORDER BY sort_order, relative_path
                """, (rs, rowNum) -> new WorkspaceFile(rs.getLong("workspace_id"), rs.getString("relative_path"),
                rs.getString("display_name"), rs.getString("language"), rs.getLong("size_bytes"),
                rs.getString("content_hash"), rs.getBoolean("is_entry"), rs.getTimestamp("updated_at").toInstant()), workspaceId);
    }

    public int nextWorkspaceFileSortOrder(long workspaceId) {
        Integer nextOrder = jdbc.queryForObject("""
                SELECT COALESCE(MAX(sort_order) + 1, 0) FROM workspace_files WHERE workspace_id = ?
                """, Integer.class, workspaceId);
        return nextOrder == null ? 0 : nextOrder;
    }

    public void upsertIndexedFile(long workspaceId, String relativePath, String displayName, String language,
                                  long sizeBytes, String contentHash, int newSortOrder) {
        jdbc.update("""
                INSERT INTO workspace_files
                  (workspace_id, relative_path, display_name, language, size_bytes, content_hash, is_entry, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, FALSE, ?)
                ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), language = VALUES(language),
                  size_bytes = VALUES(size_bytes), content_hash = VALUES(content_hash)
                """, workspaceId, relativePath, displayName, language, sizeBytes, contentHash, newSortOrder);
    }

    public void deleteIndexedFile(long workspaceId, String relativePath) {
        jdbc.update("DELETE FROM workspace_files WHERE workspace_id = ? AND relative_path = ?",
                workspaceId, relativePath);
    }

    public WorkspaceFile saveFile(long workspaceId, String relativePath, String displayName, String language,
                                  long sizeBytes, String contentHash, boolean entry) {
        return saveFile(workspaceId, relativePath, displayName, language, sizeBytes, contentHash, entry, 0);
    }

    public WorkspaceFile saveFile(long workspaceId, String relativePath, String displayName, String language,
                                  long sizeBytes, String contentHash, boolean entry, int sortOrder) {
        jdbc.update("""
            INSERT INTO workspace_files
              (workspace_id, relative_path, display_name, language, size_bytes, content_hash, is_entry, sort_order)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), language = VALUES(language),
              size_bytes = VALUES(size_bytes), content_hash = VALUES(content_hash), is_entry = VALUES(is_entry),
              sort_order = VALUES(sort_order)
            """, workspaceId, relativePath, displayName, language, sizeBytes, contentHash, entry, sortOrder);
        return jdbc.queryForObject("""
                SELECT workspace_id, relative_path, display_name, language, size_bytes, content_hash, is_entry, updated_at
                FROM workspace_files WHERE workspace_id = ? AND relative_path = ?
                """, (rs, rowNum) -> new WorkspaceFile(rs.getLong("workspace_id"), rs.getString("relative_path"),
                rs.getString("display_name"), rs.getString("language"), rs.getLong("size_bytes"),
                rs.getString("content_hash"), rs.getBoolean("is_entry"), rs.getTimestamp("updated_at").toInstant()),
                workspaceId, relativePath);
    }

    public WorkspaceFile findFile(long workspaceId, String relativePath) {
        List<WorkspaceFile> files = jdbc.query("""
                SELECT workspace_id, relative_path, display_name, language, size_bytes, content_hash, is_entry, updated_at
                FROM workspace_files WHERE workspace_id = ? AND relative_path = ?
                """, (rs, rowNum) -> new WorkspaceFile(rs.getLong("workspace_id"), rs.getString("relative_path"),
                rs.getString("display_name"), rs.getString("language"), rs.getLong("size_bytes"),
                rs.getString("content_hash"), rs.getBoolean("is_entry"), rs.getTimestamp("updated_at").toInstant()),
                workspaceId, relativePath);
        return files.isEmpty() ? null : files.get(0);
    }

    public RunView toView(RunRecord run) {
        return new RunView(run.id(), run.experimentId(), run.status(), run.currentNodeId(),
                run.workspace().id(), run.workspace().status());
    }

    boolean hasRunnableAccess(long experimentId, long studentId) {
        List<Integer> ownedDraft = jdbc.query("""
                SELECT 1 FROM experiments
                WHERE id = ? AND creator_id = ? AND status = 'draft'
                LIMIT 1
                """, (rs, rowNum) -> 1, experimentId, studentId);
        if (!ownedDraft.isEmpty()) return true;

        List<Integer> publishedCourseAccess = jdbc.query("""
                SELECT 1 FROM course_experiment_assignments a
                JOIN course_members m ON m.course_id = a.course_id
                JOIN experiments e ON e.id = a.experiment_id
                WHERE a.experiment_id = ? AND a.status = 'published' AND e.status = 'published'
                  AND m.student_id = ? AND m.status = 'active' LIMIT 1
                """, (rs, rowNum) -> 1, experimentId, studentId);
        return !publishedCourseAccess.isEmpty();
    }

    private List<RunRecord> findRunByExperimentStudent(long experimentId, long studentId) {
        return jdbc.query("SELECT id FROM experiment_runs WHERE experiment_id = ? AND student_id = ?",
                (rs, rowNum) -> findRun(rs.getLong("id"), studentId), experimentId, studentId);
    }

    private RunRecord toRunRecord(long runId, long experimentId, long studentId, String status,
                                  Long currentNodeId, WorkspaceRecord workspace) {
        return new RunRecord(runId, experimentId, studentId, status, currentNodeId, workspace);
    }

    private long requiredKey(KeyHolder keys) {
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("数据库未返回运行实例主键");
        return key.longValue();
    }

    private Instant nullableInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
