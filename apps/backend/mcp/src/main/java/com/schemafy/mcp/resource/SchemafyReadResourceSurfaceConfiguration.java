package com.schemafy.mcp.resource;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.modelcontextprotocol.server.McpServerFeatures;

import static com.schemafy.mcp.resource.SchemafyMcpFeatureFactory.resource;
import static com.schemafy.mcp.resource.SchemafyMcpFeatureFactory.template;

@Configuration(proxyBeanMethods = false)
public class SchemafyReadResourceSurfaceConfiguration {

  @Bean
  List<McpServerFeatures.AsyncResourceSpecification> schemafyResourceSpecifications(
      SchemafyResourceReader reader) {
    return List.of(
        resource("schemafy://me", "me", "Authenticated MCP principal", reader::me),
        resource("schemafy://vendors", "vendors", "Supported database vendors", reader::vendors),
        resource("schemafy://workspaces", "workspaces", "Workspaces visible to the MCP principal",
            reader::workspaces),
        resource("schemafy://projects/shared/me", "shared-projects", "Projects shared with the MCP principal",
            reader::sharedProjects));
  }

  @Bean
  List<McpServerFeatures.AsyncResourceTemplateSpecification> schemafyResourceTemplateSpecifications(
      SchemafyResourceReader reader) {
    return List.of(
        template("schemafy://workspaces?page={page}&size={size}", "workspace-pages",
            "Paginated workspaces visible to the MCP principal",
            request -> reader.workspaces(request, "schemafy://workspaces?page={page}&size={size}")),
        template("schemafy://projects/shared/me?page={page}&size={size}", "shared-project-pages",
            "Paginated projects shared with the MCP principal",
            request -> reader.sharedProjects(request,
                "schemafy://projects/shared/me?page={page}&size={size}")),
        template("schemafy://workspaces/{workspaceId}", "workspace", "Workspace detail",
            request -> reader.workspace(request, "schemafy://workspaces/{workspaceId}")),
        template("schemafy://workspaces/{workspaceId}/members", "workspace-members", "Workspace members",
            request -> reader.workspaceMembers(request, "schemafy://workspaces/{workspaceId}/members")),
        template("schemafy://workspaces/{workspaceId}/members?page={page}&size={size}",
            "workspace-member-pages", "Paginated workspace members",
            request -> reader.workspaceMembers(request,
                "schemafy://workspaces/{workspaceId}/members?page={page}&size={size}")),
        template("schemafy://workspaces/{workspaceId}/projects", "workspace-projects", "Workspace projects",
            request -> reader.workspaceProjects(request, "schemafy://workspaces/{workspaceId}/projects")),
        template("schemafy://workspaces/{workspaceId}/projects?page={page}&size={size}",
            "workspace-project-pages", "Paginated workspace projects",
            request -> reader.workspaceProjects(request,
                "schemafy://workspaces/{workspaceId}/projects?page={page}&size={size}")),
        template("schemafy://projects/{projectId}", "project", "Project detail",
            request -> reader.project(request, "schemafy://projects/{projectId}")),
        template("schemafy://projects/{projectId}/members", "project-members", "Project members",
            request -> reader.projectMembers(request, "schemafy://projects/{projectId}/members")),
        template("schemafy://projects/{projectId}/members?page={page}&size={size}",
            "project-member-pages", "Paginated project members",
            request -> reader.projectMembers(request,
                "schemafy://projects/{projectId}/members?page={page}&size={size}")),
        template("schemafy://projects/{projectId}/schemas", "project-schemas", "Project schemas",
            request -> reader.projectSchemas(request, "schemafy://projects/{projectId}/schemas")),
        template("schemafy://schemas/{schemaId}", "schema", "Schema detail",
            request -> reader.schema(request, "schemafy://schemas/{schemaId}")),
        template("schemafy://schemas/{schemaId}/tables", "schema-tables", "Schema tables",
            request -> reader.schemaTables(request, "schemafy://schemas/{schemaId}/tables")),
        template("schemafy://schemas/{schemaId}/memos", "schema-memos", "Schema memos",
            request -> reader.schemaMemos(request, "schemafy://schemas/{schemaId}/memos")),
        template("schemafy://tables/{tableId}", "table", "Table detail",
            request -> reader.table(request, "schemafy://tables/{tableId}")),
        template("schemafy://tables/{tableId}/columns", "table-columns", "Table columns",
            request -> reader.tableColumns(request, "schemafy://tables/{tableId}/columns")),
        template("schemafy://tables/{tableId}/indexes", "table-indexes", "Table indexes",
            request -> reader.tableIndexes(request, "schemafy://tables/{tableId}/indexes")),
        template("schemafy://tables/{tableId}/constraints", "table-constraints", "Table constraints",
            request -> reader.tableConstraints(request, "schemafy://tables/{tableId}/constraints")),
        template("schemafy://tables/{tableId}/relationships", "table-relationships", "Table relationships",
            request -> reader.tableRelationships(request, "schemafy://tables/{tableId}/relationships")),
        template("schemafy://memos/{memoId}", "memo", "Memo detail",
            request -> reader.memo(request, "schemafy://memos/{memoId}")),
        template("schemafy://memos/{memoId}/comments", "memo-comments", "Memo comments",
            request -> reader.memoComments(request, "schemafy://memos/{memoId}/comments")));
  }

}
