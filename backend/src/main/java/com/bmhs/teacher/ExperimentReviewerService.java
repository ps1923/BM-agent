package com.bmhs.teacher;

import com.bmhs.auth.AuthContext;
import com.bmhs.auth.AuthModels.AuthenticatedUser;
import org.springframework.stereotype.Service;

@Service
public class ExperimentReviewerService {
    private final ExperimentReviewerRepository repository;

    public ExperimentReviewerService(ExperimentReviewerRepository repository) {
        this.repository = repository;
    }

    public ExperimentReviewerModels.AssignmentView assign(AuthenticatedUser user, long experimentId,
                                                            ExperimentReviewerModels.AssignmentRequest request) {
        AuthContext.requireRole(user, "admin");
        return repository.assign(experimentId, request.teacherId(), user.id());
    }

    public ExperimentReviewerModels.AssignmentView get(AuthenticatedUser user, long experimentId) {
        AuthContext.requireRole(user, "admin");
        return repository.find(experimentId);
    }

    public java.util.List<ExperimentReviewerModels.ReviewableExperimentView> reviewableExperiments(AuthenticatedUser user) {
        AuthContext.requireRole(user, "admin");
        return repository.reviewableExperiments();
    }

    public java.util.List<ExperimentReviewerModels.TeacherOption> activeTeachers(AuthenticatedUser user) {
        AuthContext.requireRole(user, "admin");
        return repository.activeTeachers();
    }
}
