package com.schemafy.mcp.common;

import java.util.List;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.json.JsonCodec;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class McpResponseWriter {

  private static final String JSON_MIME_TYPE = "application/json";

  private final JsonCodec jsonCodec;

  public Mono<McpSchema.ReadResourceResult> resourceJson(String uri, Object payload) {
    return Mono.fromCallable(() -> {
      String text = json(payload, "resource");
      McpSchema.TextResourceContents contents = new McpSchema.TextResourceContents(
          uri,
          JSON_MIME_TYPE,
          text,
          null);
      return new McpSchema.ReadResourceResult(List.of(contents));
    });
  }

  public Mono<McpSchema.CallToolResult> toolPayload(Mono<?> payload) {
    return payload.flatMap(this::toolJson)
        .onErrorResume(error -> Mono.just(toolError(error.getMessage())));
  }

  public Mono<McpSchema.CallToolResult> toolJson(Object payload) {
    return Mono.fromCallable(() -> toolText(json(payload, "tool"), false));
  }

  public McpSchema.CallToolResult toolError(String message) {
    return toolText(message == null ? "Schemafy MCP tool call failed" : message, true);
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
