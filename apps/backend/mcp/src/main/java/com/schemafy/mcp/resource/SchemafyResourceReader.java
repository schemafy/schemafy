package com.schemafy.mcp.resource;

import java.util.Map;
import java.util.function.Function;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;

import com.schemafy.core.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.core.erd.column.application.port.in.GetColumnsByTableIdUseCase;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintsByTableIdQuery;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintsByTableIdUseCase;
import com.schemafy.core.erd.index.application.port.in.GetIndexesByTableIdQuery;
import com.schemafy.core.erd.index.application.port.in.GetIndexesByTableIdUseCase;
import com.schemafy.core.erd.memo.application.port.in.GetMemoCommentsQuery;
import com.schemafy.core.erd.memo.application.port.in.GetMemoCommentsUseCase;
import com.schemafy.core.erd.memo.application.port.in.GetMemoQuery;
import com.schemafy.core.erd.memo.application.port.in.GetMemoUseCase;
import com.schemafy.core.erd.memo.application.port.in.GetMemosBySchemaIdQuery;
import com.schemafy.core.erd.memo.application.port.in.GetMemosBySchemaIdUseCase;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipsByTableIdQuery;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipsByTableIdUseCase;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaUseCase;
import com.schemafy.core.erd.schema.application.port.in.GetSchemasByProjectIdQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemasByProjectIdUseCase;
import com.schemafy.core.erd.table.application.port.in.GetTableQuery;
import com.schemafy.core.erd.table.application.port.in.GetTableUseCase;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdUseCase;
import com.schemafy.core.erd.vendor.application.port.in.ListDbVendorsUseCase;
import com.schemafy.core.project.application.port.in.GetMySharedProjectsQuery;
import com.schemafy.core.project.application.port.in.GetMySharedProjectsUseCase;
import com.schemafy.core.project.application.port.in.GetProjectMembersQuery;
import com.schemafy.core.project.application.port.in.GetProjectMembersUseCase;
import com.schemafy.core.project.application.port.in.GetProjectQuery;
import com.schemafy.core.project.application.port.in.GetProjectUseCase;
import com.schemafy.core.project.application.port.in.GetProjectsQuery;
import com.schemafy.core.project.application.port.in.GetProjectsUseCase;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersQuery;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersUseCase;
import com.schemafy.core.project.application.port.in.GetWorkspaceQuery;
import com.schemafy.core.project.application.port.in.GetWorkspaceUseCase;
import com.schemafy.core.project.application.port.in.GetWorkspacesQuery;
import com.schemafy.core.project.application.port.in.GetWorkspacesUseCase;
import com.schemafy.mcp.common.McpResponseWriter;
import com.schemafy.mcp.common.security.McpAuthenticatedPrincipal;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.DefaultMcpUriTemplateManagerFactory;
import io.modelcontextprotocol.util.McpUriTemplateManagerFactory;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
final class SchemafyResourceReader {

  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_PAGE_SIZE = 100;

  private final McpResponseWriter responseWriter;
  private final McpUriTemplateManagerFactory uriTemplateManagerFactory = new DefaultMcpUriTemplateManagerFactory();
  private final GetWorkspacesUseCase getWorkspacesUseCase;
  private final GetWorkspaceUseCase getWorkspaceUseCase;
  private final GetWorkspaceMembersUseCase getWorkspaceMembersUseCase;
  private final GetProjectsUseCase getProjectsUseCase;
  private final GetProjectUseCase getProjectUseCase;
  private final GetProjectMembersUseCase getProjectMembersUseCase;
  private final GetMySharedProjectsUseCase getMySharedProjectsUseCase;
  private final ListDbVendorsUseCase listDbVendorsUseCase;
  private final GetSchemasByProjectIdUseCase getSchemasByProjectIdUseCase;
  private final GetSchemaUseCase getSchemaUseCase;
  private final GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;
  private final GetTableUseCase getTableUseCase;
  private final GetColumnsByTableIdUseCase getColumnsByTableIdUseCase;
  private final GetIndexesByTableIdUseCase getIndexesByTableIdUseCase;
  private final GetConstraintsByTableIdUseCase getConstraintsByTableIdUseCase;
  private final GetRelationshipsByTableIdUseCase getRelationshipsByTableIdUseCase;
  private final GetMemosBySchemaIdUseCase getMemosBySchemaIdUseCase;
  private final GetMemoUseCase getMemoUseCase;
  private final GetMemoCommentsUseCase getMemoCommentsUseCase;

  Mono<McpSchema.ReadResourceResult> me(McpSchema.ReadResourceRequest request) {
    return responseWriter.resourcePayload(request.uri(), currentPrincipal()
        .map(principal -> Map.of(
            "userId", principal.userId(),
            "scopes", principal.scopes(),
            "tokenId", principal.tokenId())));
  }

  Mono<McpSchema.ReadResourceResult> vendors(McpSchema.ReadResourceRequest request) {
    return responseWriter.resourcePayload(request.uri(), listDbVendorsUseCase.listDbVendors()
        .collectList());
  }

  Mono<McpSchema.ReadResourceResult> workspaces(McpSchema.ReadResourceRequest request) {
    return responseWriter.resourcePayload(request.uri(), currentPrincipal()
        .flatMap(principal -> getWorkspacesUseCase.getWorkspaces(
            new GetWorkspacesQuery(principal.userId(), DEFAULT_PAGE, DEFAULT_PAGE_SIZE))));
  }

  Mono<McpSchema.ReadResourceResult> sharedProjects(McpSchema.ReadResourceRequest request) {
    return responseWriter.resourcePayload(request.uri(), currentPrincipal()
        .flatMap(principal -> getMySharedProjectsUseCase.getMySharedProjects(
            new GetMySharedProjectsQuery(principal.userId(), DEFAULT_PAGE, DEFAULT_PAGE_SIZE))));
  }

  Mono<McpSchema.ReadResourceResult> workspace(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(), currentPrincipal()
        .flatMap(principal -> getWorkspaceUseCase.getWorkspace(
            new GetWorkspaceQuery(variable(request, template, "workspaceId"), principal.userId()))));
  }

  Mono<McpSchema.ReadResourceResult> workspaceMembers(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(), currentPrincipal()
        .flatMap(principal -> getWorkspaceMembersUseCase.getWorkspaceMembers(
            new GetWorkspaceMembersQuery(variable(request, template, "workspaceId"), principal.userId(),
                DEFAULT_PAGE, DEFAULT_PAGE_SIZE))));
  }

  Mono<McpSchema.ReadResourceResult> workspaceProjects(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(), currentPrincipal()
        .flatMap(principal -> getProjectsUseCase.getProjects(
            new GetProjectsQuery(variable(request, template, "workspaceId"), principal.userId(),
                DEFAULT_PAGE, DEFAULT_PAGE_SIZE))));
  }

  Mono<McpSchema.ReadResourceResult> project(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(), currentPrincipal()
        .flatMap(principal -> getProjectUseCase.getProject(
            new GetProjectQuery(variable(request, template, "projectId"), principal.userId()))));
  }

  Mono<McpSchema.ReadResourceResult> projectMembers(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(), currentPrincipal()
        .flatMap(principal -> getProjectMembersUseCase.getProjectMembers(
            new GetProjectMembersQuery(variable(request, template, "projectId"), principal.userId(),
                DEFAULT_PAGE, DEFAULT_PAGE_SIZE))));
  }

  Mono<McpSchema.ReadResourceResult> projectSchemas(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(), getSchemasByProjectIdUseCase
        .getSchemasByProjectId(new GetSchemasByProjectIdQuery(variable(request, template, "projectId")))
        .collectList());
  }

  Mono<McpSchema.ReadResourceResult> schema(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(),
        getSchemaUseCase.getSchema(new GetSchemaQuery(variable(request, template, "schemaId"))));
  }

  Mono<McpSchema.ReadResourceResult> schemaTables(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(), getTablesBySchemaIdUseCase
        .getTablesBySchemaId(new GetTablesBySchemaIdQuery(variable(request, template, "schemaId")))
        .collectList());
  }

  Mono<McpSchema.ReadResourceResult> schemaMemos(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(), getMemosBySchemaIdUseCase
        .getMemosBySchemaId(new GetMemosBySchemaIdQuery(variable(request, template, "schemaId")))
        .collectList());
  }

  Mono<McpSchema.ReadResourceResult> table(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(),
        getTableUseCase.getTable(new GetTableQuery(variable(request, template, "tableId"))));
  }

  Mono<McpSchema.ReadResourceResult> tableColumns(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(), getColumnsByTableIdUseCase
        .getColumnsByTableId(new GetColumnsByTableIdQuery(variable(request, template, "tableId"))));
  }

  Mono<McpSchema.ReadResourceResult> tableIndexes(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(), getIndexesByTableIdUseCase
        .getIndexesByTableId(new GetIndexesByTableIdQuery(variable(request, template, "tableId"))));
  }

  Mono<McpSchema.ReadResourceResult> tableConstraints(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(), getConstraintsByTableIdUseCase
        .getConstraintsByTableId(new GetConstraintsByTableIdQuery(variable(request, template, "tableId"))));
  }

  Mono<McpSchema.ReadResourceResult> tableRelationships(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(), getRelationshipsByTableIdUseCase
        .getRelationshipsByTableId(new GetRelationshipsByTableIdQuery(variable(request, template, "tableId"))));
  }

  Mono<McpSchema.ReadResourceResult> memo(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(),
        getMemoUseCase.getMemo(new GetMemoQuery(variable(request, template, "memoId"))));
  }

  Mono<McpSchema.ReadResourceResult> memoComments(
      McpSchema.ReadResourceRequest request,
      String template) {
    return responseWriter.resourcePayload(request.uri(), getMemoCommentsUseCase
        .getMemoComments(new GetMemoCommentsQuery(variable(request, template, "memoId")))
        .collectList());
  }

  Mono<McpSchema.CallToolResult> databaseVendorsTool() {
    return responseWriter.toolPayload(listDbVendorsUseCase.listDbVendors()
        .collectList());
  }

  Mono<McpSchema.CallToolResult> workspacesTool() {
    return responseWriter.toolPayload(currentPrincipal()
        .flatMap(principal -> getWorkspacesUseCase.getWorkspaces(
            new GetWorkspacesQuery(principal.userId(), DEFAULT_PAGE, DEFAULT_PAGE_SIZE))));
  }

  Mono<McpSchema.CallToolResult> sharedProjectsTool() {
    return responseWriter.toolPayload(currentPrincipal()
        .flatMap(principal -> getMySharedProjectsUseCase.getMySharedProjects(
            new GetMySharedProjectsQuery(principal.userId(), DEFAULT_PAGE, DEFAULT_PAGE_SIZE))));
  }

  Mono<McpSchema.CallToolResult> workspaceTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "workspaceId",
        workspaceId -> responseWriter.toolPayload(currentPrincipal()
            .flatMap(principal -> getWorkspaceUseCase.getWorkspace(
                new GetWorkspaceQuery(workspaceId, principal.userId())))));
  }

  Mono<McpSchema.CallToolResult> workspaceMembersTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "workspaceId",
        workspaceId -> responseWriter.toolPayload(currentPrincipal()
            .flatMap(principal -> getWorkspaceMembersUseCase.getWorkspaceMembers(
                new GetWorkspaceMembersQuery(workspaceId, principal.userId(),
                    DEFAULT_PAGE, DEFAULT_PAGE_SIZE)))));
  }

  Mono<McpSchema.CallToolResult> projectsTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "workspaceId",
        workspaceId -> responseWriter.toolPayload(currentPrincipal()
            .flatMap(principal -> getProjectsUseCase.getProjects(
                new GetProjectsQuery(workspaceId, principal.userId(),
                    DEFAULT_PAGE, DEFAULT_PAGE_SIZE)))));
  }

  Mono<McpSchema.CallToolResult> projectTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "projectId",
        projectId -> responseWriter.toolPayload(currentPrincipal()
            .flatMap(principal -> getProjectUseCase.getProject(
                new GetProjectQuery(projectId, principal.userId())))));
  }

  Mono<McpSchema.CallToolResult> projectMembersTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "projectId",
        projectId -> responseWriter.toolPayload(currentPrincipal()
            .flatMap(principal -> getProjectMembersUseCase.getProjectMembers(
                new GetProjectMembersQuery(projectId, principal.userId(),
                    DEFAULT_PAGE, DEFAULT_PAGE_SIZE)))));
  }

  Mono<McpSchema.CallToolResult> schemasTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "projectId",
        projectId -> responseWriter.toolPayload(getSchemasByProjectIdUseCase
            .getSchemasByProjectId(new GetSchemasByProjectIdQuery(projectId))
            .collectList()));
  }

  Mono<McpSchema.CallToolResult> schemaTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "schemaId",
        schemaId -> responseWriter.toolPayload(getSchemaUseCase.getSchema(
            new GetSchemaQuery(schemaId))));
  }

  Mono<McpSchema.CallToolResult> tablesTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "schemaId",
        schemaId -> responseWriter.toolPayload(getTablesBySchemaIdUseCase
            .getTablesBySchemaId(new GetTablesBySchemaIdQuery(schemaId))
            .collectList()));
  }

  Mono<McpSchema.CallToolResult> schemaMemosTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "schemaId",
        schemaId -> responseWriter.toolPayload(getMemosBySchemaIdUseCase
            .getMemosBySchemaId(new GetMemosBySchemaIdQuery(schemaId))
            .collectList()));
  }

  Mono<McpSchema.CallToolResult> tableTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "tableId",
        tableId -> responseWriter.toolPayload(getTableUseCase.getTable(
            new GetTableQuery(tableId))));
  }

  Mono<McpSchema.CallToolResult> columnsTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "tableId",
        tableId -> responseWriter.toolPayload(getColumnsByTableIdUseCase
            .getColumnsByTableId(new GetColumnsByTableIdQuery(tableId))));
  }

  Mono<McpSchema.CallToolResult> indexesTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "tableId",
        tableId -> responseWriter.toolPayload(getIndexesByTableIdUseCase
            .getIndexesByTableId(new GetIndexesByTableIdQuery(tableId))));
  }

  Mono<McpSchema.CallToolResult> constraintsTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "tableId",
        tableId -> responseWriter.toolPayload(getConstraintsByTableIdUseCase
            .getConstraintsByTableId(new GetConstraintsByTableIdQuery(tableId))));
  }

  Mono<McpSchema.CallToolResult> relationshipsTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "tableId",
        tableId -> responseWriter.toolPayload(getRelationshipsByTableIdUseCase
            .getRelationshipsByTableId(new GetRelationshipsByTableIdQuery(tableId))));
  }

  Mono<McpSchema.CallToolResult> memoTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "memoId",
        memoId -> responseWriter.toolPayload(getMemoUseCase.getMemo(
            new GetMemoQuery(memoId))));
  }

  Mono<McpSchema.CallToolResult> memoCommentsTool(
      McpSchema.CallToolRequest request) {
    return withRequiredArgument(request, "memoId",
        memoId -> responseWriter.toolPayload(getMemoCommentsUseCase
            .getMemoComments(new GetMemoCommentsQuery(memoId))
            .collectList()));
  }

  private Mono<McpAuthenticatedPrincipal> currentPrincipal() {
    return ReactiveSecurityContextHolder.getContext()
        .map(securityContext -> securityContext.getAuthentication().getPrincipal())
        .cast(McpAuthenticatedPrincipal.class)
        .switchIfEmpty(Mono.error(new AccessDeniedException("MCP principal is missing")));
  }

  private String variable(
      McpSchema.ReadResourceRequest request,
      String template,
      String name) {
    return uriTemplateManagerFactory.create(template)
        .extractVariableValues(request.uri())
        .get(name);
  }

  private Mono<McpSchema.CallToolResult> withRequiredArgument(
      McpSchema.CallToolRequest request,
      String name,
      Function<String, Mono<McpSchema.CallToolResult>> handler) {
    try {
      return handler.apply(requiredStringArgument(request, name));
    } catch (IllegalArgumentException e) {
      return Mono.just(responseWriter.toolError(e.getMessage()));
    }
  }

  private String requiredStringArgument(
      McpSchema.CallToolRequest request,
      String name) {
    Object value = request.arguments() == null ? null : request.arguments().get(name);
    if (value instanceof String text && !text.isBlank()) {
      return text;
    }
    throw new IllegalArgumentException(name + " is required");
  }

}
