package com.bmhs.course;

import com.bmhs.auth.AuthContext;
import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.course.CourseModels.CreateCourseRequest;
import com.bmhs.course.CourseModels.JoinCourseRequest;
import com.bmhs.course.CourseModels.PublishExperimentRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CourseController {
    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    @PostMapping("/courses")
    public ResponseEntity<?> create(AuthenticatedUser user, @Valid @RequestBody CreateCourseRequest request) {
        AuthContext.requireRole(user, "teacher");
        return ResponseEntity.ok(service.create(user, request.name()));
    }

    @GetMapping("/courses")
    public ResponseEntity<?> list(AuthenticatedUser user) {
        return ResponseEntity.ok(service.list(user));
    }

    @PostMapping("/courses/{courseId}/join")
    public ResponseEntity<?> join(@PathVariable long courseId, AuthenticatedUser user,
                                  @Valid @RequestBody JoinCourseRequest request) {
        return ResponseEntity.ok(service.join(user, courseId, request.inviteCode()));
    }

    @PostMapping("/courses/join")
    public ResponseEntity<?> joinByInvite(AuthenticatedUser user,
                                          @Valid @RequestBody JoinCourseRequest request) {
        return ResponseEntity.ok(service.joinByInvite(user, request.inviteCode()));
    }

    @GetMapping("/courses/{courseId}/members")
    public ResponseEntity<?> members(@PathVariable long courseId, AuthenticatedUser user) {
        AuthContext.requireRole(user, "teacher");
        return ResponseEntity.ok(service.members(user, courseId));
    }

    @GetMapping("/experiments/drafts")
    public ResponseEntity<?> experiments(AuthenticatedUser user) {
        AuthContext.requireRole(user, "teacher");
        return ResponseEntity.ok(service.experiments(user));
    }

    @GetMapping("/experiments/mine")
    public ResponseEntity<?> myExperiments(AuthenticatedUser user) {
        return ResponseEntity.ok(service.myExperiments(user));
    }

    @GetMapping("/experiments/{experimentId:\\d+}")
    public ResponseEntity<?> experiment(@PathVariable long experimentId, AuthenticatedUser user) {
        return ResponseEntity.ok(service.experiment(user, experimentId));
    }

    @PostMapping("/experiments/{experimentId}/publish")
    public ResponseEntity<?> publish(@PathVariable long experimentId, AuthenticatedUser user,
                                     @Valid @RequestBody PublishExperimentRequest request) {
        AuthContext.requireRole(user, "teacher");
        return ResponseEntity.ok(Map.of("publication", service.publish(user, experimentId, request.courseId())));
    }
}
