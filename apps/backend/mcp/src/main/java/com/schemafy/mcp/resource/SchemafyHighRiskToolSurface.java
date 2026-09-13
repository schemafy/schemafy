package com.schemafy.mcp.resource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.core.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.core.erd.constraint.application.port.in.DeleteConstraintCommand;
import com.schemafy.core.erd.constraint.application.port.in.DeleteConstraintUseCase;
import com.schemafy.core.erd.index.application.port.in.DeleteIndexCommand;
import com.schemafy.core.erd.index.application.port.in.DeleteIndexUseCase;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommand;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommentCommand;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommentUseCase;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoUseCase;
import com.schemafy.core.erd.operation.application.inverse.StructuralOperationInverse;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationUseCase;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationUseCase;
import com.schemafy.core.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.DeleteRelationshipUseCase;
import com.schemafy.core.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.DeleteSchemaUseCase;
import com.schemafy.core.erd.sync.ErdStateSyncPublisher;
import com.schemafy.core.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.core.erd.table.application.port.in.DeleteTableUseCase;
import com.schemafy.core.mcp.domain.McpScope;
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationUseCase;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationUseCase;
import com.schemafy.core.project.application.port.in.CreateShareLinkCommand;
import com.schemafy.core.project.application.port.in.CreateShareLinkUseCase;
import com.schemafy.core.project.application.port.in.CreateWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.in.DeleteProjectCommand;
import com.schemafy.core.project.application.port.in.DeleteProjectUseCase;
import com.schemafy.core.project.application.port.in.DeleteShareLinkCommand;
import com.schemafy.core.project.application.port.in.DeleteShareLinkUseCase;
import com.schemafy.core.project.application.port.in.DeleteWorkspaceCommand;
import com.schemafy.core.project.application.port.in.DeleteWorkspaceUseCase;
import com.schemafy.core.project.application.port.in.GetShareLinkQuery;
import com.schemafy.core.project.application.port.in.GetShareLinkUseCase;
import com.schemafy.core.project.application.port.in.GetShareLinksQuery;
import com.schemafy.core.project.application.port.in.GetShareLinksUseCase;
import com.schemafy.core.project.application.port.in.RejectProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.RejectProjectInvitationUseCase;
import com.schemafy.core.project.application.port.in.RejectWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.RejectWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.in.RemoveProjectMemberCommand;
import com.schemafy.core.project.application.port.in.RemoveProjectMemberUseCase;
import com.schemafy.core.project.application.port.in.RemoveWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.in.RemoveWorkspaceMemberUseCase;
import com.schemafy.core.project.application.port.in.RevokeShareLinkCommand;
import com.schemafy.core.project.application.port.in.RevokeShareLinkUseCase;
import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleUseCase;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleCommand;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleUseCase;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.mcp.common.McpResponseWriter;
import com.schemafy.mcp.common.McpWriteExecutor;
import com.schemafy.mcp.common.security.McpAuthenticatedPrincipal;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.mcp.resource.SchemafyMcpFeatureFactory.tool;
import static com.schemafy.mcp.resource.SchemafyMcpFeatureFactory.updateTool;

@Component
@RequiredArgsConstructor
final class SchemafyHighRiskToolSurface {

  private final McpWriteExecutor writeExecutor;
  private final McpResponseWriter responseWriter;
  private final ObjectProvider<ErdStateSyncPublisher> publisherProvider;
  private final CreateWorkspaceInvitationUseCase createWorkspaceInvitationUseCase;
  private final CreateProjectInvitationUseCase createProjectInvitationUseCase;
  private final AcceptWorkspaceInvitationUseCase acceptWorkspaceInvitationUseCase;
  private final AcceptProjectInvitationUseCase acceptProjectInvitationUseCase;
  private final RejectWorkspaceInvitationUseCase rejectWorkspaceInvitationUseCase;
  private final RejectProjectInvitationUseCase rejectProjectInvitationUseCase;
  private final UpdateWorkspaceMemberRoleUseCase updateWorkspaceMemberRoleUseCase;
  private final UpdateProjectMemberRoleUseCase updateProjectMemberRoleUseCase;
  private final RemoveWorkspaceMemberUseCase removeWorkspaceMemberUseCase;
  private final RemoveProjectMemberUseCase removeProjectMemberUseCase;
  private final CreateShareLinkUseCase createShareLinkUseCase;
  private final GetShareLinksUseCase getShareLinksUseCase;
  private final GetShareLinkUseCase getShareLinkUseCase;
  private final RevokeShareLinkUseCase revokeShareLinkUseCase;
  private final DeleteShareLinkUseCase deleteShareLinkUseCase;
  private final DeleteWorkspaceUseCase deleteWorkspaceUseCase;
  private final DeleteProjectUseCase deleteProjectUseCase;
  private final DeleteSchemaUseCase deleteSchemaUseCase;
  private final DeleteTableUseCase deleteTableUseCase;
  private final DeleteColumnUseCase deleteColumnUseCase;
  private final DeleteIndexUseCase deleteIndexUseCase;
  private final DeleteConstraintUseCase deleteConstraintUseCase;
  private final DeleteRelationshipUseCase deleteRelationshipUseCase;
  private final DeleteMemoUseCase deleteMemoUseCase;
  private final DeleteMemoCommentUseCase deleteMemoCommentUseCase;
  private final UndoErdOperationUseCase undoErdOperationUseCase;
  private final RedoErdOperationUseCase redoErdOperationUseCase;

  @Value("${app.base-url:http://localhost:8080}")
  private String baseUrl;

  @Value("${app.api-version:v1}")
  private String apiVersion;

  List<McpServerFeatures.AsyncToolSpecification> specifications() {
    return List.of(
        updateTool("schemafy_create_workspace_invitation", "Create workspace invitation",
            "Invite a user to a workspace.", confirmed(strings("workspaceId", "Workspace ID.",
                "email", "Invitee email.", "role", "Workspace role.")),
            List.of("workspaceId", "email", "role", "confirmed"), this::createWorkspaceInvitation),
        updateTool("schemafy_create_project_invitation", "Create project invitation",
            "Invite a user to a project.", confirmed(strings("projectId", "Project ID.",
                "email", "Invitee email.", "role", "Project role.")),
            List.of("projectId", "email", "role", "confirmed"), this::createProjectInvitation),
        updateTool("schemafy_accept_workspace_invitation", "Accept workspace invitation",
            "Accept an invitation addressed to the authenticated user.", confirmed(ids("invitationId")),
            List.of("invitationId", "confirmed"), this::acceptWorkspaceInvitation),
        updateTool("schemafy_accept_project_invitation", "Accept project invitation",
            "Accept an invitation addressed to the authenticated user.", confirmed(ids("invitationId")),
            List.of("invitationId", "confirmed"), this::acceptProjectInvitation),
        updateTool("schemafy_reject_workspace_invitation", "Reject workspace invitation",
            "Reject an invitation addressed to the authenticated user.", confirmed(ids("invitationId")),
            List.of("invitationId", "confirmed"), this::rejectWorkspaceInvitation),
        updateTool("schemafy_reject_project_invitation", "Reject project invitation",
            "Reject an invitation addressed to the authenticated user.", confirmed(ids("invitationId")),
            List.of("invitationId", "confirmed"), this::rejectProjectInvitation),
        updateTool("schemafy_update_workspace_member_role", "Update workspace member role",
            "Change a workspace member role.", confirmed(roleIds("workspaceId", "targetUserId", "role")),
            List.of("workspaceId", "targetUserId", "role", "confirmed"), this::updateWorkspaceMemberRole),
        updateTool("schemafy_update_project_member_role", "Update project member role",
            "Change a project member role.", confirmed(roleIds("projectId", "targetUserId", "role")),
            List.of("projectId", "targetUserId", "role", "confirmed"), this::updateProjectMemberRole),
        updateTool("schemafy_remove_workspace_member", "Remove workspace member",
            "Remove a workspace member.", confirmed(ids("workspaceId", "targetUserId")),
            List.of("workspaceId", "targetUserId", "confirmed"), this::removeWorkspaceMember),
        updateTool("schemafy_remove_project_member", "Remove project member",
            "Remove a project member.", confirmed(ids("projectId", "targetUserId")),
            List.of("projectId", "targetUserId", "confirmed"), this::removeProjectMember),
        tool("schemafy_list_share_links", "List share links", "List share links without exposing their codes or URLs.",
            Map.of("projectId", stringProperty("Project ID."), "page", integerProperty(0), "size", integerProperty(
                100)),
            List.of("projectId"), this::listShareLinks),
        tool("schemafy_get_share_link", "Get share link", "Get share-link metadata without exposing its code or URL.",
            ids("projectId", "shareLinkId"), List.of("projectId", "shareLinkId"), this::getShareLink),
        updateTool("schemafy_create_share_link", "Create share link",
            "Create a share link. The raw public URL is returned only by this successful operation.",
            confirmed(ids("projectId")), List.of("projectId", "confirmed"), this::createShareLink),
        updateTool("schemafy_revoke_share_link", "Revoke share link", "Revoke a share link.",
            confirmed(ids("projectId", "shareLinkId")), List.of("projectId", "shareLinkId", "confirmed"),
            this::revokeShareLink),
        updateTool("schemafy_delete_share_link", "Delete share link", "Delete a share link.",
            confirmed(ids("projectId", "shareLinkId")), List.of("projectId", "shareLinkId", "confirmed"),
            this::deleteShareLink),
        updateTool("schemafy_delete_workspace", "Delete workspace", "Delete a workspace and its projects.",
            confirmed(ids("workspaceId")), List.of("workspaceId", "confirmed"), this::deleteWorkspace),
        updateTool("schemafy_delete_project", "Delete project", "Delete a project and its ERD data.",
            confirmed(ids("projectId")), List.of("projectId", "confirmed"), this::deleteProject),
        updateTool("schemafy_delete_schema", "Delete schema", "Delete a schema and its ERD data.",
            confirmed(ids("schemaId")), List.of("schemaId", "confirmed"), this::deleteSchema),
        updateTool("schemafy_delete_table", "Delete table", "Delete a table and dependent ERD data.",
            confirmed(ids("tableId")), List.of("tableId", "confirmed"), this::deleteTable),
        updateTool("schemafy_delete_column", "Delete column", "Delete a column.",
            confirmed(ids("columnId")), List.of("columnId", "confirmed"), this::deleteColumn),
        updateTool("schemafy_delete_index", "Delete index", "Delete an index.",
            confirmed(ids("indexId")), List.of("indexId", "confirmed"), this::deleteIndex),
        updateTool("schemafy_delete_constraint", "Delete constraint", "Delete a constraint.",
            confirmed(ids("constraintId")), List.of("constraintId", "confirmed"), this::deleteConstraint),
        updateTool("schemafy_delete_relationship", "Delete relationship", "Delete a relationship.",
            confirmed(ids("relationshipId")), List.of("relationshipId", "confirmed"), this::deleteRelationship),
        updateTool("schemafy_delete_memo", "Delete memo", "Delete a memo.",
            confirmed(ids("memoId")), List.of("memoId", "confirmed"), this::deleteMemo),
        updateTool("schemafy_delete_memo_comment", "Delete memo comment", "Delete a memo comment.",
            confirmed(ids("commentId")), List.of("commentId", "confirmed"), this::deleteMemoComment),
        updateTool("schemafy_undo_erd_operation", "Undo ERD operation", "Undo an ERD operation.",
            confirmed(ids("opId")), List.of("opId", "confirmed"), this::undo),
        updateTool("schemafy_redo_erd_operation", "Redo ERD operation", "Redo an ERD operation.",
            confirmed(ids("opId")), List.of("opId", "confirmed"), this::redo));
  }

  private Mono<McpSchema.CallToolResult> createWorkspaceInvitation(McpSchema.CallToolRequest request) {
    return execute(request, "schemafy_create_workspace_invitation", McpScope.INVITATION_WRITE, "workspaceId",
        actor -> createWorkspaceInvitationUseCase.createWorkspaceInvitation(new CreateWorkspaceInvitationCommand(
            required(request, "workspaceId"), required(request, "email"), WorkspaceRole.fromString(required(request,
                "role")), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> createProjectInvitation(McpSchema.CallToolRequest request) {
    return execute(request, "schemafy_create_project_invitation", McpScope.INVITATION_WRITE, "projectId",
        actor -> createProjectInvitationUseCase.createProjectInvitation(new CreateProjectInvitationCommand(
            required(request, "projectId"), required(request, "email"), ProjectRole.fromString(required(request,
                "role")), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> acceptWorkspaceInvitation(McpSchema.CallToolRequest request) {
    return execute(request, "schemafy_accept_workspace_invitation", McpScope.INVITATION_WRITE, "invitationId",
        actor -> acceptWorkspaceInvitationUseCase.acceptWorkspaceInvitation(new AcceptWorkspaceInvitationCommand(
            required(request, "invitationId"), actor.userId())).map(member -> Map.of("member", member)));
  }

  private Mono<McpSchema.CallToolResult> acceptProjectInvitation(McpSchema.CallToolRequest request) {
    return execute(request, "schemafy_accept_project_invitation", McpScope.INVITATION_WRITE, "invitationId",
        actor -> acceptProjectInvitationUseCase.acceptProjectInvitation(new AcceptProjectInvitationCommand(
            required(request, "invitationId"), actor.userId())).map(member -> Map.of("member", member)));
  }

  private Mono<McpSchema.CallToolResult> rejectWorkspaceInvitation(McpSchema.CallToolRequest request) {
    return executeVoid(request, "schemafy_reject_workspace_invitation", McpScope.INVITATION_WRITE, "invitationId",
        actor -> rejectWorkspaceInvitationUseCase.rejectWorkspaceInvitation(new RejectWorkspaceInvitationCommand(
            required(request, "invitationId"), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> rejectProjectInvitation(McpSchema.CallToolRequest request) {
    return executeVoid(request, "schemafy_reject_project_invitation", McpScope.INVITATION_WRITE, "invitationId",
        actor -> rejectProjectInvitationUseCase.rejectProjectInvitation(new RejectProjectInvitationCommand(
            required(request, "invitationId"), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> updateWorkspaceMemberRole(McpSchema.CallToolRequest request) {
    return execute(request, "schemafy_update_workspace_member_role", McpScope.MEMBERSHIP_ADMIN, "workspaceId",
        actor -> updateWorkspaceMemberRoleUseCase.updateWorkspaceMemberRole(new UpdateWorkspaceMemberRoleCommand(
            required(request, "workspaceId"), required(request, "targetUserId"), WorkspaceRole.fromString(required(
                request, "role")), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> updateProjectMemberRole(McpSchema.CallToolRequest request) {
    return execute(request, "schemafy_update_project_member_role", McpScope.MEMBERSHIP_ADMIN, "projectId",
        actor -> updateProjectMemberRoleUseCase.updateProjectMemberRole(new UpdateProjectMemberRoleCommand(
            required(request, "projectId"), required(request, "targetUserId"), ProjectRole.fromString(required(
                request, "role")), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> removeWorkspaceMember(McpSchema.CallToolRequest request) {
    return executeVoid(request, "schemafy_remove_workspace_member", McpScope.MEMBERSHIP_ADMIN, "workspaceId",
        actor -> removeWorkspaceMemberUseCase.removeWorkspaceMember(new RemoveWorkspaceMemberCommand(
            required(request, "workspaceId"), required(request, "targetUserId"), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> removeProjectMember(McpSchema.CallToolRequest request) {
    return executeVoid(request, "schemafy_remove_project_member", McpScope.MEMBERSHIP_ADMIN, "projectId",
        actor -> removeProjectMemberUseCase.removeProjectMember(new RemoveProjectMemberCommand(
            required(request, "projectId"), required(request, "targetUserId"), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> listShareLinks(McpSchema.CallToolRequest request) {
    return readScope(request, "schemafy_list_share_links", McpScope.SHARE_LINK_READ, actor -> responseWriter
        .toolPayload(
            getShareLinksUseCase.getShareLinks(new GetShareLinksQuery(required(request, "projectId"), actor.userId(),
                integer(request, "page", 0), integer(request, "size", 100))).map(result -> result.map(
                    this::redactedShareLink))));
  }

  private Mono<McpSchema.CallToolResult> getShareLink(McpSchema.CallToolRequest request) {
    return readScope(request, "schemafy_get_share_link", McpScope.SHARE_LINK_READ, actor -> responseWriter.toolPayload(
        getShareLinkUseCase.getShareLink(new GetShareLinkQuery(required(request, "projectId"), required(request,
            "shareLinkId"), actor.userId())).map(this::redactedShareLink)));
  }

  private Mono<McpSchema.CallToolResult> createShareLink(McpSchema.CallToolRequest request) {
    return execute(request, "schemafy_create_share_link", McpScope.SHARE_LINK_WRITE, "projectId",
        actor -> createShareLinkUseCase.createShareLink(new CreateShareLinkCommand(required(request, "projectId"), actor
            .userId()))
            .map(link -> rawShareLink(link)));
  }

  private Mono<McpSchema.CallToolResult> revokeShareLink(McpSchema.CallToolRequest request) {
    return execute(request, "schemafy_revoke_share_link", McpScope.SHARE_LINK_WRITE, "shareLinkId",
        actor -> revokeShareLinkUseCase.revokeShareLink(new RevokeShareLinkCommand(required(request, "projectId"),
            required(
                request, "shareLinkId"), actor.userId())).map(this::redactedShareLink));
  }

  private Mono<McpSchema.CallToolResult> deleteShareLink(McpSchema.CallToolRequest request) {
    return executeVoid(request, "schemafy_delete_share_link", McpScope.SHARE_LINK_WRITE, "shareLinkId",
        actor -> deleteShareLinkUseCase.deleteShareLink(new DeleteShareLinkCommand(required(request, "projectId"),
            required(
                request, "shareLinkId"), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> deleteWorkspace(McpSchema.CallToolRequest request) {
    return executeVoid(request, "schemafy_delete_workspace", McpScope.WORKSPACE_WRITE, "workspaceId",
        actor -> deleteWorkspaceUseCase.deleteWorkspace(new DeleteWorkspaceCommand(required(request, "workspaceId"),
            actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> deleteProject(McpSchema.CallToolRequest request) {
    return executeVoid(request, "schemafy_delete_project", McpScope.WORKSPACE_WRITE, "projectId",
        actor -> deleteProjectUseCase.deleteProject(new DeleteProjectCommand(required(request, "projectId"), actor
            .userId())));
  }

  private Mono<McpSchema.CallToolResult> deleteSchema(McpSchema.CallToolRequest request) {
    return executeMutation(request, "schemafy_delete_schema", "schemaId", actor -> {
      String schemaId = required(request, "schemaId");
      ErdStateSyncPublisher publisher = publisherProvider.getIfAvailable();
      Mono<MutationResult<Void>> deletion = deleteSchemaUseCase.deleteSchema(
          new DeleteSchemaCommand(schemaId));
      if (publisher == null) {
        return deletion.map(this::mutation);
      }
      return publisher.resolveFromSchemaId(schemaId)
          .flatMap(context -> deletion.flatMap(result -> publisher
              .publishDeletedWithContext(context, result.affectedTableIds(), result.operation())
              .thenReturn(mutation(result))));
    });
  }

  private Mono<McpSchema.CallToolResult> deleteTable(McpSchema.CallToolRequest request) {
    return executeMutation(request, "schemafy_delete_table", "tableId", actor -> deleteTableUseCase.deleteTable(
        new DeleteTableCommand(required(request, "tableId")))
        .flatMap(result -> publishActive(result).thenReturn(mutation(result))));
  }

  private Mono<McpSchema.CallToolResult> deleteColumn(McpSchema.CallToolRequest request) {
    return executeMutation(request, "schemafy_delete_column", "columnId", actor -> deleteColumnUseCase.deleteColumn(
        new DeleteColumnCommand(required(request, "columnId")))
        .flatMap(result -> publishActive(result).thenReturn(mutation(result))));
  }

  private Mono<McpSchema.CallToolResult> deleteIndex(McpSchema.CallToolRequest request) {
    return executeMutation(request, "schemafy_delete_index", "indexId", actor -> deleteIndexUseCase.deleteIndex(
        new DeleteIndexCommand(required(request, "indexId")))
        .flatMap(result -> publishActive(result).thenReturn(mutation(result))));
  }

  private Mono<McpSchema.CallToolResult> deleteConstraint(McpSchema.CallToolRequest request) {
    return executeMutation(request, "schemafy_delete_constraint", "constraintId", actor -> deleteConstraintUseCase
        .deleteConstraint(new DeleteConstraintCommand(required(request, "constraintId")))
        .flatMap(result -> publishActive(result).thenReturn(mutation(result))));
  }

  private Mono<McpSchema.CallToolResult> deleteRelationship(McpSchema.CallToolRequest request) {
    return executeMutation(request, "schemafy_delete_relationship", "relationshipId", actor -> deleteRelationshipUseCase
        .deleteRelationship(new DeleteRelationshipCommand(required(request, "relationshipId")))
        .flatMap(result -> publishActive(result).thenReturn(mutation(result))));
  }

  private Mono<McpSchema.CallToolResult> deleteMemo(McpSchema.CallToolRequest request) {
    return executeVoid(request, "schemafy_delete_memo", McpScope.MEMO_WRITE, "memoId", actor -> deleteMemoUseCase
        .deleteMemo(new DeleteMemoCommand(required(request, "memoId"), actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> deleteMemoComment(McpSchema.CallToolRequest request) {
    return executeVoid(request, "schemafy_delete_memo_comment", McpScope.MEMO_WRITE, "commentId",
        actor -> deleteMemoCommentUseCase.deleteMemoComment(new DeleteMemoCommentCommand(required(request, "commentId"),
            actor.userId())));
  }

  private Mono<McpSchema.CallToolResult> undo(McpSchema.CallToolRequest request) {
    return executeMutation(request, "schemafy_undo_erd_operation", "opId", actor -> undoErdOperationUseCase.undo(
        new UndoErdOperationCommand(required(request, "opId")))
        .flatMap(this::publishUndoRedo).map(this::mutation));
  }

  private Mono<McpSchema.CallToolResult> redo(McpSchema.CallToolRequest request) {
    return executeMutation(request, "schemafy_redo_erd_operation", "opId", actor -> redoErdOperationUseCase.redo(
        new RedoErdOperationCommand(required(request, "opId")))
        .flatMap(this::publishUndoRedo).map(this::mutation));
  }

  private Mono<McpSchema.CallToolResult> execute(
      McpSchema.CallToolRequest request, String tool, McpScope scope, String target,
      Function<McpAuthenticatedPrincipal, Mono<?>> action) {
    return writeExecutor.execute(request, tool, scope, confirmed(request), string(request, target), action);
  }

  private Mono<McpSchema.CallToolResult> executeVoid(
      McpSchema.CallToolRequest request, String tool, McpScope scope, String target,
      Function<McpAuthenticatedPrincipal, Mono<Void>> action) {
    return execute(request, tool, scope, target, actor -> action.apply(actor)
        .thenReturn(Map.of("ok", true)));
  }

  private Mono<McpSchema.CallToolResult> executeMutation(
      McpSchema.CallToolRequest request, String tool, String target,
      Function<McpAuthenticatedPrincipal, Mono<Map<String, Object>>> action) {
    return execute(request, tool, McpScope.ERD_WRITE, target, action::apply);
  }

  private Mono<McpSchema.CallToolResult> readScope(
      McpSchema.CallToolRequest request, String tool, McpScope scope,
      Function<McpAuthenticatedPrincipal, Mono<McpSchema.CallToolResult>> action) {
    return writeExecutor.principal().flatMap(actor -> actor.scopes().contains(scope.value())
        ? action.apply(actor)
        : Mono.just(responseWriter.toolError("MCP token scope is insufficient")));
  }

  private Mono<Void> publishActive(MutationResult<?> result) {
    ErdStateSyncPublisher publisher = publisherProvider.getIfAvailable();
    return publisher == null || result.affectedTableIds().isEmpty() ? Mono.empty()
        : publisher.publishMutation(result.affectedTableIds(), result.operation());
  }

  private Mono<MutationResult<Void>> publishUndoRedo(MutationResult<Void> result) {
    ErdStateSyncPublisher publisher = publisherProvider.getIfAvailable();
    if (publisher == null || result.operation() == null) {
      return Mono.just(result);
    }
    if (result.inversePayload() instanceof StructuralOperationInverse inverse) {
      return publisher.publishSchemaMutation(inverse.schemaId(), result.affectedTableIds(), result.operation())
          .thenReturn(result);
    }
    return publisher.publishMutation(result.affectedTableIds(), result.operation()).thenReturn(result);
  }

  private Map<String, Object> mutation(MutationResult<?> result) {
    return Map.of("result", result.result() == null ? Map.of() : result.result(),
        "affectedTableIds", result.affectedTableIds(), "operation", result.operation() == null ? Map.of()
            : result.operation(),
        "noOp", result.noOp());
  }

  private Map<String, Object> rawShareLink(ShareLink link) {
    return Map.of("id", link.getId(), "projectId", link.getProjectId(), "code", link.getCode(),
        "publicUrl", publicUrl(link.getCode()), "expiresAt", link.getExpiresAt(), "isRevoked", link.getIsRevoked());
  }

  private Map<String, Object> redactedShareLink(ShareLink link) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", link.getId());
    result.put("projectId", link.getProjectId());
    result.put("expiresAt", link.getExpiresAt());
    result.put("isRevoked", link.getIsRevoked());
    result.put("lastAccessedAt", link.getLastAccessedAt());
    result.put("accessCount", link.getAccessCount());
    result.put("createdAt", link.getCreatedAt());
    return result;
  }

  private String publicUrl(String code) {
    String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    return normalized + "/public/api/" + apiVersion + "/share/" + code;
  }

  private boolean confirmed(McpSchema.CallToolRequest request) {
    return Boolean.TRUE.equals(request.arguments() == null ? null : request.arguments().get("confirmed"));
  }

  private String required(McpSchema.CallToolRequest request, String name) {
    String value = string(request, name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required");
    }
    return value;
  }

  private String string(McpSchema.CallToolRequest request, String name) {
    Object value = request.arguments() == null ? null : request.arguments().get(name);
    return value instanceof String text ? text : null;
  }

  private int integer(McpSchema.CallToolRequest request, String name, int defaultValue) {
    Object value = request.arguments() == null ? null : request.arguments().get(name);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Number number && number.intValue() >= 0) {
      return number.intValue();
    }
    throw new IllegalArgumentException(name + " must be a non-negative integer");
  }

  private static Map<String, Object> ids(String... names) {
    Map<String, Object> properties = new LinkedHashMap<>();
    for (String name : names) {
      properties.put(name, stringProperty(name + " ID."));
    }
    return properties;
  }

  private static Map<String, Object> roleIds(String first, String second, String role) {
    Map<String, Object> properties = new LinkedHashMap<>(ids(first, second));
    properties.put(role, stringProperty("Role name."));
    return properties;
  }

  private static Map<String, Object> strings(String... values) {
    Map<String, Object> properties = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      properties.put(values[i], stringProperty(values[i + 1]));
    }
    return properties;
  }

  private static Map<String, Object> confirmed(Map<String, Object> properties) {
    properties.put("confirmed", Map.of("type", "boolean", "const", true,
        "description", "Must be true to confirm this high-risk operation."));
    return properties;
  }

  private static Map<String, Object> stringProperty(String description) {
    return Map.of("type", "string", "description", description);
  }

  private static Map<String, Object> integerProperty(int defaultValue) {
    return Map.of("type", "integer", "minimum", 0, "default", defaultValue);
  }

}
