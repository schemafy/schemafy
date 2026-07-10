package com.schemafy.mcp.resource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

final class SchemafyMcpFeatureFactory {

  private static final String JSON_MIME_TYPE = "application/json";

  private SchemafyMcpFeatureFactory() {}

  static McpServerFeatures.AsyncResourceSpecification resource(
      String uri,
      String name,
      String description,
      Function<McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> readHandler) {
    McpSchema.Resource resource = McpSchema.Resource.builder()
        .uri(uri)
        .name(name)
        .title(name)
        .description(description)
        .mimeType(JSON_MIME_TYPE)
        .build();
    return new McpServerFeatures.AsyncResourceSpecification(resource,
        (McpAsyncServerExchange exchange, McpSchema.ReadResourceRequest request) -> readHandler.apply(request));
  }

  static McpServerFeatures.AsyncResourceTemplateSpecification template(
      String uriTemplate,
      String name,
      String description,
      Function<McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> readHandler) {
    McpSchema.ResourceTemplate resourceTemplate = McpSchema.ResourceTemplate.builder()
        .uriTemplate(uriTemplate)
        .name(name)
        .title(name)
        .description(description)
        .mimeType(JSON_MIME_TYPE)
        .build();
    return new McpServerFeatures.AsyncResourceTemplateSpecification(resourceTemplate,
        (McpAsyncServerExchange exchange, McpSchema.ReadResourceRequest request) -> readHandler.apply(request));
  }

  static McpServerFeatures.AsyncToolSpecification tool(
      String name,
      String title,
      String description,
      Map<String, Object> properties,
      List<String> required,
      Function<McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> callHandler) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
        .name(name)
        .title(title)
        .description(description)
        .inputSchema(objectSchema(properties, required))
        .annotations(readOnlyToolAnnotations(title))
        .build();
    return McpServerFeatures.AsyncToolSpecification.builder()
        .tool(tool)
        .callHandler((exchange, request) -> callHandler.apply(request))
        .build();
  }

  static Map<String, Object> idArgument(
      String name,
      String description) {
    return Map.of(name, Map.of(
        "type", "string",
        "description", description));
  }

  static Map<String, Object> paginationArguments() {
    return Map.of(
        "page", Map.of(
            "type", "integer",
            "description", "Zero-based page number. Defaults to 0.",
            "minimum", 0,
            "default", 0),
        "size", Map.of(
            "type", "integer",
            "description", "Page size from 1 to 100. Defaults to 100.",
            "minimum", 1,
            "maximum", 100,
            "default", 100));
  }

  static Map<String, Object> pagedIdArgument(
      String name,
      String description) {
    Map<String, Object> arguments = new LinkedHashMap<>(idArgument(name, description));
    arguments.putAll(paginationArguments());
    return arguments;
  }

  private static McpSchema.JsonSchema objectSchema(
      Map<String, Object> properties,
      List<String> required) {
    return new McpSchema.JsonSchema("object", properties, required, false, null, null);
  }

  private static McpSchema.ToolAnnotations readOnlyToolAnnotations(String title) {
    return new McpSchema.ToolAnnotations(title, true, false, true, false, false);
  }

}
