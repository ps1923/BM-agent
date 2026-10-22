package com.bmhs.git;

import java.util.List;

public interface GiteaGateway {
    GitModels.GiteaRepository ensureRepository(long runId, long studentId, String owner, String name,
                                                String webhookUrl, String webhookSecret);
    String createGitToken(String username, long runId);
    GitModels.CommitInfo commit(String owner, String repo, String sha, String token);
    List<GitModels.CommitFile> filesAtCommit(String owner, String repo, GitModels.CommitInfo commit, String token);
}
