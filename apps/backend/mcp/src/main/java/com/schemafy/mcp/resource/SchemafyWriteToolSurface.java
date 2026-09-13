package com.schemafy.mcp.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameUseCase;
import com.schemafy.core.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.core.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.core.erd.memo.application.port.in.CreateMemoCommand;
import com.schemafy.core.erd.memo.application.port.in.CreateMemoCommentCommand;
import com.schemafy.core.erd.memo.application.port.in.CreateMemoCommentUseCase;
import com.schemafy.core.erd.memo.application.port.in.CreateMemoUseCase;
import com.schemafy.core.erd.memo.application.port.in.UpdateMemoCommentCommand;
import com.schemafy.core.erd.memo.application.port.in.UpdateMemoCommentUseCase;
import com.schemafy.core.erd.memo.application.port.in.UpdateMemoPositionCommand;
import com.schemafy.core.erd.memo.application.port.in.UpdateMemoPositionUseCase;
import com.schemafy.core.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.core.erd.schema.application.port.in.ChangeSchemaNameUseCase;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.core.erd.sync.ErdStateSyncPublisher;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameUseCase;
import com.schemafy.core.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.core.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.core.mcp.domain.McpScope;
import com.schemafy.core.project.application.access.ProjectAccessRequesterContext;
import com.schemafy.core.project.application.port.in.CreateProjectCommand;
import com.schemafy.core.project.application.port.in.CreateProjectUseCase;
import com.schemafy.core.project.application.port.in.CreateWorkspaceCommand;
import com.schemafy.core.project.application.port.in.CreateWorkspaceUseCase;
import com.schemafy.core.project.application.port.in.UpdateProjectCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectUseCase;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceCommand;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceUseCase;
import com.schemafy.core.project.application.port.in.WorkspaceDetail;
import com.schemafy.mcp.common.McpResponseWriter;
import com.schemafy.mcp.common.security.McpAuthenticatedPrincipal;
import com.schemafy.mcp.common.security.McpSecurityAuditLogger;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.mcp.resource.SchemafyMcpFeatureFactory.createTool;
import static com.schemafy.mcp.resource.SchemafyMcpFeatureFactory.updateTool;

@Component
@RequiredArgsConstructor
final class SchemafyWriteToolSurface {

  private final McpResponseWriter responseWriter;
  private final McpSecurityAuditLogger auditLogger;
  private final ObjectMapper objectMapper;
  private final ObjectProvider<ErdStateSyncPublisher> stateSyncPublisher;
  private final CreateWorkspaceUseCase createWorkspaceUseCase;
  private final UpdateWorkspaceUseCase updateWorkspaceUseCase;
  private final CreateProjectUseCase createProjectUseCase;
  private final UpdateProjectUseCase updateProjectUseCase;
  private final CreateSchemaUseCase createSchemaUseCase;
  private final ChangeSchemaNameUseCase changeSchemaNameUseCase;
  private final CreateTableUseCase createTableUseCase;
  private final ChangeTableNameUseCase changeTableNameUseCase;
  private final CreateColumnUseCase createColumnUseCase;
  private final ChangeColumnNameUseCase changeColumnNameUseCase;
  private final CreateMemoUseCase createMemoUseCase;
  private final UpdateMemoPositionUseCase updateMemoPositionUseCase;
  private final CreateMemoCommentUseCase createMemoCommentUseCase;
  private final UpdateMemoCommentUseCase updateMemoCommentUseCase;

  List<McpServerFeatures.AsyncToolSpecification> specifications() {
    return List.of(
        createTool("schemafy_create_workspace", "Create workspace", "Create a workspace.",
            strings("name", "Workspace name.", "description", "Optional description."), List.of("name"),
            this::createWorkspace),
        updateTool("schemafy_update_workspace", "Update workspace", "Update a workspace.",
            strings("workspaceId", "Workspace ID.", "name", "Workspace name.", "description", "Optional description."),
            List.of("workspaceId", "name"), this::updateWorkspace),
        createTool("schemafy_create_project", "Create project", "Create a project in a workspace.",
            projectProperties(), List.of("workspaceId", "dbVendorId", "name"), this::createProject),
        updateTool("schemafy_update_project", "Update project", "Update a project.",
            strings("projectId", "Project ID.", "name", "Project name.", "description", "Optional description."),
            List.of("projectId", "name"), this::updateProject),
        createTool("schemafy_create_schema", "Create schema", "Create a schema in a project.",
            strings("projectId", "Project ID.", "name", "Schema name.", "charset", "Optional charset.", "collation",
                "Optional collation."),
            List.of("projectId", "name"), this::createSchema),
        updateTool("schemafy_rename_schema", "Rename schema", "Rename a schema.",
            strings("schemaId", "Schema ID.", "newName", "New schema name."), List.of("schemaId", "newName"),
            this::renameSchema),
        createTool("schemafy_create_table", "Create table", "Create a table in a schema.",
            tableProperties(), List.of("schemaId", "name"), this::createTable),
        updateTool("schemafy_rename_table", "Rename table", "Rename a table.",
            strings("tableId", "Table ID.", "newName", "New table name."), List.of("tableId", "newName"),
            this::renameTable),
        createTool("schemafy_create_column", "Create column", "Create a column in a table.",
            columnProperties(), List.of("tableId", "name", "dataType"), this::createColumn),
        updateTool("schemafy_rename_column", "Rename column", "Rename a column.",
            strings("columnId", "Column ID.", "newName", "New column name."), List.of("columnId", "newName"),
            this::renameColumn),
        createTool("schemafy_create_memo", "Create memo", "Create a memo and its initial comment.",
            createMemoProperties(), List.of("schemaId", "positions", "body"), this::createMemo),
        updateTool("schemafy_move_memo", "Move memo", "Update a memo position.",
            moveMemoProperties(), List.of("memoId", "positions"), this::moveMemo),
        createTool("schemafy_create_memo_comment", "Create memo comment", "Add a comment to a memo.",
            strings("memoId", "Memo ID.", "body", "Comment body."), List.of("memoId", "body"),
            this::createMemoComment),
        updateTool("schemafy_update_memo_comment", "Update memo comment", "Update your memo comment.",
            strings("commentId", "Memo comment ID.", "body", "Comment body."), List.of("commentId", "body"),
            this::updateMemoComment));
  }

  private Mono<McpSchema.CallToolResult> createWorkspace(McpSchema.CallToolRequest request) {
    return execute("schemafy_create_workspace", McpScope.WORKSPACE_WRITE, null,
        result -> result instanceof WorkspaceDetail detail ? detail.workspace().getId() : null,
        actor -> createWorkspaceUseCase.createWorkspace(new CreateWorkspaceCommand(
            required(request, "name"), optional(request, "description"), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> updateWorkspace(McpSchema.CallToolRequest request) {
    return execute("schemafy_update_workspace", McpScope.WORKSPACE_WRITE, request, "workspaceId",
        actor -> updateWorkspaceUseCase.updateWorkspace(new UpdateWorkspaceCommand(required(request, "workspaceId"),
            required(request, "name"), optional(request, "description"), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> createProject(McpSchema.CallToolRequest request) {
    return execute("schemafy_create_project", McpScope.WORKSPACE_WRITE, request, "workspaceId",
        actor -> createProjectUseCase.createProject(new CreateProjectCommand(required(request, "workspaceId"),
            positiveInt(request, "dbVendorId"), required(request, "name"), optional(request, "description"),
            actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> updateProject(McpSchema.CallToolRequest request) {
    return execute("schemafy_update_project", McpScope.WORKSPACE_WRITE, request, "projectId",
        actor -> updateProjectUseCase.updateProject(new UpdateProjectCommand(required(request, "projectId"),
            required(request, "name"), optional(request, "description"), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> createSchema(McpSchema.CallToolRequest request) {
    return execute("schemafy_create_schema", McpScope.ERD_WRITE, request, "projectId", actor -> createSchemaUseCase
        .createSchema(new CreateSchemaCommand(required(request, "projectId"), required(request, "name"),
            optional(request, "charset"), optional(request, "collation")))
        .flatMap(result -> publishSchema(result.result().id(), result).thenReturn(mutationPayload(result))));
  }

  private Mono<McpSchema.CallToolResult> renameSchema(McpSchema.CallToolRequest request) {
    return execute("schemafy_rename_schema", McpScope.ERD_WRITE, request, "schemaId", actor -> {
      String schemaId = required(request, "schemaId");
      return changeSchemaNameUseCase.changeSchemaName(new ChangeSchemaNameCommand(schemaId, required(request,
          "newName")))
          .flatMap(result -> publishSchema(schemaId, result).thenReturn(mutationPayload(result)));
    });
  }

  private Mono<McpSchema.CallToolResult> createTable(McpSchema.CallToolRequest request) {
    return execute("schemafy_create_table", McpScope.ERD_WRITE, request, "schemaId", actor -> createTableUseCase
        .createTable(new CreateTableCommand(required(request, "schemaId"), required(request, "name"),
            optional(request, "charset"), optional(request, "collation"), optionalObject(request, "extra")))
        .flatMap(result -> publishMutation(result).thenReturn(mutationPayload(result))));
  }

  private Mono<McpSchema.CallToolResult> renameTable(McpSchema.CallToolRequest request) {
    return execute("schemafy_rename_table", McpScope.ERD_WRITE, request, "tableId", actor -> changeTableNameUseCase
        .changeTableName(new ChangeTableNameCommand(required(request, "tableId"), required(request, "newName")))
        .flatMap(result -> publishMutation(result).thenReturn(mutationPayload(result))));
  }

  private Mono<McpSchema.CallToolResult> createColumn(McpSchema.CallToolRequest request) {
    return execute("schemafy_create_column", McpScope.ERD_WRITE, request, "tableId", actor -> createColumnUseCase
        .createColumn(new CreateColumnCommand(required(request, "tableId"), required(request, "name"),
            required(request, "dataType"), optionalInt(request, "length"), optionalInt(request, "precision"),
            optionalInt(request, "scale"), optionalBoolean(request, "autoIncrement", false), optional(request,
                "charset"),
            optional(request, "collation"), optional(request, "comment"), optionalStringList(request, "values")))
        .flatMap(result -> publishMutation(result).thenReturn(mutationPayload(result))));
  }

  private Mono<McpSchema.CallToolResult> renameColumn(McpSchema.CallToolRequest request) {
    return execute("schemafy_rename_column", McpScope.ERD_WRITE, request, "columnId", actor -> changeColumnNameUseCase
        .changeColumnName(new ChangeColumnNameCommand(required(request, "columnId"), required(request, "newName")))
        .flatMap(result -> publishMutation(result).thenReturn(mutationPayload(result))));
  }

  private Mono<McpSchema.CallToolResult> createMemo(McpSchema.CallToolRequest request) {
    return execute("schemafy_create_memo", McpScope.MEMO_WRITE, request, "schemaId", actor -> createMemoUseCase
        .createMemo(new CreateMemoCommand(required(request, "schemaId"), requiredObject(request, "positions"),
            required(request, "body"), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> moveMemo(McpSchema.CallToolRequest request) {
    return execute("schemafy_move_memo", McpScope.MEMO_WRITE, request, "memoId", actor -> updateMemoPositionUseCase
        .updateMemoPosition(new UpdateMemoPositionCommand(required(request, "memoId"),
            requiredObject(request, "positions"), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> createMemoComment(McpSchema.CallToolRequest request) {
    return execute("schemafy_create_memo_comment", McpScope.MEMO_WRITE, request, "memoId",
        actor -> createMemoCommentUseCase.createMemoComment(new CreateMemoCommentCommand(required(request, "memoId"),
            required(request, "body"), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> updateMemoComment(McpSchema.CallToolRequest request) {
    return execute("schemafy_update_memo_comment", McpScope.MEMO_WRITE, request, "commentId",
        actor -> updateMemoCommentUseCase.updateMemoComment(new UpdateMemoCommentCommand(required(request, "commentId"),
            required(request, "body"), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> execute(String tool, McpScope scope, McpSchema.CallToolRequest request,
      String targetArgument, Function<Actor, Mono<?>> action) {
    return execute(tool, scope, stringArgument(request, targetArgument), result -> null, action);
  }

  private Mono<McpSchema.CallToolResult> execute(String tool, McpScope scope, String targetId,
      Function<Object, String> createdTargetId, Function<Actor, Mono<?>> action) {
    return responseWriter.toolResult(actor().flatMap(actor -> {
      if (!actor.principal().scopes().contains(scope.value())) {
        AccessDeniedException error = new AccessDeniedException("MCP token scope is insufficient");
        auditLogger.writeToolFailed(actor.principal(), tool, targetId, error);
        return Mono.just(responseWriter.toolError(error.getMessage()));
      }
      return responseWriter.toolPayload(Mono.defer(() -> action.apply(actor))
          .doOnSuccess(result -> auditLogger.writeToolSucceeded(actor.principal(), tool,
              targetId != null ? targetId : createdTargetId.apply(result),
              result instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get("noOp"))))
          .doOnError(error -> auditLogger.writeToolFailed(actor.principal(), tool, targetId, error)));
    }));
  }

  private Mono<Actor> actor() {
    return Mono.deferContextual(context -> ReactiveSecurityContextHolder.getContext()
        .map(securityContext -> securityContext.getAuthentication().getPrincipal())
        .cast(McpAuthenticatedPrincipal.class)
        .switchIfEmpty(Mono.error(new AccessDeniedException("MCP principal is missing")))
        .map(principal -> {
          String requesterId = ProjectAccessRequesterContext.requesterIdOrNull(context);
          if (requesterId == null || !requesterId.equals(principal.userId())) {
            throw new AccessDeniedException("MCP requester context is invalid");
          }
          return new Actor(principal, requesterId);
        }));
  }

  private Mono<Void> publishSchema(String schemaId, MutationResult<?> result) {
    ErdStateSyncPublisher publisher = stateSyncPublisher.getIfAvailable();
    return publisher == null ? Mono.empty() : publisher.publishSchemaChange(schemaId, result.operation());
  }

  private Mono<Void> publishMutation(MutationResult<?> result) {
    ErdStateSyncPublisher publisher = stateSyncPublisher.getIfAvailable();
    return publisher == null ? Mono.empty() : publisher.publishMutation(result.affectedTableIds(), result.operation());
  }

  private Map<String, Object> mutationPayload(MutationResult<?> result) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("result", result.result());
    payload.put("affectedTableIds", result.affectedTableIds());
    payload.put("operation", result.operation());
    payload.put("noOp", result.noOp());
    return payload;
  }

  private String required(McpSchema.CallToolRequest request, String name) {
    String value = stringArgument(request, name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required");
    }
    return value;
  }

  private String optional(McpSchema.CallToolRequest request, String name) {
    Object value = argument(request, name);
    if (value == null) {
      return null;
    }
    if (value instanceof String text) {
      return text;
    }
    throw new IllegalArgumentException(name + " must be a string");
  }

  private int positiveInt(McpSchema.CallToolRequest request, String name) {
    int value = integer(argument(request, name), name);
    if (value < 1) {
      throw new IllegalArgumentException(name + " must be a positive integer");
    }
    return value;
  }

  private Integer optionalInt(McpSchema.CallToolRequest request, String name) {
    Object value = argument(request, name);
    return value == null ? null : integer(value, name);
  }

  private int integer(Object value, String name) {
    if (value instanceof Number number && number.doubleValue() == number.intValue()) {
      return number.intValue();
    }
    if (value instanceof String text) {
      try {
        return Integer.parseInt(text);
      } catch (NumberFormatException ignored) {
        // Use the common validation message below.
      }
    }
    throw new IllegalArgumentException(name + " must be an integer");
  }

  private boolean optionalBoolean(McpSchema.CallToolRequest request, String name, boolean defaultValue) {
    Object value = argument(request, name);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Boolean bool) {
      return bool;
    }
    throw new IllegalArgumentException(name + " must be a boolean");
  }

  private List<String> optionalStringList(McpSchema.CallToolRequest request, String name) {
    Object value = argument(request, name);
    if (value == null) {
      return null;
    }
    if (!(value instanceof List<?> values)) {
      throw new IllegalArgumentException(name + " must be an array of strings");
    }
    List<String> strings = new ArrayList<>();
    for (Object item : values) {
      if (!(item instanceof String text)) {
        throw new IllegalArgumentException(name + " must be an array of strings");
      }
      strings.add(text);
    }
    return List.copyOf(strings);
  }

  private JsonNode requiredObject(McpSchema.CallToolRequest request, String name) {
    JsonNode node = objectNode(argument(request, name), name);
    if (node == null) {
      throw new IllegalArgumentException(name + " is required");
    }
    return node;
  }

  private JsonNode optionalObject(McpSchema.CallToolRequest request, String name) {
    return objectNode(argument(request, name), name);
  }

  private JsonNode objectNode(Object value, String name) {
    if (value == null) {
      return null;
    }
    JsonNode node = objectMapper.valueToTree(value);
    if (!node.isObject()) {
      throw new IllegalArgumentException(name + " must be a JSON object");
    }
    return node;
  }

  private Object argument(McpSchema.CallToolRequest request, String name) {
    return request.arguments() == null ? null : request.arguments().get(name);
  }

  private String stringArgument(McpSchema.CallToolRequest request, String name) {
    Object value = argument(request, name);
    return value instanceof String text ? text : null;
  }

  private static Map<String, Object> strings(String... values) {
    Map<String, Object> properties = new LinkedHashMap<>();
    for (int index = 0; index < values.length; index += 2) {
      properties.put(values[index], Map.of("type", "string", "description", values[index + 1]));
    }
    return properties;
  }

  private static Map<String, Object> projectProperties() {
    Map<String, Object> properties = strings("workspaceId", "Workspace ID.", "name", "Project name.",
        "description", "Optional description.");
    properties.put("dbVendorId", Map.of("type", "integer", "minimum", 1, "description", "Database vendor ID."));
    return properties;
  }

  private static Map<String, Object> tableProperties() {
    Map<String, Object> properties = strings("schemaId", "Schema ID.", "name", "Table name.",
        "charset", "Optional charset.", "collation", "Optional collation.");
    properties.put("extra", Map.of("type", "object", "description", "Optional table metadata."));
    return properties;
  }

  private static Map<String, Object> columnProperties() {
    Map<String, Object> properties = strings("tableId", "Table ID.", "name", "Column name.",
        "dataType", "SQL data type.", "charset", "Optional charset.", "collation", "Optional collation.",
        "comment", "Optional comment.");
    properties.put("length", Map.of("type", "integer", "description", "Optional type length."));
    properties.put("precision", Map.of("type", "integer", "description", "Optional type precision."));
    properties.put("scale", Map.of("type", "integer", "description", "Optional type scale."));
    properties.put("autoIncrement", Map.of("type", "boolean", "default", false, "description", "Auto increment."));
    properties.put("values", Map.of("type", "array", "items", Map.of("type", "string"),
        "description", "Optional enum/set values."));
    return properties;
  }

  private static Map<String, Object> createMemoProperties() {
    Map<String, Object> properties = strings("schemaId", "Schema ID.", "body", "Initial memo body.");
    properties.put("positions", Map.of("type", "object", "description", "Memo positions metadata."));
    return properties;
  }

  private static Map<String, Object> moveMemoProperties() {
    Map<String, Object> properties = strings("memoId", "Memo ID.");
    properties.put("positions", Map.of("type", "object", "description", "Memo positions metadata."));
    return properties;
  }

  private record Actor(McpAuthenticatedPrincipal principal, String userId) {
  }

}
