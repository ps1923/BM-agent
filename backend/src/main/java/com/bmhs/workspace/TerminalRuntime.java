package com.bmhs.workspace;

import java.util.List;

public interface TerminalRuntime {
    TerminalResult execute(WorkspaceModels.WorkspaceRecord workspace, List<String> command,
                           String workingDirectory, long timeoutMs, int maxOutputBytes);

    record TerminalResult(String status, Integer exitCode, String output) {}
}
