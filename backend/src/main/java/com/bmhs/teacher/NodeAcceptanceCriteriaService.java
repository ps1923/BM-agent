package com.bmhs.teacher;

import com.bmhs.auth.AuthContext;
import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.experimentcreation.CreationModels.AcceptanceCriterion;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class NodeAcceptanceCriteriaService {
    private static final Set<String> METHODS = Set.of("static_code", "test_artifact", "manual_demo");
    private final JdbcTemplate jdbc;
    private final NodeAcceptanceCriteriaRepository repository;

    public NodeAcceptanceCriteriaService(JdbcTemplate jdbc, NodeAcceptanceCriteriaRepository repository) {
        this.jdbc = jdbc;
        this.repository = repository;
    }

    public List<NodeAcceptanceCriteriaModels.RevisionView> history(AuthenticatedUser user, long experimentId, long nodeId) {
        AuthContext.requireRole(user, "teacher");
        assertCanManage(user.id(), experimentId, nodeId);
        return repository.history(experimentId, nodeId);
    }

    public List<NodeAcceptanceCriteriaModels.NodeView> list(AuthenticatedUser user, long experimentId) {
        AuthContext.requireRole(user, "teacher");
        List<NodeAcceptanceCriteriaModels.NodeView> nodes = repository.currentForExperiment(experimentId);
        if (nodes.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "EXPERIMENT_NOT_FOUND", "实验不存在或没有可管理节点");
        assertCanManage(user.id(), experimentId, nodes.get(0).nodeId());
        return nodes;
    }

    @Transactional
    public NodeAcceptanceCriteriaModels.RevisionView update(AuthenticatedUser user, long experimentId, long nodeId,
                                                             NodeAcceptanceCriteriaModels.UpdateRequest request) {
        AuthContext.requireRole(user, "teacher");
        assertCanManage(user.id(), experimentId, nodeId);
        if (request == null || request.criteria() == null || request.criteria().isEmpty()
                || request.criteria().size() > 30) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ACCEPTANCE_CRITERIA_INVALID", "请填写至少一条验收准则");
        }
        List<AcceptanceCriterion> criteria = request.criteria().stream().map(item -> {
            if (item == null || !METHODS.contains(item.verificationMethod())
                    || vague(item.criterion()) || vague(item.expectedEvidence())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ACCEPTANCE_CRITERIA_INVALID",
                        "验收准则必须包含具体通过条件、检查方式和可观察证据");
            }
            return item.toCriterion();
        }).toList();
        return repository.append(experimentId, nodeId, user.id(), criteria);
    }

    private void assertCanManage(long teacherId, long experimentId, long nodeId) {
        Integer allowed = jdbc.query("""
                SELECT 1 FROM experiments e JOIN experiment_nodes n ON n.experiment_id = e.id
                WHERE e.id = ? AND n.id = ? AND (
                  e.creator_id = ? OR EXISTS (
                    SELECT 1 FROM course_experiment_assignments a JOIN courses c ON c.id = a.course_id
                    WHERE a.experiment_id = e.id AND a.status = 'published' AND c.teacher_id = ? AND c.status = 'active'
                  ) OR EXISTS (
                    SELECT 1 FROM experiment_reviewers er JOIN courses c ON c.id = er.course_id
                    WHERE er.experiment_id = e.id AND er.teacher_id = ? AND c.teacher_id = ? AND c.status = 'active'
                  )
                )
                """, rs -> rs.next() ? 1 : null, experimentId, nodeId, teacherId, teacherId, teacherId, teacherId);
        if (allowed == null) throw new ApiException(HttpStatus.FORBIDDEN, "ACCEPTANCE_CRITERIA_FORBIDDEN", "无权管理该实验节点的验收准则");
    }

    private boolean vague(String value) {
        if (value == null || value.isBlank()) return true;
        String normalized = value.trim();
        return Set.of("功能正常", "代码正确", "完成模块", "works", "works correctly")
                .contains(normalized.toLowerCase(Locale.ROOT));
    }
}
