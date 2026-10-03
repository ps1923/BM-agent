package com.bmhs.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class TerminalModels {
    private TerminalModels() {}

    public record TerminalSessionView(long id, long workspaceId, String status, Instant createdAt) {}

    public record CommandRequest(@NotBlank @Size(max = 2000) String commandText,
                                 @Size(max = 700) String workingDirectory) {}

    public record CommandView(long id, long terminalSessionId, String commandText, String status,
                              Integer exitCode, String output, Instant createdAt) {}
}
