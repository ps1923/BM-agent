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
        AuthContext.requireRole(user, "teacher");
        if (request != null && request.teacherId() != user.id()) {
            throw new com.bmhs.experimentcreation.ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "EXPERIMENT_REVIEW_FORBIDDEN", "课程教师只能将自己设为审核教师");
        }
        if (request == null) {
            throw new com.bmhs.experimentcreation.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "EXPERIMENT_REVIEW_INVALID", "请选择负责课程");
        }
        return repository.assign(experimentId, user.id(), request.courseId());
    }

    public ExperimentReviewerModels.AssignmentView get(AuthenticatedUser user, long experimentId) {
        AuthContext.requireRole(user, "teacher");
        return repository.find(experimentId, user.id());
    }

    public java.util.List<ExperimentReviewerModels.ReviewableExperimentView> reviewableExperiments(AuthenticatedUser user) {
        AuthContext.requireRole(user, "teacher");
        return repository.reviewableExperiments(user.id());
    }
}
