package com.bmhs.git;

import com.bmhs.experimentcreation.ApiException;
import com.bmhs.experimentcreation.CreationModels.AcceptanceCriterion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.FieldNamingPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GitSubmissionRepository {
    private static final Type EVIDENCE_TYPE = new TypeToken<List<GitModels.Evidence>>() {}.getType();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();
    private static final Type CRITERIA_TYPE = new TypeToken<List<AcceptanceCriterion>>() {}.getType();
    private static final Type CRITERION_RESULTS_TYPE = new TypeToken<List<GitModels.CriterionResult>>() {}.getType();
    private final JdbcTemplate jdbc;
    private final Gson gson = new Gson();
    private final Gson criteriaGson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    public GitSubmissionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public GitModels.RegisteredRun createOrFindRun(long experimentId, long studentId) {
        assertRunnableAccess(experimentId, studentId);
        List<GitModels.RegisteredRun> existing = jdbc.query("""
                SELECT id, experiment_id, student_id, status FROM experiment_runs
                WHERE experiment_id = ? AND student_id = ? AND execution_mode = 'git'
                """, (rs, rowNum) -> new GitModels.RegisteredRun(rs.getLong("id"), rs.getLong("experiment_id"),
                rs.getLong("student_id"), rs.getString("status")), experimentId, studentId);
        if (!existing.isEmpty()) return existing.get(0);
        int inserted = jdbc.update("""
                INSERT IGNORE INTO experiment_runs (experiment_id, student_id, execution_mode, status, last_opened_at)
                VALUES (?, ?, 'git', 'not_started', CURRENT_TIMESTAMP(6))
                """, experimentId, studentId);
        GitModels.RegisteredRun run = jdbc.queryForObject("""
                SELECT id, experiment_id, student_id, status FROM experiment_runs
                WHERE experiment_id = ? AND student_id = ? AND execution_mode = 'git'
                """, (rs, rowNum) -> new GitModels.RegisteredRun(rs.getLong("id"), rs.getLong("experiment_id"),
                rs.getLong("student_id"), rs.getString("status")), experimentId, studentId);
        if (inserted == 1) createProgressRows(run.runId(), experimentId);
        return run;
    }

    public GitModels.RegisteredRun requireStudentRun(long runId, long studentId) {
        List<GitModels.RegisteredRun> rows = jdbc.query("""
                SELECT id, experiment_id, student_id, status FROM experiment_runs
                WHERE id = ? AND student_id = ? AND execution_mode = 'git'
                """, (rs, rowNum) -> new GitModels.RegisteredRun(rs.getLong("id"), rs.getLong("experiment_id"),
                rs.getLong("student_id"), rs.getString("status")), runId, studentId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "GIT_RUN_NOT_FOUND", "Git 实验运行实例不存在或无权访问");
        return rows.get(0);
    }

    @Transactional
    public GitModels.RepositoryRecord createRepositoryRecord(GitModels.RegisteredRun run, String owner,
                                                               String repoName, String repoUrl, String cloneUrl) {
        jdbc.update("""
                INSERT IGNORE INTO git_repositories
                  (run_id, experiment_id, student_id, gitea_owner, repo_name, repo_url, clone_url,
                   branch_name, credential_username, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'main', ?, 'provisioning')
                """, run.runId(), run.experimentId(), run.studentId(), owner, repoName, repoUrl, cloneUrl, owner);
        return repositoryForStudent(run.runId(), run.studentId());
    }

    public GitModels.RepositoryRecord repositoryForStudent(long runId, long studentId) {
        return repositoryForRun(runId, "AND student_id = ?", studentId);
    }

    public GitModels.RepositoryRecord repositoryForRun(long runId) {
        return repositoryForRun(runId, "", new Object[0]);
    }

    public void saveCredential(long runId, String encrypted) {
        jdbc.update("UPDATE git_repositories SET credential_ciphertext = ?, status = 'provisioning', last_error_code = NULL WHERE run_id = ?",
                encrypted, runId);
    }

    public void markRepositoryReady(long runId, long repoId) {
        jdbc.update("UPDATE git_repositories SET gitea_repo_id = ?, status = 'ready', last_error_code = NULL WHERE run_id = ?",
                repoId, runId);
    }

    public void markRepositoryError(long runId, String code) {
        jdbc.update("UPDATE git_repositories SET status = 'error', last_error_code = ? WHERE run_id = ?",
                code, runId);
    }

    @Transactional
    public GitModels.SubmissionRecord registerSubmission(GitModels.RegisteredRun run, String sha,
                                                           GitModels.CommitInfo commit, List<Long> nodeIds,
                                                           String description) {
        List<GitModels.SubmissionRecord> existing = findByRunSha(run.runId(), sha);
        if (!existing.isEmpty()) {
            List<Long> existingNodes = nodeIds(existing.get(0).id());
            if (!existingNodes.equals(nodeIds.stream().sorted().toList())) {
                throw new ApiException(HttpStatus.CONFLICT, "GIT_COMMIT_ALREADY_REGISTERED", "该 commit 已登记，不能更改关联节点");
            }
            return existing.get(0);
        }
        int validNodes = jdbc.queryForObject("""
                SELECT COUNT(*) FROM experiment_nodes WHERE experiment_id = ? AND id IN ("""
                + placeholders(nodeIds.size()) + ")", Integer.class, bind(run.experimentId(), nodeIds));
        if (validNodes != nodeIds.size()) throw new ApiException(HttpStatus.BAD_REQUEST, "GIT_NODE_OUTSIDE_EXPERIMENT", "所选节点包含不属于当前实验的节点");
        KeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO git_submissions
                      (run_id, experiment_id, student_id, commit_sha, parent_sha, commit_message,
                       commit_author, committed_at, description, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'queued')
                    """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, run.runId());
                statement.setLong(2, run.experimentId());
                statement.setLong(3, run.studentId());
                statement.setString(4, commit.sha());
                statement.setString(5, commit.parentShas().isEmpty() ? null : commit.parentShas().get(0));
                statement.setString(6, safeLimit(commit.message(), 1000));
                statement.setString(7, safeLimit(commit.author(), 200));
                statement.setTimestamp(8, commit.committedAt() == null ? null : Timestamp.from(commit.committedAt()));
                statement.setString(9, description);
                return statement;
            }, keys);
        } catch (DuplicateKeyException exception) {
            List<GitModels.SubmissionRecord> raced = findByRunSha(run.runId(), sha);
            if (raced.isEmpty()) throw exception;
            if (!nodeIds(raced.get(0).id()).equals(nodeIds.stream().sorted().toList())) {
                throw new ApiException(HttpStatus.CONFLICT, "GIT_COMMIT_ALREADY_REGISTERED", "该 commit 已登记，不能更改关联节点");
            }
            return raced.get(0);
        }
        long submissionId = requiredKey(keys);
        for (Long nodeId : nodeIds.stream().sorted().toList()) {
            CriteriaSnapshot snapshot = criteriaSnapshot(run.experimentId(), nodeId);
            jdbc.update("""
                    INSERT INTO git_submission_nodes
                      (submission_id, experiment_id, node_id, criteria_version, criteria_source, criteria_snapshot_json)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, submissionId, run.experimentId(), nodeId, snapshot.version(), snapshot.source(),
                    criteriaGson.toJson(snapshot.criteria()));
        }
        jdbc.update("UPDATE experiment_runs SET status = 'in_progress', started_at = COALESCE(started_at, CURRENT_TIMESTAMP(6)), last_opened_at = CURRENT_TIMESTAMP(6) WHERE id = ?",
                run.runId());
        return findSubmission(submissionId);
    }

    public List<GitModels.SubmissionRecord> findByRunSha(long runId, String sha) {
        return jdbc.query("""
                SELECT id, run_id, experiment_id, student_id, commit_sha, parent_sha, commit_message,
                       commit_author, committed_at, description, status, created_at
                FROM git_submissions WHERE run_id = ? AND commit_sha = ?
                """, this::submissionRow, runId, sha);
    }

    public GitModels.SubmissionRecord findSubmission(long id) {
        List<GitModels.SubmissionRecord> rows = jdbc.query("""
                SELECT id, run_id, experiment_id, student_id, commit_sha, parent_sha, commit_message,
                       commit_author, committed_at, description, status, created_at
                FROM git_submissions WHERE id = ?
                """, this::submissionRow, id);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "GIT_SUBMISSION_NOT_FOUND", "代码提交记录不存在");
        return rows.get(0);
    }

    public boolean claim(long submissionId) {
        return jdbc.update("UPDATE git_submissions SET status = 'running', error_code = NULL WHERE id = ? AND status = 'queued'", submissionId) == 1;
    }

    public void recoverStaleRunningSubmissions() {
        jdbc.update("UPDATE git_submissions SET status = 'queued', error_code = NULL WHERE status = 'running' AND updated_at < CURRENT_TIMESTAMP(6) - INTERVAL 20 MINUTE");
    }

    public boolean retryFailedSubmission(long submissionId) {
        return jdbc.update("UPDATE git_submissions SET status = 'queued', error_code = NULL WHERE id = ? AND status = 'failed'", submissionId) == 1;
    }

    @Transactional
    public void saveResults(long submissionId, long experimentId, List<GitModels.NodeSpec> nodes,
                            List<GitModels.RawResult> results) {
        java.util.Map<Long, GitModels.NodeSpec> nodesById = nodes.stream()
                .collect(java.util.stream.Collectors.toMap(GitModels.NodeSpec::nodeId, item -> item));
        for (GitModels.RawResult result : results) {
            GitModels.NodeSpec node = nodesById.get(result.nodeId());
            if (node == null) throw new IllegalStateException("验收结果没有匹配的节点准则快照");
            jdbc.update("""
                    INSERT INTO git_acceptance_results
                      (submission_id, experiment_id, node_id, decision, summary, evidence_json,
                       suggestions_json, bug_draft_json, execution_check, criteria_version, criteria_source,
                       criteria_snapshot_json, criterion_results_json)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'not_run', ?, ?, ?, ?)
                    """, submissionId, experimentId, result.nodeId(), result.decision(), result.summary(),
                    gson.toJson(result.evidence()), gson.toJson(result.suggestions()),
                    result.bugDraft() == null ? null : gson.toJson(result.bugDraft()), node.criteriaVersion(),
                    node.criteriaSource(), criteriaGson.toJson(node.acceptanceCriteria()),
                    gson.toJson(result.criterionResults()));
        }
        jdbc.update("UPDATE git_submissions SET status = 'completed', error_code = NULL WHERE id = ?", submissionId);
    }

    public void fail(long submissionId, String code) {
        jdbc.update("UPDATE git_submissions SET status = 'failed', error_code = ? WHERE id = ? AND status <> 'completed'",
                safeLimit(code, 80), submissionId);
    }

    public boolean markWebhookSeen(long repoId, String sha) {
        return jdbc.update("""
                UPDATE git_submissions s JOIN git_repositories r ON r.run_id = s.run_id
                SET s.webhook_seen_at = COALESCE(s.webhook_seen_at, CURRENT_TIMESTAMP(6))
                WHERE r.gitea_repo_id = ? AND s.commit_sha = ?
                """, repoId, sha) > 0;
    }

    public GitModels.SubmissionRecord findByRepoIdAndSha(long repoId, String sha) {
        List<GitModels.SubmissionRecord> rows = jdbc.query("""
                SELECT s.id, s.run_id, s.experiment_id, s.student_id, s.commit_sha, s.parent_sha,
                       s.commit_message, s.commit_author, s.committed_at, s.description, s.status, s.created_at
                FROM git_submissions s JOIN git_repositories r ON r.run_id = s.run_id
                WHERE r.gitea_repo_id = ? AND s.commit_sha = ?
                """, this::submissionRow, repoId, sha);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Long> queuedSubmissionIds() {
        return jdbc.query("SELECT id FROM git_submissions WHERE status = 'queued' ORDER BY created_at, id LIMIT 100",
                (rs, rowNum) -> rs.getLong(1));
    }

    public List<Long> nodeIds(long submissionId) {
        return jdbc.query("SELECT node_id FROM git_submission_nodes WHERE submission_id = ? ORDER BY node_id",
                (rs, rowNum) -> rs.getLong(1), submissionId);
    }

    public List<GitModels.NodeSpec> nodeSpecs(long submissionId) {
        List<GitModels.NodeSpec> nodes = jdbc.query("""
                SELECT n.id, n.sequence_code, n.name, n.description,
                       COALESCE(sn.criteria_version, c.version) AS criteria_version,
                       COALESCE(sn.criteria_source, c.source) AS criteria_source,
                       COALESCE(sn.criteria_snapshot_json, c.criteria_json) AS criteria_json
                FROM git_submission_nodes sn JOIN experiment_nodes n
                  ON n.id = sn.node_id AND n.experiment_id = sn.experiment_id
                LEFT JOIN experiment_node_acceptance_criteria c ON c.experiment_id = n.experiment_id
                  AND c.node_id = n.id AND c.version = (
                    SELECT MAX(latest.version) FROM experiment_node_acceptance_criteria latest
                    WHERE latest.experiment_id = n.experiment_id AND latest.node_id = n.id)
                WHERE sn.submission_id = ? ORDER BY n.sort_order, n.id
                """, (rs, rowNum) -> {
            String json = rs.getString("criteria_json");
            List<AcceptanceCriterion> criteria = json == null ? List.of() : criteriaGson.fromJson(json, CRITERIA_TYPE);
            return new GitModels.NodeSpec(rs.getLong("id"), rs.getString("sequence_code"), rs.getString("name"),
                    rs.getString("description"), List.of(), criteria == null ? List.of() : criteria,
                    rs.getObject("criteria_version", Integer.class), rs.getString("criteria_source"));
        }, submissionId);
        List<GitModels.NodeSpec> withTasks = new ArrayList<>();
        for (GitModels.NodeSpec node : nodes) {
            List<String> tasks = jdbc.query("SELECT CONCAT(title, IF(description IS NULL OR description = '', '', CONCAT(': ', description))) FROM node_tasks WHERE node_id = ? ORDER BY sort_order, id",
                    (rs, rowNum) -> rs.getString(1), node.nodeId());
            List<AcceptanceCriterion> criteria = node.acceptanceCriteria();
            if (criteria == null || criteria.isEmpty()) {
                criteria = List.of(new AcceptanceCriterion(node.name(), "static_code",
                        node.description() + (tasks.isEmpty() ? "" : "；" + String.join("；", tasks))));
            }
            withTasks.add(new GitModels.NodeSpec(node.nodeId(), node.code(), node.name(), node.description(), tasks,
                    criteria, node.criteriaVersion(), node.criteriaSource() == null ? "legacy_inferred" : node.criteriaSource()));
        }
        return List.copyOf(withTasks);
    }

    private CriteriaSnapshot criteriaSnapshot(long experimentId, long nodeId) {
        List<CriteriaSnapshot> rows = jdbc.query("""
                SELECT c.version, c.source, c.criteria_json
                FROM experiment_nodes n
                LEFT JOIN experiment_node_acceptance_criteria c ON c.experiment_id = n.experiment_id
                  AND c.node_id = n.id AND c.version = (
                    SELECT MAX(latest.version) FROM experiment_node_acceptance_criteria latest
                    WHERE latest.experiment_id = n.experiment_id AND latest.node_id = n.id)
                WHERE n.experiment_id = ? AND n.id = ?
                """, (rs, rowNum) -> {
            String json = rs.getString("criteria_json");
            List<AcceptanceCriterion> criteria = json == null ? List.of() : criteriaGson.fromJson(json, CRITERIA_TYPE);
            return new CriteriaSnapshot(rs.getObject("version", Integer.class), rs.getString("source"),
                    criteria == null ? List.of() : criteria);
        }, experimentId, nodeId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "GIT_NODE_OUTSIDE_EXPERIMENT", "所选节点不属于当前实验");
        CriteriaSnapshot snapshot = rows.get(0);
        return snapshot.version() == null
                ? new CriteriaSnapshot(null, "legacy_inferred", List.of()) : snapshot;
    }

    private record CriteriaSnapshot(Integer version, String source, List<AcceptanceCriterion> criteria) {}

    public GitModels.StudentSubmissionView studentSubmission(long id, long studentId) {
        GitModels.SubmissionRecord submission = findStudentSubmission(id, studentId);
        return new GitModels.StudentSubmissionView(id, submission.commitSha(), submission.status(), results(id), submission.createdAt());
    }

    public GitModels.SubmissionRecord findStudentSubmission(long id, long studentId) {
        List<GitModels.SubmissionRecord> rows = jdbc.query("""
                SELECT s.id, s.run_id, s.experiment_id, s.student_id, s.commit_sha, s.parent_sha,
                       s.commit_message, s.commit_author, s.committed_at, s.description, s.status, s.created_at
                FROM git_submissions s JOIN experiment_runs r ON r.id = s.run_id
                WHERE s.id = ? AND s.student_id = ? AND r.execution_mode = 'git'
                """, this::submissionRow, id, studentId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "GIT_SUBMISSION_NOT_FOUND", "代码提交记录不存在或无权访问");
        return rows.get(0);
    }

    public List<GitModels.StudentSubmissionSummary> studentSubmissions(long runId, long studentId) {
        List<GitModels.SubmissionRecord> rows = jdbc.query("""
                SELECT s.id, s.run_id, s.experiment_id, s.student_id, s.commit_sha, s.parent_sha,
                       s.commit_message, s.commit_author, s.committed_at, s.description, s.status, s.created_at
                FROM git_submissions s JOIN experiment_runs r ON r.id = s.run_id
                WHERE s.run_id = ? AND s.student_id = ? AND r.execution_mode = 'git'
                ORDER BY s.created_at DESC, s.id DESC
                """, this::submissionRow, runId, studentId);
        if (rows.isEmpty()) {
            List<Long> ownedRun = jdbc.query("SELECT id FROM experiment_runs WHERE id = ? AND student_id = ? AND execution_mode = 'git'",
                    (rs, rowNum) -> rs.getLong(1), runId, studentId);
            if (ownedRun.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "GIT_RUN_NOT_FOUND", "Git 实验运行实例不存在或无权访问");
        }
        return rows.stream().map(row -> new GitModels.StudentSubmissionSummary(row.id(), row.commitSha(),
                row.status(), row.description(), nodeIds(row.id()), results(row.id()), row.createdAt())).toList();
    }

    public List<GitModels.TeacherSubmissionSummary> teacherSubmissions(long teacherId, long runId) {
        assertTeacherRun(teacherId, runId);
        List<GitModels.SubmissionRecord> rows = jdbc.query("""
                SELECT id, run_id, experiment_id, student_id, commit_sha, parent_sha, commit_message,
                       commit_author, committed_at, description, status, created_at
                FROM git_submissions WHERE run_id = ? ORDER BY created_at DESC, id DESC
                """, this::submissionRow, runId);
        return rows.stream().map(row -> new GitModels.TeacherSubmissionSummary(row.id(), row.commitSha(),
                row.status(), nodeIds(row.id()), row.message(), row.committedAt(), row.createdAt())).toList();
    }

    public GitModels.SubmissionRecord teacherSubmission(long teacherId, long submissionId) {
        List<GitModels.SubmissionRecord> rows = jdbc.query("""
                SELECT s.id, s.run_id, s.experiment_id, s.student_id, s.commit_sha, s.parent_sha,
                       s.commit_message, s.commit_author, s.committed_at, s.description, s.status, s.created_at
                FROM git_submissions s JOIN experiment_runs r ON r.id = s.run_id
                WHERE s.id = ? AND r.execution_mode = 'git' AND (
                  EXISTS (SELECT 1 FROM course_experiment_assignments a JOIN courses c ON c.id = a.course_id
                    JOIN course_members m ON m.course_id = c.id AND m.student_id = r.student_id AND m.status = 'active'
                    WHERE a.experiment_id = r.experiment_id AND a.status = 'published' AND c.teacher_id = ? AND c.status = 'active')
                  OR EXISTS (SELECT 1 FROM experiment_reviewers er
                             JOIN courses rc ON rc.id = er.course_id AND rc.teacher_id = ? AND rc.status = 'active'
                             JOIN course_members rm ON rm.course_id = rc.id AND rm.student_id = r.student_id AND rm.status = 'active'
                             JOIN experiments re ON re.id = er.experiment_id AND re.creator_id = rm.student_id
                             WHERE er.experiment_id = r.experiment_id AND er.teacher_id = ?)
                )
                """, this::submissionRow, submissionId, teacherId, teacherId, teacherId);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.FORBIDDEN, "SUBMISSION_FORBIDDEN", "无权查看该实验提交");
        return rows.get(0);
    }

    public List<GitModels.NodeResult> results(long submissionId) {
        return jdbc.query("""
                SELECT node_id, decision, summary, evidence_json, suggestions_json, bug_draft_json,
                       criterion_results_json, criteria_version, criteria_source, criteria_snapshot_json
                FROM git_acceptance_results WHERE submission_id = ? ORDER BY node_id
                """, (rs, rowNum) -> new GitModels.NodeResult(rs.getLong("node_id"), rs.getString("decision"),
                rs.getString("summary"), gson.fromJson(rs.getString("evidence_json"), EVIDENCE_TYPE),
                gson.fromJson(rs.getString("suggestions_json"), STRING_LIST_TYPE),
                rs.getString("bug_draft_json") == null ? null : gson.fromJson(rs.getString("bug_draft_json"), GitModels.BugDraft.class),
                rs.getString("criterion_results_json") == null ? List.of()
                        : gson.fromJson(rs.getString("criterion_results_json"), CRITERION_RESULTS_TYPE),
                rs.getObject("criteria_version", Integer.class), rs.getString("criteria_source"),
                rs.getString("criteria_snapshot_json") == null ? List.of()
                        : criteriaGson.fromJson(rs.getString("criteria_snapshot_json"), CRITERIA_TYPE)),
                submissionId);
    }

    private GitModels.RepositoryRecord repositoryForRun(long runId, String condition, Object... args) {
        List<GitModels.RepositoryRecord> rows = jdbc.query("""
                SELECT run_id, experiment_id, student_id, gitea_repo_id, gitea_owner, repo_name,
                       repo_url, clone_url, branch_name, credential_username, credential_ciphertext, status
                FROM git_repositories WHERE run_id = """ + runId + " " + condition,
                (rs, rowNum) -> new GitModels.RepositoryRecord(rs.getLong("run_id"), rs.getLong("experiment_id"),
                        rs.getLong("student_id"), rs.getObject("gitea_repo_id", Long.class), rs.getString("gitea_owner"),
                        rs.getString("repo_name"), rs.getString("repo_url"), rs.getString("clone_url"),
                        rs.getString("branch_name"), rs.getString("credential_username"),
                        rs.getString("credential_ciphertext"), rs.getString("status")), args);
        if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "GIT_REPOSITORY_NOT_FOUND", "Git 仓库尚未初始化");
        return rows.get(0);
    }

    private void assertRunnableAccess(long experimentId, long studentId) {
        Integer allowed = jdbc.query("""
                SELECT 1 FROM experiments e WHERE e.id = ? AND EXISTS (
                  SELECT 1 FROM experiment_nodes n WHERE n.experiment_id = e.id) AND (
                  (e.creator_id = ? AND e.status = 'draft') OR
                  (e.status = 'published' AND EXISTS (
                    SELECT 1 FROM course_experiment_assignments a JOIN courses c ON c.id = a.course_id
                    JOIN course_members m ON m.course_id = c.id AND m.student_id = ? AND m.status = 'active'
                    WHERE a.experiment_id = e.id AND a.status = 'published' AND c.status = 'active')))
                """, rs -> rs.next() ? 1 : null, experimentId, studentId, studentId);
        if (allowed == null) {
            Integer exists = jdbc.query("SELECT 1 FROM experiments WHERE id = ?", rs -> rs.next() ? 1 : null, experimentId);
            if (exists == null) throw new ApiException(HttpStatus.NOT_FOUND, "EXPERIMENT_NOT_FOUND", "实验不存在");
            Integer hasNodes = jdbc.query("SELECT 1 FROM experiment_nodes WHERE experiment_id = ? LIMIT 1", rs -> rs.next() ? 1 : null, experimentId);
            if (hasNodes == null) throw new ApiException(HttpStatus.CONFLICT, "EXPERIMENT_HAS_NO_NODES", "该实验还没有学习节点，暂不能创建 Git 运行实例");
            throw new ApiException(HttpStatus.FORBIDDEN, "EXPERIMENT_FORBIDDEN", "当前学生不能运行该实验");
        }
    }

    private void assertTeacherRun(long teacherId, long runId) {
        Integer allowed = jdbc.query("""
                SELECT 1 FROM experiment_runs r WHERE r.id = ? AND r.execution_mode = 'git' AND (
                  EXISTS (SELECT 1 FROM course_experiment_assignments a JOIN courses c ON c.id = a.course_id
                    JOIN course_members m ON m.course_id = c.id AND m.student_id = r.student_id AND m.status = 'active'
                    WHERE a.experiment_id = r.experiment_id AND a.status = 'published' AND c.teacher_id = ? AND c.status = 'active')
                    OR EXISTS (SELECT 1 FROM experiment_reviewers er
                               JOIN courses rc ON rc.id = er.course_id AND rc.teacher_id = ? AND rc.status = 'active'
                               JOIN course_members rm ON rm.course_id = rc.id AND rm.student_id = r.student_id AND rm.status = 'active'
                               JOIN experiments re ON re.id = er.experiment_id AND re.creator_id = rm.student_id
                               WHERE er.experiment_id = r.experiment_id AND er.teacher_id = ?))
                """, rs -> rs.next() ? 1 : null, runId, teacherId, teacherId, teacherId);
        if (allowed == null) throw new ApiException(HttpStatus.FORBIDDEN, "RUN_FORBIDDEN", "无权查看该学生 Git 运行实例");
    }

    private void createProgressRows(long runId, long experimentId) {
        jdbc.update("""
                INSERT INTO node_progress (experiment_id, run_id, node_id, status, progress_percent)
                SELECT experiment_id, ?, id, 'pending', 0 FROM experiment_nodes WHERE experiment_id = ?
                """, runId, experimentId);
        jdbc.update("""
                INSERT INTO task_progress (experiment_id, run_id, task_id, status)
                SELECT experiment_id, ?, id, 'pending' FROM node_tasks WHERE experiment_id = ?
                """, runId, experimentId);
    }

    private GitModels.SubmissionRecord submissionRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Timestamp committedAt = rs.getTimestamp("committed_at");
        return new GitModels.SubmissionRecord(rs.getLong("id"), rs.getLong("run_id"), rs.getLong("experiment_id"),
                rs.getLong("student_id"), rs.getString("commit_sha"), rs.getString("parent_sha"),
                rs.getString("commit_message"), rs.getString("commit_author"), committedAt == null ? null : committedAt.toInstant(),
                rs.getString("description"), rs.getString("status"), rs.getTimestamp("created_at").toInstant());
    }

    private long requiredKey(KeyHolder keys) {
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("Database did not return generated key");
        return key.longValue();
    }

    private String placeholders(int count) { return String.join(",", java.util.Collections.nCopies(count, "?")); }
    private Object[] bind(long experimentId, List<Long> ids) {
        Object[] args = new Object[ids.size() + 1]; args[0] = experimentId;
        for (int index = 0; index < ids.size(); index++) args[index + 1] = ids.get(index);
        return args;
    }
    private String safeLimit(String value, int max) { return value == null ? null : value.substring(0, Math.min(value.length(), max)); }
}
