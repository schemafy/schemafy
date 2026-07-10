package com.schemafy.mcp.resource;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.modelcontextprotocol.server.McpServerFeatures;

import static com.schemafy.mcp.resource.SchemafyMcpFeatureFactory.idArgument;
import static com.schemafy.mcp.resource.SchemafyMcpFeatureFactory.tool;

@Configuration(proxyBeanMethods = false)
public class SchemafyReadToolSurfaceConfiguration {

  @Bean
  List<McpServerFeatures.AsyncToolSpecification> schemafyToolSpecifications(
      SchemafyResourceReader reader) {
    return List.of(
        tool("schemafy_list_database_vendors", "List database vendors",
            "Use when the user asks which database vendors Schemafy supports or needs a valid dbVendorName for schema creation or ERD interpretation.",
            Map.of(), List.of(), request -> reader.databaseVendorsTool()),
        tool("schemafy_list_workspaces", "List workspaces",
            "Use first when the user asks about their Schemafy workspaces, projects, schemas, ERDs, or permissions and no workspaceId is known.",
            Map.of(), List.of(), request -> reader.workspacesTool()),
        tool("schemafy_list_shared_projects", "List shared projects",
            "Use when the user asks about Schemafy projects shared with them outside their own workspace project lists.",
            Map.of(), List.of(), request -> reader.sharedProjectsTool()),
        tool("schemafy_get_workspace", "Get workspace",
            "Use after a workspaceId is known to inspect one Schemafy workspace, including its description, project count, and current user role.",
            idArgument("workspaceId", "Schemafy workspace ID."),
            List.of("workspaceId"),
            reader::workspaceTool),
        tool("schemafy_list_workspace_members", "List workspace members",
            "Use when the user asks who can access a Schemafy workspace or what workspace roles members have.",
            idArgument("workspaceId", "Schemafy workspace ID."),
            List.of("workspaceId"),
            reader::workspaceMembersTool),
        tool("schemafy_list_projects", "List projects",
            "Use after schemafy_list_workspaces when a workspaceId is known and the user asks for projects or needs the next step toward schemas and ERDs.",
            idArgument("workspaceId", "Schemafy workspace ID."),
            List.of("workspaceId"),
            reader::projectsTool),
        tool("schemafy_get_project", "Get project",
            "Use after a projectId is known to inspect one Schemafy project, including its workspace, description, and current user role.",
            idArgument("projectId", "Schemafy project ID."),
            List.of("projectId"),
            reader::projectTool),
        tool("schemafy_list_project_members", "List project members",
            "Use when the user asks who can access a Schemafy project or what project roles members have.",
            idArgument("projectId", "Schemafy project ID."),
            List.of("projectId"),
            reader::projectMembersTool),
        tool("schemafy_list_schemas", "List schemas",
            "Use after schemafy_list_projects when a projectId is known and the user asks for schemas, ERDs, database models, or schema IDs.",
            idArgument("projectId", "Schemafy project ID."),
            List.of("projectId"),
            reader::schemasTool),
        tool("schemafy_get_schema", "Get schema",
            "Use after a schemaId is known to inspect one Schemafy schema, including database vendor, charset, collation, and revision metadata.",
            idArgument("schemaId", "Schemafy schema ID."),
            List.of("schemaId"),
            reader::schemaTool),
        tool("schemafy_list_tables", "List tables",
            "Use after schemafy_list_schemas when a schemaId is known to enumerate ERD tables before reading columns, indexes, constraints, or relationships.",
            idArgument("schemaId", "Schemafy schema ID."),
            List.of("schemaId"),
            reader::tablesTool),
        tool("schemafy_list_schema_memos", "List schema memos",
            "Use when the user asks for notes, annotations, or memos attached to a Schemafy schema or ERD canvas.",
            idArgument("schemaId", "Schemafy schema ID."),
            List.of("schemaId"),
            reader::schemaMemosTool),
        tool("schemafy_get_table", "Get table",
            "Use after a tableId is known to inspect one ERD table's name, schema, charset, collation, and visual metadata.",
            idArgument("tableId", "Schemafy table ID."),
            List.of("tableId"),
            reader::tableTool),
        tool("schemafy_list_columns", "List columns",
            "Use after schemafy_list_tables when a tableId is known to inspect table columns, data types, type arguments, order, auto-increment flags, and comments.",
            idArgument("tableId", "Schemafy table ID."),
            List.of("tableId"),
            reader::columnsTool),
        tool("schemafy_list_indexes", "List indexes",
            "Use after a tableId is known when the user asks about indexes, lookup performance, or indexed table structures.",
            idArgument("tableId", "Schemafy table ID."),
            List.of("tableId"),
            reader::indexesTool),
        tool("schemafy_list_constraints", "List constraints",
            "Use after a tableId is known when the user asks about primary keys, unique constraints, checks, defaults, not-null rules, or table integrity.",
            idArgument("tableId", "Schemafy table ID."),
            List.of("tableId"),
            reader::constraintsTool),
        tool("schemafy_list_relationships", "List relationships",
            "Use after a tableId is known to discover ERD relationships connected to that table; call it for multiple tables to assemble a relationship graph.",
            idArgument("tableId", "Schemafy table ID."),
            List.of("tableId"),
            reader::relationshipsTool),
        tool("schemafy_get_memo", "Get memo",
            "Use after a memoId is known to inspect one Schemafy ERD memo or annotation.",
            idArgument("memoId", "Schemafy memo ID."),
            List.of("memoId"),
            reader::memoTool),
        tool("schemafy_list_memo_comments", "List memo comments",
            "Use after a memoId is known when the user asks for discussion or comments on a Schemafy ERD memo.",
            idArgument("memoId", "Schemafy memo ID."),
            List.of("memoId"),
            reader::memoCommentsTool));
  }

}
