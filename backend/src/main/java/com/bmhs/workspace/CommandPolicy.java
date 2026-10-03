package com.bmhs.workspace;

import com.bmhs.experimentcreation.ApiException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class CommandPolicy {
    private static final Set<String> ALLOWED = Set.of(
            "mvn", "mvnw", "gradle", "gradlew", "python", "python3", "pytest",
            "java", "javac", "node", "npm", "ls", "pwd", "cat", "find", "grep");

    private CommandPolicy() {}

    static List<String> parse(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > 2000) reject("命令不能为空且长度不能超过 2000 字符");
        if (raw.chars().anyMatch(Character::isISOControl)
                || raw.matches(".*[;&|<>$`()].*")) reject("命令包含不允许的 Shell 特殊字符");
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        char quote = 0;
        boolean escaped = false;
        for (char character : raw.trim().toCharArray()) {
            if (escaped) { token.append(character); escaped = false; continue; }
            if (character == '\\' && quote != '\'') { escaped = true; continue; }
            if ((character == '\'' || character == '"')) {
                if (quote == 0) quote = character;
                else if (quote == character) quote = 0;
                else token.append(character);
                continue;
            }
            if (Character.isWhitespace(character) && quote == 0) {
                if (!token.isEmpty()) { tokens.add(token.toString()); token.setLength(0); }
            } else token.append(character);
        }
        if (quote != 0 || escaped) reject("命令引号不完整");
        if (!token.isEmpty()) tokens.add(token.toString());
        if (tokens.isEmpty()) reject("命令不能为空");
        String executable = tokens.get(0).replace("./", "");
        if (!ALLOWED.contains(executable)) reject("命令不在允许列表中");
        for (String argument : tokens) {
            if (argument.contains("../") || argument.equals("..") || argument.startsWith("/proc")
                    || argument.startsWith("/sys") || argument.startsWith("/dev")
                    || argument.equals("--privileged") || argument.equals("--mount")
                    || (argument.startsWith("/") && !isWorkspaceOrTempPath(argument))) {
                reject("命令参数访问了受保护资源");
            }
        }
        tokens.set(0, executable);
        return List.copyOf(tokens);
    }

    static String workingDirectory(String raw) {
        String value = raw == null || raw.isBlank() ? "." : raw.replace('\\', '/');
        if (value.startsWith("/") || value.matches("^[A-Za-z]:.*") || value.contains("..")) {
            reject("工作目录无效");
        }
        return value.equals(".") ? "/workspace" : "/workspace/" + value;
    }

    private static boolean isWorkspaceOrTempPath(String value) {
        return value.equals("/workspace") || value.startsWith("/workspace/")
                || value.equals("/tmp") || value.startsWith("/tmp/");
    }

    private static void reject(String message) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "COMMAND_REJECTED", message);
    }
}
