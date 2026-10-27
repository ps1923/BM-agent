package com.bmhs.rag;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.auth.AuthContext;
import com.bmhs.dsh.DshPrincipal;
import com.bmhs.experimentcreation.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BugService {
    private final BugRepository repository;
    private final RagService ragService;

    public BugService(BugRepository repository, RagService ragService) {
        this.repository = repository;
        this.ragService = ragService;
    }

    public RagModels.BugCaseView create(AuthenticatedUser user, long runId, RagModels.BugRequest request) {
        requireRole(user, "student");
        return repository.create(runId, user.id(), request);
    }

    public RagModels.BugCaseView createFromDsh(DshPrincipal principal, long runId, RagModels.BugRequest request) {
        return repository.create(runId, principal.studentId(), request);
    }

    public List<RagModels.BugCaseView> list(AuthenticatedUser user, Long courseId) {
        if ("student".equals(user.role())) {
            return repository.findVisibleForStudent(user.id());
        }
        if ("teacher".equals(user.role()) && courseId != null) {
            return repository.findForTeacher(user.id(), courseId);
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_FORBIDDEN", "当前角色不能查看 Bug 案例");
    }

    public RagModels.BugCaseView review(AuthenticatedUser user, long bugCaseId, RagModels.BugReviewRequest request) {
        requireRole(user, "teacher");
        RagModels.BugCaseView result = repository.review(bugCaseId, user.id(), request);
        if ("approved".equals(result.status())) ragService.indexApproved(result);
        else if (result.chromaDocumentId() != null) ragService.removeRejectedIndex(result);
        return repository.findById(bugCaseId);
    }

    public List<RagModels.BugCaseView> listForExperiment(AuthenticatedUser user, long experimentId) {
        requireRole(user, "teacher");
        return repository.findForExperimentForTeacher(user.id(), experimentId);
    }

    private void requireRole(AuthenticatedUser user, String role) {
        AuthContext.requireRole(user, role);
    }
}
