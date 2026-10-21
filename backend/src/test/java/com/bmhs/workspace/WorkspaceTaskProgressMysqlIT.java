package com.bmhs.workspace;

import com.bmhs.experimentcreation.ApiException;
import com.bmhs.teacher.TeacherModels;
import com.bmhs.teacher.TeacherRepository;
import com.bmhs.workspace.WorkspaceModels.NodeProgressView;
import com.bmhs.workspace.WorkspaceModels.RunProgressView;
import com.bmhs.workspace.WorkspaceModels.TaskProgressView;
import org.springframework.dao.DataAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in integration coverage against a disposable MySQL 8 database on 127.0.0.1:13316.
 * See the required JVM property and environment marker in {@link #connectToOptInDatabase()}.
 */
class WorkspaceTaskProgressMysqlIT {
    private static final String OPT_IN_PROPERTY = "bmhs.task-progress.mysql-it";
    private static final String JDBC_URL_PROPERTY = "bmhs.task-progress.mysql-url";
    private static final String JDBC_USER_ENV = "BMHS_IT_MYSQL_USER";
    private static final String JDBC_PASSWORD_ENV = "BMHS_IT_MYSQL_PASSWORD";
    private static final String DISPOSABLE_TARGET_ENV = "BMHS_DISPOSABLE_MYSQL_TARGET";
    private static final String EXPECTED_SERVER_UUID_ENV = "BMHS_IT_MYSQL_SERVER_UUID";
    private static final String REQUIRED_TARGET_MARKER = "BM-sql@127.0.0.1:13316";
    private static final String DATABASE_MARKER_TABLE = "bmhs_disposable_test_marker";
    private static final String REQUIRED_DATABASE_MARKER = "BMHS_TASK_PROGRESS_IT_V1";
    private static final String SCHEMA = "BM-sql";
    private static final int REQUIRED_PORT = 13316;

    private static JdbcTemplate jdbc;
    private static TransactionTemplate transactions;
    private static WorkspaceRepository workspaceRepository;
    private static TeacherRepository teacherRepository;

    private Fixture fixture;

    @BeforeAll
    static void connectToOptInDatabase() {
        if (!"true".equalsIgnoreCase(System.getProperty(OPT_IN_PROPERTY))) {
            throw new IllegalStateException("Explicit MySQL IT selection requires -D" + OPT_IN_PROPERTY + "=true");
        }
        if (!REQUIRED_TARGET_MARKER.equals(System.getenv(DISPOSABLE_TARGET_ENV))) {
            throw new IllegalStateException("Explicit MySQL IT selection requires "
                    + DISPOSABLE_TARGET_ENV + "=" + REQUIRED_TARGET_MARKER);
        }
        String expectedServerUuid = System.getenv(EXPECTED_SERVER_UUID_ENV);
        if (expectedServerUuid == null || !expectedServerUuid.matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new IllegalStateException("Explicit MySQL IT selection requires " + EXPECTED_SERVER_UUID_ENV
                    + " to be a UUID copied from the disposable server's @@server_uuid");
        }

        String url = System.getProperty(JDBC_URL_PROPERTY, "");
        URI target;
        try {
            if (!url.startsWith("jdbc:mysql://")) {
                throw new IllegalArgumentException("JDBC URL must use jdbc:mysql://");
            }
            target = URI.create(url.substring("jdbc:".length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid opt-in MySQL JDBC URL", exception);
        }
        String host = target.getHost();
        String database = target.getPath() == null ? "" : target.getPath().substring(1);
        if (target.getUserInfo() != null || host == null || !isLoopbackHost(host)
                || target.getPort() != REQUIRED_PORT || !SCHEMA.equals(database)
                || target.getQuery() != null || target.getFragment() != null
                || url.indexOf(',') >= 0 || url.indexOf(';') >= 0) {
            throw new IllegalStateException("Refusing MySQL target; expected loopback port " + REQUIRED_PORT
                    + " and schema " + SCHEMA + " with no multi-host URL");
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        String username = System.getenv(JDBC_USER_ENV);
        String password = System.getenv(JDBC_PASSWORD_ENV);
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("Set the disposable MySQL credentials in "
                    + JDBC_USER_ENV + " and " + JDBC_PASSWORD_ENV);
        }
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        DataSource verifiedDataSource = new VerifiedMySqlDataSource(dataSource, expectedServerUuid);
        verifyServerTarget(verifiedDataSource);

        jdbc = new JdbcTemplate(verifiedDataSource);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(verifiedDataSource));
        workspaceRepository = new WorkspaceRepository(jdbc);
        teacherRepository = new TeacherRepository(jdbc);
    }

    @BeforeEach
    void createFixture() {
        fixture = new Fixture("it_" + UUID.randomUUID().toString().replace("-", ""));
        inTransaction(() -> {
            fixture.teacherId = insertId("INSERT INTO users (email, display_name, role) VALUES (?, ?, 'teacher')",
                    fixture.prefix + "_teacher@example.test", "IT teacher");
            fixture.studentId = insertId("INSERT INTO users (email, display_name, role) VALUES (?, ?, 'student')",
                    fixture.prefix + "_student@example.test", "IT student");
            fixture.otherStudentId = insertId("INSERT INTO users (email, display_name, role) VALUES (?, ?, 'student')",
                    fixture.prefix + "_other@example.test", "IT other student");
            fixture.courseId = insertId("INSERT INTO courses (teacher_id, name, invite_code) VALUES (?, ?, ?)",
                    fixture.teacherId, fixture.prefix, fixture.prefix.substring(fixture.prefix.length() - 8));
            fixture.experimentId = insertId("""
                    INSERT INTO experiments
                      (creator_id, name, learning_goal, difficulty, duration_days, duration_minutes,
                       status, visibility, published_at)
                    VALUES (?, ?, 'Integration behavior fixture', 'beginner', 1, 30,
                            'published', 'private', CURRENT_TIMESTAMP(6))
                    """, fixture.teacherId, fixture.prefix);
            jdbc.update("INSERT INTO course_members (course_id, student_id, status) VALUES (?, ?, 'active')",
                    fixture.courseId, fixture.studentId);
            jdbc.update("INSERT INTO course_members (course_id, student_id, status) VALUES (?, ?, 'active')",
                    fixture.courseId, fixture.otherStudentId);
            fixture.assignmentId = insertId("""
                    INSERT INTO course_experiment_assignments (course_id, experiment_id, publisher_id, status)
                    VALUES (?, ?, ?, 'published')
                    """, fixture.courseId, fixture.experimentId, fixture.teacherId);

            fixture.rootNodeId = insertNode("root", "Root with tasks", null, 1);
            fixture.childNodeId = insertNode("child", "Locked child", fixture.rootNodeId, 2);
            fixture.tasklessNodeId = insertNode("manual", "Taskless manual node", null, 3);
            fixture.firstTaskId = insertTask(fixture.rootNodeId, "First root task", 1);
            fixture.secondTaskId = insertTask(fixture.rootNodeId, "Second root task", 2);
            fixture.childTaskId = insertTask(fixture.childNodeId, "Child task", 1);

            fixture.runId = insertRun(fixture.studentId, "primary");
            fixture.otherRunId = insertRun(fixture.otherStudentId, "other");
            createProgress(fixture.runId);
            createProgress(fixture.otherRunId);
            return null;
        });
    }

    @AfterEach
    void removeFixture() {
        if (fixture == null) return;
        RuntimeException cleanupFailure = null;
        try {
            jdbc.execute("DROP TRIGGER IF EXISTS `" + SCHEMA + "`.`" + fixture.prefix + "_fail_run_update`");
        } catch (RuntimeException exception) {
            cleanupFailure = exception;
        }
        try {
            inTransaction(() -> {
                for (long runId : fixture.runIds) {
                    jdbc.update("DELETE FROM experiment_runs WHERE id = ?", runId);
                }
                if (fixture.assignmentId != null) {
                    jdbc.update("DELETE FROM course_experiment_assignments WHERE id = ?", fixture.assignmentId);
                }
                if (fixture.courseId != null && fixture.studentId != null && fixture.otherStudentId != null) {
                    jdbc.update("DELETE FROM course_members WHERE course_id = ? AND student_id IN (?, ?)",
                            fixture.courseId, fixture.studentId, fixture.otherStudentId);
                }
                if (fixture.experimentId != null) {
                    jdbc.update("DELETE FROM experiments WHERE id = ?", fixture.experimentId);
                }
                if (fixture.courseId != null) {
                    jdbc.update("DELETE FROM courses WHERE id = ?", fixture.courseId);
                }
                for (long userId : fixture.userIds) {
                    jdbc.update("DELETE FROM users WHERE id = ?", userId);
                }
                return null;
            });
        } catch (RuntimeException exception) {
            if (cleanupFailure == null) cleanupFailure = exception;
            else cleanupFailure.addSuppressed(exception);
        }
        if (cleanupFailure != null) {
            throw new IllegalStateException("Failed to clean disposable MySQL fixture " + fixture.prefix
                    + "; inspect this prefix before reusing the disposable target", cleanupFailure);
        }
    }

    @Test
    void taskCompletionPersistsAcrossSeparateTransactionsAndReads() {
        RunProgressView updated = inTransaction(() -> workspaceRepository.updateTaskProgress(
                fixture.runId, fixture.studentId, fixture.firstTaskId, "completed"));
        assertEquals("completed", task(updated, fixture.firstTaskId).status());
        assertEquals("in_progress", node(updated, fixture.rootNodeId).status());
        assertEquals(50, node(updated, fixture.rootNodeId).progressPercent().intValue());

        RunProgressView reread = inTransaction(() -> workspaceRepository.findProgress(fixture.runId, fixture.studentId));
        assertEquals("completed", task(reread, fixture.firstTaskId).status());
        assertNotNull(task(reread, fixture.firstTaskId).completedAt());
        assertEquals("pending", task(reread, fixture.secondTaskId).status());
        assertEquals("in_progress", node(reread, fixture.rootNodeId).status());
        assertEquals(1, scalar("SELECT COUNT(*) FROM task_progress WHERE run_id = ? AND task_id = ? AND status = 'completed'",
                fixture.runId, fixture.firstTaskId));
    }

    @Test
    void incompleteAndLockedTasksRejectWritesWithoutChangingNodeRunOrChild() {
        ApiException lockedTask = assertThrows(ApiException.class, () -> inTransaction(() ->
                workspaceRepository.updateTaskProgress(fixture.runId, fixture.studentId, fixture.childTaskId, "completed")));
        assertEquals("NODE_LOCKED", lockedTask.code());

        ApiException error = assertThrows(ApiException.class, () -> inTransaction(() ->
                workspaceRepository.updateNodeProgress(fixture.runId, fixture.studentId, fixture.rootNodeId,
                        "completed", new java.math.BigDecimal("100"))));
        assertEquals("NODE_TASKS_INCOMPLETE", error.code());

        assertEquals("pending", scalarString("SELECT status FROM node_progress WHERE run_id = ? AND node_id = ?",
                fixture.runId, fixture.rootNodeId));
        assertEquals(0, scalar("SELECT progress_percent FROM node_progress WHERE run_id = ? AND node_id = ?",
                fixture.runId, fixture.rootNodeId));
        assertEquals("locked", scalarString("SELECT status FROM node_progress WHERE run_id = ? AND node_id = ?",
                fixture.runId, fixture.childNodeId));
        assertEquals("pending", scalarString("SELECT status FROM task_progress WHERE run_id = ? AND task_id = ?",
                fixture.runId, fixture.childTaskId));
        assertEquals("not_started", scalarString("SELECT status FROM experiment_runs WHERE id = ?", fixture.runId));
        assertNull(scalarNullableLong("SELECT current_node_id FROM experiment_runs WHERE id = ?", fixture.runId));
        assertEquals("pending", scalarString("SELECT status FROM task_progress WHERE run_id = ? AND task_id = ?",
                fixture.runId, fixture.firstTaskId));
    }

    @Test
    void completingAllTasksThenNodeCompletesNodeAndUnlocksChild() {
        inTransaction(() -> workspaceRepository.updateTaskProgress(
                fixture.runId, fixture.studentId, fixture.firstTaskId, "completed"));
        RunProgressView afterSecondTask = inTransaction(() -> workspaceRepository.updateTaskProgress(
                fixture.runId, fixture.studentId, fixture.secondTaskId, "completed"));
        assertEquals(100, node(afterSecondTask, fixture.rootNodeId).progressPercent().intValue());

        RunProgressView completed = inTransaction(() -> workspaceRepository.updateNodeProgress(
                fixture.runId, fixture.studentId, fixture.rootNodeId, "completed", new java.math.BigDecimal("100")));
        assertEquals("completed", node(completed, fixture.rootNodeId).status());

        RunProgressView persisted = inTransaction(() -> workspaceRepository.findProgress(fixture.runId, fixture.studentId));
        assertEquals("completed", node(persisted, fixture.rootNodeId).status());
        assertEquals("pending", node(persisted, fixture.childNodeId).status());
        assertEquals("in_progress", persisted.runStatus());
        assertNull(persisted.currentNodeId());
    }

    @Test
    void completionFailureRollsBackNodeAndChildUpdates() throws Exception {
        inTransaction(() -> workspaceRepository.updateTaskProgress(
                fixture.runId, fixture.studentId, fixture.firstTaskId, "completed"));
        inTransaction(() -> workspaceRepository.updateTaskProgress(
                fixture.runId, fixture.studentId, fixture.secondTaskId, "completed"));

        String triggerName = fixture.prefix + "_fail_run_update";
        String createTrigger = "CREATE TRIGGER `" + SCHEMA + "`.`" + triggerName + "` "
                + "BEFORE UPDATE ON `experiment_runs` FOR EACH ROW "
                + "BEGIN IF OLD.id = " + fixture.runId + " THEN "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'integration rollback fault injection'; "
                + "END IF; END";
        String originalRunStatus = scalarString("SELECT status FROM experiment_runs WHERE id = ?", fixture.runId);
        Long originalCurrentNode = scalarNullableLong("SELECT current_node_id FROM experiment_runs WHERE id = ?", fixture.runId);
        jdbc.execute(createTrigger);
        try (AutoCloseable triggerCleanup = () -> jdbc.execute(
                "DROP TRIGGER IF EXISTS `" + SCHEMA + "`.`" + triggerName + "`")) {
            DataAccessException failure = assertThrows(DataAccessException.class, () -> inTransaction(() -> workspaceRepository.updateNodeProgress(
                    fixture.runId, fixture.studentId, fixture.rootNodeId, "completed",
                    new java.math.BigDecimal("100"))));
            Throwable rootCause = failure.getMostSpecificCause();
            assertNotNull(rootCause, "The rollback failure must have a database root cause");
            assertNotNull(rootCause.getMessage(), "The rollback root cause must contain the injected database message");
            assertTrue(rootCause.getMessage().contains("integration rollback fault injection"),
                    "The rollback scenario must fail at the fixture-scoped trigger");

            assertEquals("in_progress", scalarString("SELECT status FROM node_progress WHERE run_id = ? AND node_id = ?",
                    fixture.runId, fixture.rootNodeId));
            assertEquals(100, scalar("SELECT progress_percent FROM node_progress WHERE run_id = ? AND node_id = ?",
                    fixture.runId, fixture.rootNodeId));
            assertEquals("locked", scalarString("SELECT status FROM node_progress WHERE run_id = ? AND node_id = ?",
                    fixture.runId, fixture.childNodeId));
            assertEquals("completed", scalarString("SELECT status FROM task_progress WHERE run_id = ? AND task_id = ?",
                    fixture.runId, fixture.firstTaskId));
            assertEquals("completed", scalarString("SELECT status FROM task_progress WHERE run_id = ? AND task_id = ?",
                    fixture.runId, fixture.secondTaskId));
            assertEquals(originalRunStatus,
                    scalarString("SELECT status FROM experiment_runs WHERE id = ?", fixture.runId));
            assertEquals(originalCurrentNode,
                    scalarNullableLong("SELECT current_node_id FROM experiment_runs WHERE id = ?", fixture.runId));
        }
    }

    @Test
    void tasklessNodeCanCompleteManuallyAndProgressRemainsIsolatedPerRunAndStudent() {
        RunProgressView firstRun = inTransaction(() -> workspaceRepository.updateTaskProgress(
                fixture.runId, fixture.studentId, fixture.firstTaskId, "completed"));
        assertEquals("pending", task(inTransaction(() -> workspaceRepository.findProgress(
                fixture.otherRunId, fixture.otherStudentId)), fixture.firstTaskId).status());

        ApiException wrongOwner = assertThrows(ApiException.class, () -> inTransaction(() ->
                workspaceRepository.updateTaskProgress(fixture.runId, fixture.otherStudentId,
                        fixture.secondTaskId, "completed")));
        assertEquals("RUN_NOT_FOUND", wrongOwner.code());

        RunProgressView tasklessCompleted = inTransaction(() -> workspaceRepository.updateNodeProgress(
                fixture.runId, fixture.studentId, fixture.tasklessNodeId, "completed",
                new java.math.BigDecimal("100")));
        assertEquals("completed", node(tasklessCompleted, fixture.tasklessNodeId).status());
        assertEquals("completed", task(firstRun, fixture.firstTaskId).status());
        assertEquals("pending", task(tasklessCompleted, fixture.secondTaskId).status());

        TeacherModels.CourseDashboard dashboard = teacherRepository.dashboard(fixture.teacherId, fixture.courseId);
        assertEquals(2, dashboard.memberCount());
        assertEquals(1, dashboard.experimentCount());
        TeacherModels.StudentProgress primary = dashboard.students().stream()
                .filter(student -> student.studentId() == fixture.studentId).findFirst().orElseThrow();
        TeacherModels.StudentProgress other = dashboard.students().stream()
                .filter(student -> student.studentId() == fixture.otherStudentId).findFirst().orElseThrow();
        assertEquals(1, primary.completedNodes());
        assertEquals(3, primary.totalNodes());
        assertEquals(1, primary.completedTasks());
        assertEquals(3, primary.totalTasks());
        assertEquals(0, other.completedNodes());
        assertEquals(3, other.totalNodes());
        assertEquals(0, other.completedTasks());
        assertEquals(3, other.totalTasks());
        assertEquals("not_started", scalarString("SELECT status FROM experiment_runs WHERE id = ?", fixture.otherRunId));
        assertEquals("pending", scalarString("SELECT status FROM node_progress WHERE run_id = ? AND node_id = ?",
                fixture.otherRunId, fixture.rootNodeId));
        assertEquals(0, scalar("SELECT progress_percent FROM node_progress WHERE run_id = ? AND node_id = ?",
                fixture.otherRunId, fixture.rootNodeId));
    }

    private long insertNode(String sequence, String name, Long parentNodeId, int order) {
        return insertId("""
                INSERT INTO experiment_nodes (experiment_id, parent_node_id, sequence_code, name, sort_order)
                VALUES (?, ?, ?, ?, ?)
                """, fixture.experimentId, parentNodeId, fixture.prefix + "_" + sequence, name, order);
    }

    private long insertTask(long nodeId, String title, int order) {
        return insertId("INSERT INTO node_tasks (experiment_id, node_id, title, sort_order) VALUES (?, ?, ?, ?)",
                fixture.experimentId, nodeId, title, order);
    }

    private long insertRun(long studentId, String suffix) {
        long runId = insertId("""
                INSERT INTO experiment_runs (experiment_id, student_id, status, last_opened_at)
                VALUES (?, ?, 'not_started', CURRENT_TIMESTAMP(6))
                """, fixture.experimentId, studentId);
        fixture.runIds.add(runId);
        jdbc.update("""
                INSERT INTO coding_workspaces (run_id, storage_path, runtime_image, status, cpu_limit, memory_limit_mb)
                VALUES (?, ?, 'integration-test', 'provisioning', 1.000, 256)
                """, runId, "/tmp/" + fixture.prefix + "/" + suffix);
        return runId;
    }

    private void createProgress(long runId) {
        jdbc.update("""
                INSERT INTO node_progress (experiment_id, run_id, node_id, status, progress_percent)
                VALUES (?, ?, ?, 'pending', 0), (?, ?, ?, 'locked', 0), (?, ?, ?, 'pending', 0)
                """, fixture.experimentId, runId, fixture.rootNodeId,
                fixture.experimentId, runId, fixture.childNodeId,
                fixture.experimentId, runId, fixture.tasklessNodeId);
        jdbc.update("""
                INSERT INTO task_progress (experiment_id, run_id, task_id, status)
                VALUES (?, ?, ?, 'pending'), (?, ?, ?, 'pending'), (?, ?, ?, 'pending')
                """, fixture.experimentId, runId, fixture.firstTaskId,
                fixture.experimentId, runId, fixture.secondTaskId,
                fixture.experimentId, runId, fixture.childTaskId);
    }

    private long insertId(String sql, Object... args) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < args.length; index++) {
                statement.setObject(index + 1, args[index]);
            }
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("MySQL did not return a generated fixture key");
        long id = key.longValue();
        if (sql.startsWith("INSERT INTO users")) fixture.userIds.add(id);
        return id;
    }

    private static void verifyServerTarget(DataSource dataSource) {
        try (var connection = dataSource.getConnection()) {
            // VerifiedMySqlDataSource checks every subsequently acquired connection too.
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not verify the explicitly opted-in disposable MySQL target", exception);
        }
    }

    private static final class VerifiedMySqlDataSource extends DelegatingDataSource {
        private final String expectedServerUuid;

        private VerifiedMySqlDataSource(DataSource target, String expectedServerUuid) {
            super(target);
            this.expectedServerUuid = expectedServerUuid;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return verifyConnection(super.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return verifyConnection(super.getConnection(username, password));
        }

        private Connection verifyConnection(Connection connection) throws SQLException {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT VERSION(), DATABASE(), @@server_uuid, "
                         + "(SELECT `marker` FROM `" + DATABASE_MARKER_TABLE + "` WHERE `marker` = '"
                         + REQUIRED_DATABASE_MARKER + "')")) {
            if (!result.next() || !result.getString(1).startsWith("8.") || !SCHEMA.equals(result.getString(2))
                        || !expectedServerUuid.equals(result.getString(3))
                        || !REQUIRED_DATABASE_MARKER.equals(result.getString(4))) {
                    throw new SQLException("Connection is not the explicitly marked disposable MySQL 8 target");
                }
                return connection;
            } catch (SQLException exception) {
                try {
                    connection.close();
                } catch (SQLException closeFailure) {
                    exception.addSuppressed(closeFailure);
                }
                throw exception;
            }
        }
    }

    private static boolean isLoopbackHost(String host) {
        return host.equals("127.0.0.1");
    }

    private static NodeProgressView node(RunProgressView progress, long nodeId) {
        return progress.nodes().stream().filter(value -> value.nodeId() == nodeId).findFirst().orElseThrow();
    }

    private static TaskProgressView task(RunProgressView progress, long taskId) {
        return progress.tasks().stream().filter(value -> value.taskId() == taskId).findFirst().orElseThrow();
    }

    private static int scalar(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private static String scalarString(String sql, Object... args) {
        return jdbc.queryForObject(sql, String.class, args);
    }

    private static Long scalarNullableLong(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private static <T> T inTransaction(Supplier<T> work) {
        return transactions.execute(status -> work.get());
    }

    private static final class Fixture {
        private final String prefix;
        private final List<Long> userIds = new ArrayList<>();
        private final List<Long> runIds = new ArrayList<>();
        private Long teacherId;
        private Long studentId;
        private Long otherStudentId;
        private Long courseId;
        private Long assignmentId;
        private Long experimentId;
        private Long rootNodeId;
        private Long childNodeId;
        private Long tasklessNodeId;
        private Long firstTaskId;
        private Long secondTaskId;
        private Long childTaskId;
        private Long runId;
        private Long otherRunId;

        private Fixture(String prefix) {
            this.prefix = prefix;
        }
    }
}
