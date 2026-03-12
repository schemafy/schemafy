package com.schemafy.core.project.integration;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;

import com.schemafy.core.DomainTestApplication;
import com.schemafy.core.project.adapter.out.persistence.DomainInvitationRepository;
import com.schemafy.core.project.adapter.out.persistence.DomainProjectMemberRepository;
import com.schemafy.core.project.adapter.out.persistence.DomainProjectRepository;
import com.schemafy.core.project.adapter.out.persistence.DomainShareLinkRepository;
import com.schemafy.core.project.adapter.out.persistence.DomainWorkspaceMemberRepository;
import com.schemafy.core.project.adapter.out.persistence.DomainWorkspaceRepository;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.ulid.application.service.UlidGenerator;
import com.schemafy.core.user.application.port.in.SignUpUserCommand;
import com.schemafy.core.user.application.port.in.SignUpUserUseCase;
import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Mono;

@SpringBootTest(classes = DomainTestApplication.class)
@ActiveProfiles("test")
abstract class ProjectDomainIntegrationSupport {

  @Autowired
  protected DatabaseClient databaseClient;

  @Autowired
  protected SignUpUserUseCase signUpUserUseCase;

  @Autowired
  protected DomainWorkspaceRepository workspaceRepository;

  @Autowired
  protected DomainWorkspaceMemberRepository workspaceMemberRepository;

  @Autowired
  protected DomainProjectRepository projectRepository;

  @Autowired
  protected DomainProjectMemberRepository projectMemberRepository;

  @Autowired
  protected DomainInvitationRepository invitationRepository;

  @Autowired
  protected DomainShareLinkRepository shareLinkRepository;

  @BeforeEach
  void cleanProjectDomainTables() {
    Mono.when(
        deleteAll("share_links"),
        deleteAll("invitations"),
        deleteAll("project_members"),
        deleteAll("projects"),
        deleteAll("workspace_members"),
        deleteAll("workspaces"),
        deleteAll("user_auth_providers"),
        deleteAll("users"))
        .block();
  }

  protected User signUpUser(String email, String name) {
    return signUpUserUseCase.signUpUser(new SignUpUserCommand(
        email,
        name,
        "password"))
        .block();
  }

  protected Workspace saveWorkspace(String name, String description) {
    return workspaceRepository.save(Workspace.create(
        UlidGenerator.generate(),
        name,
        description)).block();
  }

  protected WorkspaceMember saveWorkspaceMember(
      Workspace workspace,
      User user,
      WorkspaceRole role) {
    return workspaceMemberRepository.save(WorkspaceMember.create(
        UlidGenerator.generate(),
        workspace.getId(),
        user.id(),
        role)).block();
  }

  protected Project saveProject(Workspace workspace, String name) {
    return saveProject(workspace, name, "description");
  }

  protected Project saveProject(
      Workspace workspace,
      String name,
      String description) {
    return projectRepository.save(Project.create(
        UlidGenerator.generate(),
        workspace.getId(),
        name,
        description)).block();
  }

  protected ProjectMember saveProjectMember(
      Project project,
      User user,
      ProjectRole role) {
    return projectMemberRepository.save(ProjectMember.create(
        UlidGenerator.generate(),
        project.getId(),
        user.id(),
        role)).block();
  }

  protected Invitation saveWorkspaceInvitation(
      Workspace workspace,
      String invitedEmail,
      WorkspaceRole role,
      User invitedBy) {
    return invitationRepository.save(Invitation.createWorkspaceInvitation(
        UlidGenerator.generate(),
        workspace.getId(),
        invitedEmail,
        role,
        invitedBy.id())).block();
  }

  protected Invitation saveProjectInvitation(
      Project project,
      Workspace workspace,
      String invitedEmail,
      ProjectRole role,
      User invitedBy) {
    return invitationRepository.save(Invitation.createProjectInvitation(
        UlidGenerator.generate(),
        project.getId(),
        workspace.getId(),
        invitedEmail,
        role,
        invitedBy.id())).block();
  }

  protected ShareLink saveShareLink(Project project) {
    return saveShareLink(project, Instant.now().plusSeconds(86400));
  }

  protected ShareLink saveShareLink(Project project, Instant expiresAt) {
    return shareLinkRepository.save(ShareLink.create(
        UlidGenerator.generate(),
        project.getId(),
        UUID.randomUUID().toString().replace("-", ""),
        expiresAt)).block();
  }

  protected void softDeleteWorkspaceMember(String memberId) {
    update("UPDATE workspace_members SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id",
        memberId);
  }

  protected void softDeleteProjectMember(String memberId) {
    update("UPDATE project_members SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id",
        memberId);
  }

  protected void revokeShareLink(String shareLinkId) {
    update("UPDATE share_links SET is_revoked = TRUE WHERE id = :id", shareLinkId);
  }

  protected void updateShareLinkExpiration(String shareLinkId, Instant expiresAt) {
    databaseClient.sql("UPDATE share_links SET expires_at = :expiresAt WHERE id = :id")
        .bind("expiresAt", expiresAt)
        .bind("id", shareLinkId)
        .fetch()
        .rowsUpdated()
        .block();
  }

  private Mono<Long> deleteAll(String table) {
    return databaseClient.sql("DELETE FROM " + table)
        .fetch()
        .rowsUpdated();
  }

  private void update(String sql, String id) {
    databaseClient.sql(sql)
        .bind("id", id)
        .fetch()
        .rowsUpdated()
        .block();
  }

}
