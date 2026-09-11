package com.bmhs.experimentcreation;

import com.bmhs.experimentcreation.CreationModels.ConfirmedIntent;
import com.bmhs.experimentcreation.CreationModels.Difficulty;
import com.bmhs.experimentcreation.CreationModels.ExperimentTree;
import com.bmhs.experimentcreation.CreationModels.StudentIntentEnvelope;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Component
public class McpHarnessGateway implements HarnessGateway {
    private static final String REQUIREMENTS_TOOL = "clarify_intent";
    private static final String PLANNER_TOOL = "generate_experiment_tree";
    private static final Logger logger = LoggerFactory.getLogger(McpHarnessGateway.class);

    private final List<McpSyncClient> clients;
    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    public McpHarnessGateway(List<McpSyncClient> clients) {
        this.clients = List.copyOf(clients);
    }

    @Override
    public StudentIntentEnvelope clarify(String message, StudentIntentEnvelope current, boolean confirm) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("message", message);
        arguments.put("current_intent", current == null ? null : gson.fromJson(gson.toJson(current), Map.class));
        arguments.put("confirm", confirm);
        return invoke(REQUIREMENTS_TOOL, arguments, StudentIntentEnvelope.class);
    }

    @Override
    public ExperimentTree generateTree(ConfirmedIntent intent, int durationMinutes, Difficulty difficulty) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("confirmed_intent", gson.fromJson(gson.toJson(intent), Map.class));
        arguments.put("duration_minutes", durationMinutes);
        arguments.put("difficulty", difficulty.name().toLowerCase(Locale.ROOT));
        return invoke(PLANNER_TOOL, arguments, ExperimentTree.class);
    }

    private <T> T invoke(String toolName, Map<String, Object> arguments, Class<T> resultType) {
        McpSyncClient client = null;
        for (McpSyncClient candidate : clients) {
            try {
                boolean providesTool = candidate.listTools().tools().stream()
                        .anyMatch(tool -> toolName.equals(tool.name()));
                if (providesTool) {
                    client = candidate;
                    break;
                }
            } catch (RuntimeException ignored) {
                // One unavailable Harness must not hide a healthy, independently registered one.
            }
        }
        if (client == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "HARNESS_UNAVAILABLE", "未找到提供 " + toolName + " 的 Harness");
        }

        McpSchema.CallToolResult result;
        try {
            result = client.callTool(new McpSchema.CallToolRequest(toolName, arguments));
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "HARNESS_CALL_FAILED",
                    "Harness 调用失败，请稍后重试");
        }
        if (result == null) {
            logger.warn("MCP Harness returned no result (tool={}, category=missing_result)", toolName);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "HARNESS_OUTPUT_MISSING",
                    "Harness 未返回结果，请稍后重试");
        }
        if (Boolean.TRUE.equals(result.isError())) {
            logger.warn("MCP Harness returned a tool error (tool={}, category=tool_error)", toolName);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "HARNESS_TOOL_ERROR",
                    "Harness 执行失败，请稍后重试");
        }
        if (result.structuredContent() == null) {
            logger.warn("MCP Harness returned no structured content (tool={}, category=missing_structured_content)",
                    toolName);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "HARNESS_OUTPUT_MISSING",
                    "Harness 未返回结构化结果，请稍后重试");
        }
        if (result.structuredContent() instanceof Map<?, ?> map && map.isEmpty()) {
            logger.warn("MCP Harness returned empty structured content (tool={}, category=empty_structured_content)",
                    toolName);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "HARNESS_OUTPUT_EMPTY",
                    "Harness 返回空的结构化结果，请稍后重试");
        }
        try {
            String json = gson.toJson(result.structuredContent());
            if ("null".equals(json)) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "HARNESS_OUTPUT_INVALID",
                        "Harness 返回结果不符合约定结构");
            }
            T parsed = gson.fromJson(json, resultType);
            if (parsed == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "HARNESS_OUTPUT_INVALID",
                        "Harness 返回结果不符合约定结构");
            }
            return parsed;
        } catch (RuntimeException exception) {
            logger.warn("MCP Harness structured content could not be deserialized "
                    + "(tool={}, category=deserialization_error, type={})",
                    toolName, result.structuredContent().getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "HARNESS_OUTPUT_INVALID",
                    "Harness 返回结果不符合约定结构");
        }
    }
}
