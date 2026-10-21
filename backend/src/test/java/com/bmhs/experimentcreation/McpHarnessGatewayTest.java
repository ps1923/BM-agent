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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test
    void refusesAmbiguousToolProvidersWithoutCallingEitherHarness() {
        McpSyncClient second = mock(McpSyncClient.class);
        McpSchema.Tool tool = McpSchema.Tool.builder("clarify_intent")
                .description("duplicate requirements tool")
                .inputSchema(Map.of())
                .build();
        when(second.listTools()).thenReturn(new McpSchema.ListToolsResult(List.of(tool), null));
        McpHarnessGateway ambiguousGateway = new McpHarnessGateway(List.of(client, second));

        ApiException exception = assertThrows(ApiException.class,
                () -> ambiguousGateway.clarify("目标", null, false));

        assertEquals("HARNESS_TOOL_AMBIGUOUS", exception.code());
        verify(client, never()).callTool(any());
        verify(second, never()).callTool(any());
    }

    @Test
    void usesTheDiscoverableProviderWhenAnotherHarnessIsUnavailable() {
        McpSyncClient unavailable = mock(McpSyncClient.class);
        when(unavailable.listTools()).thenThrow(new IllegalStateException("private endpoint detail"));
        when(client.callTool(any())).thenReturn(McpSchema.CallToolResult.builder()
                .structuredContent(Map.of("schema_version", "v1", "status", "collecting", "assistant_message", "ok"))
                .isError(false)
                .build());
        McpHarnessGateway fallbackGateway = new McpHarnessGateway(List.of(unavailable, client));

        var result = fallbackGateway.clarify("目标", null, false);

        assertEquals("ok", result.assistantMessage());
        verify(client).callTool(any());
    }

    @Test
    void distinguishesDiscoveryFailureFromAHealthyHarnessWithoutTheTool() {
        when(client.listTools()).thenThrow(new IllegalStateException("private endpoint detail"));

        ApiException exception = assertThrows(ApiException.class,
                () -> gateway.clarify("目标", null, false));

        assertEquals("HARNESS_DISCOVERY_FAILED", exception.code());
        verify(client, never()).callTool(any());
    }
}
