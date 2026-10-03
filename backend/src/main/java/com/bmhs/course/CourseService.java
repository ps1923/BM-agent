package com.bmhs.course;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.course.CourseModels.CourseMember;
import com.bmhs.course.CourseModels.CourseView;
import com.bmhs.course.CourseModels.ExperimentSummary;
import com.bmhs.course.CourseModels.PublishedExperiment;
import com.bmhs.experimentcreation.CreationModels.MaterializedExperiment;
import com.bmhs.experimentcreation.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseService {
    private final CourseRepository repository;

    public CourseService(CourseRepository repository) {
        this.repository = repository;
    }

    public CourseView create(AuthenticatedUser user, String name) {
        return repository.create(user.id(), name);
    }

    public List<CourseView> list(AuthenticatedUser user) {
        if ("teacher".equals(user.role())) return repository.findCoursesForTeacher(user.id());
        if ("student".equals(user.role())) return repository.findCoursesForStudent(user.id());
        throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_FORBIDDEN", "当前角色不能访问课程数据");
    }

    public CourseView join(AuthenticatedUser user, long courseId, String inviteCode) {
        if (!"student".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_FORBIDDEN", "只有学生可以加入课程");
        }
        return repository.join(courseId, user.id(), inviteCode);
    }

    public CourseView joinByInvite(AuthenticatedUser user, String inviteCode) {
        if (!"student".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_FORBIDDEN", "只有学生可以加入课程");
        }
        return repository.joinByInvite(user.id(), inviteCode);
    }

    public List<CourseMember> members(AuthenticatedUser user, long courseId) {
        CourseRepository.CourseRow course = repository.findCourse(courseId);
        if (course == null || course.teacherId() != user.id()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "COURSE_FORBIDDEN", "只能查看自己课程的成员");
        }
        return repository.findMembers(courseId);
    }

    public List<ExperimentSummary> experiments(AuthenticatedUser user) {
        return repository.findExperimentsForTeacher(user.id());
    }

    public List<ExperimentSummary> myExperiments(AuthenticatedUser user) {
        if (!"student".equals(user.role()) && !"teacher".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_FORBIDDEN", "当前角色不能访问个人实验库");
        }
        return repository.findExperimentsCreatedBy(user.id());
    }

    @Transactional
    public PublishedExperiment publish(AuthenticatedUser user, long experimentId, long courseId) {
        return repository.publish(user.id(), experimentId, courseId);
    }

    public MaterializedExperiment experiment(AuthenticatedUser user, long experimentId) {
        return repository.findExperiment(experimentId, user.id());
    }
}
