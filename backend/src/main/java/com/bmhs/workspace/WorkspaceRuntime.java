package com.bmhs.workspace;

public interface WorkspaceRuntime {
    String start(WorkspaceModels.WorkspaceRecord workspace);

    default void stop(String runtimeInstanceId) {
        // Test and non-container runtimes may not own a separately managed process.
    }

    default void pause(String runtimeInstanceId) {
        // Test and non-container runtimes do not run background workspace processes.
    }

    default void resume(String runtimeInstanceId) {
        // Test and non-container runtimes do not run background workspace processes.
    }
}
