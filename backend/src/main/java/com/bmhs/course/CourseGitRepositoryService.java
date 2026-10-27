package com.bmhs.course;

import com.bmhs.auth.AuthContext;
import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.course.CourseModels.CourseGitRepositoryRequest;
import com.bmhs.course.CourseModels.CourseGitRepositoryView;
import com.bmhs.experimentcreation.ApiException;
import com.bmhs.git.GitConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Locale;

@Service
public class CourseGitRepositoryService {
    private final CourseRepository repository;
    private final GitConfig gitConfig;

    public CourseGitRepositoryService(CourseRepository repository, GitConfig gitConfig) {
        this.repository = repository;
        this.gitConfig = gitConfig;
    }

    public CourseGitRepositoryView get(AuthenticatedUser user, long courseId) {
        requireTeacher(user);
        CourseRepository.CourseRow course = requireOwnedCourse(user, courseId);
        return new CourseGitRepositoryView(course.id(), repository.findGitRepositoryUrl(courseId));
    }

    @Transactional
    public CourseGitRepositoryView update(AuthenticatedUser user, long courseId,
                                         CourseGitRepositoryRequest request) {
        requireTeacher(user);
        CourseRepository.CourseRow course = requireOwnedCourse(user, courseId);
        if (request == null || request.repositoryUrl() == null || request.repositoryUrl().isBlank()) {
            throw invalid("仓库地址不能为空");
        }
        String repositoryUrl = validateRepositoryUrl(request.repositoryUrl());
        repository.updateGitRepositoryUrl(courseId, repositoryUrl);
        return new CourseGitRepositoryView(course.id(), repositoryUrl);
    }

    private CourseRepository.CourseRow requireOwnedCourse(AuthenticatedUser user, long courseId) {
        CourseRepository.CourseRow course = repository.findCourse(courseId);
        if (course == null || course.teacherId() != user.id()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "COURSE_FORBIDDEN", "只能管理自己负责课程的 Git 仓库");
        }
        return course;
    }

    private String validateRepositoryUrl(String value) {
        String publicBase = gitConfig.publicBaseUrl();
        if (publicBase == null || publicBase.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GIT_REPOSITORY_NOT_CONFIGURED", "平台尚未配置 Gitea 公网地址");
        }
        URI uri = parse(value.trim());
        URI base = parse(publicBase);
        String scheme = lower(uri.getScheme());
        if (!scheme.equals(lower(base.getScheme())) || !lower(uri.getHost()).equals(lower(base.getHost()))
                || effectivePort(uri) != effectivePort(base)) {
            throw invalid("仓库地址必须属于平台配置的 Gitea 服务");
        }
        String basePath = normalizePath(base.getPath());
        String path = normalizePath(uri.getPath());
        if (!basePath.isEmpty() && !(path.equals(basePath) || path.startsWith(basePath + "/"))) {
            throw invalid("仓库地址不在平台 Gitea 服务路径内");
        }
        String relativePath = basePath.isEmpty() ? path : path.substring(basePath.length());
        String[] segments = relativePath.replaceFirst("^/", "").split("/");
        if (segments.length != 2 || segments[0].isBlank() || segments[1].isBlank()) {
            throw invalid("请填写完整的 Gitea 仓库地址，例如 https://git.example.edu/owner/repository");
        }
        if (segments[1].toLowerCase(Locale.ROOT).endsWith(".git")) {
            segments[1] = segments[1].substring(0, segments[1].length() - 4);
        }
        if (segments[0].equals(".") || segments[0].equals("..")
                || segments[1].equals(".") || segments[1].equals("..")
                || !segments[0].matches("[A-Za-z0-9_.-]{1,100}")
                || !segments[1].matches("[A-Za-z0-9_.-]{1,100}")) {
            throw invalid("仓库地址包含不支持的路径字符");
        }
        String prefix = basePath;
        return scheme + "://" + uri.getRawAuthority() + prefix + "/" + segments[0] + "/" + segments[1];
    }

    private URI parse(String value) {
        try {
            URI uri = URI.create(value.trim());
            String scheme = lower(uri.getScheme());
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null || uri.getPort() == 0 || uri.getPort() > 65535
                    || !(scheme.equals("http") || scheme.equals("https"))) {
                throw invalid("仓库地址格式无效，不能包含凭据、查询参数或片段");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw invalid("仓库地址格式无效");
        }
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() != -1) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private String normalizePath(String value) {
        if (value == null || value.equals("/")) return "";
        String result = value;
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private String lower(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
    private void requireTeacher(AuthenticatedUser user) { AuthContext.requireRole(user, "teacher"); }
    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "COURSE_GIT_REPOSITORY_INVALID", message);
    }
}
