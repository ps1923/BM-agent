package com.bmhs.auth;

import com.bmhs.auth.AuthModels.AuthenticatedUser;
import com.bmhs.auth.AuthModels.ProfileUpdateRequest;
import com.bmhs.auth.AuthModels.ProfileView;
import com.bmhs.experimentcreation.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final AuthRepository repository;

    public ProfileService(AuthRepository repository) {
        this.repository = repository;
    }

    public ProfileView get(AuthenticatedUser user) {
        requireSupportedRole(user);
        ProfileView profile = repository.findProfile(user.id());
        if (profile == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "请先登录");
        return profile;
    }

    @Transactional
    public ProfileView update(AuthenticatedUser user, ProfileUpdateRequest request) {
        requireSupportedRole(user);
        if (request == null) throw new ApiException(HttpStatus.BAD_REQUEST, "PROFILE_INVALID", "个人资料不能为空");
        String displayName = request.displayName().trim();
        String gitAuthorName = clean(request.gitAuthorName());
        String gitAuthorEmail = clean(request.gitAuthorEmail());
        if (displayName.isBlank() || displayName.length() > 100 || gitAuthorName.length() > 100
                || gitAuthorEmail.length() > 320 || containsLineBreak(displayName)
                || containsLineBreak(gitAuthorName) || containsLineBreak(gitAuthorEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PROFILE_INVALID", "个人资料字段长度或内容无效");
        }
        ProfileView profile = repository.updateProfile(user.id(), displayName, emptyToNull(gitAuthorName),
                emptyToNull(gitAuthorEmail));
        if (profile == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "请先登录");
        return profile;
    }

    private void requireSupportedRole(AuthenticatedUser user) {
        if (user == null || !("student".equals(user.role()) || "teacher".equals(user.role()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_FORBIDDEN", "当前用户无权访问个人资料");
        }
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private boolean containsLineBreak(String value) { return value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0; }
    private String emptyToNull(String value) { return value.isBlank() ? null : value; }
}
