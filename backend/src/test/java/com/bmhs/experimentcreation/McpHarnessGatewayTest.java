package com.bmhs.experimentcreation;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpHarnessGatewayTest {
    private McpSyncClient client;
    private McpHarnessGateway gateway;

    @BeforeEach
    void setUp() {
        client = mock(McpSyncClient.class);
        gateway = new McpHarnessGateway(List.of(client));
        McpSchema.Tool tool = McpSchema.Tool.builder("clarify_intent")
                .description("requirements")
                .inputSchema(Map.of())
                .build();
        when(client.listTools()).thenReturn(new McpSchema.ListToolsResult(List.of(tool), null));
    }

    @Test
    void classifiesToolErrorWithoutExposingToolText() {
        when(client.callTool(any())).thenReturn(McpSchema.CallToolResult.builder()
                .addTextContent("provider secret and full model response")
                .isError(true)
                .build());

        ApiException exception = assertThrows(ApiException.class,
                () -> gateway.clarify("目标", null, false));

        assertEquals("HARNESS_TOOL_ERROR", exception.code());
        assertEquals("Harness 执行失败，请稍后重试", exception.getMessage());
    }

    @Test
    void classifiesMissingStructuredContentSeparately() {
        when(client.callTool(any())).thenReturn(McpSchema.CallToolResult.builder()
                .isError(false)
                .build());

        ApiException exception = assertThrows(ApiException.class,
                () -> gateway.clarify("目标", null, false));

        assertEquals("HARNESS_OUTPUT_MISSING", exception.code());
        assertEquals("Harness 未返回结构化结果，请稍后重试", exception.getMessage());
    }

    @Test
    void rejectsEmptyStructuredObject() {
        when(client.callTool(any())).thenReturn(McpSchema.CallToolResult.builder()
                .structuredContent(Map.of())
                .isError(false)
                .build());

        ApiException exception = assertThrows(ApiException.class,
                () -> gateway.clarify("目标", null, false));

        assertEquals("HARNESS_OUTPUT_EMPTY", exception.code());
        assertEquals("Harness 返回空的结构化结果，请稍后重试", exception.getMessage());
    }

    @Test
    void classifiesDeserializationFailureSeparately() {
        when(client.callTool(any())).thenReturn(McpSchema.CallToolResult.builder()
                .structuredContent("not an envelope")
                .isError(false)
                .build());

        ApiException exception = assertThrows(ApiException.class,
                () -> gateway.clarify("目标", null, false));

        assertEquals("HARNESS_OUTPUT_INVALID", exception.code());
        assertEquals("Harness 返回结果不符合约定结构", exception.getMessage());
    }
}
