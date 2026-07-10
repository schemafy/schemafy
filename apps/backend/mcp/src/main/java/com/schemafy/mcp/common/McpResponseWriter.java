package com.schemafy.mcp.common;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpResponseWriter {

  private static final String JSON_MIME_TYPE = "application/json";
  private static final String RESOURCE_FAILURE_MESSAGE = "Schemafy MCP resource read failed";
  private static final String TOOL_FAILURE_MESSAGE = "Schemafy MCP tool call failed";

  private final JsonCodec jsonCodec;

  public Mono<McpSchema.ReadResourceResult> resourcePayload(String uri, Mono<?> payload) {
    return payload.flatMap(value -> Mono.fromCallable(() -> {
      String text = json(value, "resource");
      McpSchema.TextResourceContents contents = new McpSchema.TextResourceContents(
          uri,
          JSON_MIME_TYPE,
          text,
          null);
      return new McpSchema.ReadResourceResult(List.of(contents));
    })).onErrorMap(this::resourceError);
  }

  public Mono<McpSchema.CallToolResult> toolPayload(Mono<?> payload) {
    return payload.flatMap(this::toolJson)
        .onErrorResume(error -> Mono.just(toolError(toolErrorMessage(error))));
  }

  public Mono<McpSchema.CallToolResult> toolJson(Object payload) {
    return Mono.fromCallable(() -> toolText(json(payload, "tool"), false));
  }

  public McpSchema.CallToolResult toolError(String message) {
    return toolText(message == null ? TOOL_FAILURE_MESSAGE : message, true);
  }

  private Throwable resourceError(Throwable error) {
    if (isExpected(error)) {
      return error;
    }
    log.error("Unexpected Schemafy MCP resource failure", error);
    return new IllegalStateException(RESOURCE_FAILURE_MESSAGE);
  }

  private String toolErrorMessage(Throwable error) {
    if (isExpected(error)) {
      return error.getMessage();
    }
    log.error("Unexpected Schemafy MCP tool failure", error);
    return TOOL_FAILURE_MESSAGE;
  }

  private boolean isExpected(Throwable error) {
    return error instanceof DomainException
        || error instanceof IllegalArgumentException
        || error instanceof AccessDeniedException;
  }

  private McpSchema.CallToolResult toolText(String text, boolean isError) {
    return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), isError);
  }

  private String json(Object payload, String kind) {
    try {
      return jsonCodec.toJson(payload);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Failed to serialize MCP " + kind + " payload", e);
    }
  }

}
