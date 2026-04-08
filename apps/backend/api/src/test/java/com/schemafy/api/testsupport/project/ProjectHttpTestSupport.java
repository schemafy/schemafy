package com.schemafy.api.testsupport.project;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.api.testsupport.user.UserHttpTestSupport;
import com.schemafy.core.project.adapter.out.persistence.InvitationRepository;
import com.schemafy.core.project.adapter.out.persistence.ProjectMemberRepository;
import com.schemafy.core.project.adapter.out.persistence.ProjectRepository;
import com.schemafy.core.project.adapter.out.persistence.ShareLinkRepository;
import com.schemafy.core.project.adapter.out.persistence.WorkspaceMemberRepository;
import com.schemafy.core.project.adapter.out.persistence.WorkspaceRepository;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ProjectHttpTestSupport extends UserHttpTestSupport {

  @Autowired
  protected DatabaseClient databaseClient;

  @Autowired
  protected WorkspaceRepository workspaceRepository;

  @Autowired
  protected WorkspaceMemberRepository workspaceMemberRepository;

  @Autowired
  protected ProjectRepository projectRepository;

  @Autowired
  protected ProjectMemberRepository projectMemberRepository;

  @Autowired
  protected InvitationRepository invitationRepository;

  @Autowired
  protected ShareLinkRepository shareLinkRepository;

  protected Mono<Void> cleanupProjectFixtures() {
    return Mono.when(
        shareLinkRepository.deleteAll(),
        invitationRepository.deleteAll(),
        projectMemberRepository.deleteAll(),
        projectRepository.deleteAll(),
        workspaceMemberRepository.deleteAll(),
        workspaceRepository.deleteAll())
        .then(cleanupUserFixtures());
  }

  protected Workspace saveWorkspace(String name, String description) {
    return workspaceRepository.save(Workspace.create(nextId(), name,
        description)).block();
  }

  protected Workspace saveWorkspace(Workspace workspace) {
    return workspaceRepository.save(workspace).block();
  }

  protected WorkspaceMember addWorkspaceMember(
      String workspaceId,
      String userId,
      WorkspaceRole role) {
    return workspaceMemberRepository.save(WorkspaceMember.create(nextId(),
        workspaceId, userId, role)).block();
  }

  protected WorkspaceMember saveWorkspaceMember(WorkspaceMember member) {
    return workspaceMemberRepository.save(member).block();
  }

  protected Flux<WorkspaceMember> findActiveWorkspaceMembersByUserId(
      String userId) {
    return workspaceMemberRepository.findAll()
        .filter(member -> userId.equals(member.getUserId()))
        .filter(member -> !member.isDeleted());
  }

  protected Project saveProject(
      String workspaceId,
      String name,
      String description) {
    return projectRepository.save(Project.create(nextId(), workspaceId, name,
        description)).block();
  }

  protected Project saveProject(Project project) {
    return projectRepository.save(project).block();
  }

  protected ProjectMember addProjectMember(
      String projectId,
      String userId,
      ProjectRole role) {
    return projectMemberRepository.save(ProjectMember.create(nextId(),
        projectId, userId, role)).block();
  }

  protected ProjectMember saveProjectMember(ProjectMember member) {
    return projectMemberRepository.save(member).block();
  }

  protected Invitation saveWorkspaceInvitation(
      String workspaceId,
      String invitedEmail,
      WorkspaceRole role,
      String invitedBy) {
    return invitationRepository.save(Invitation.createWorkspaceInvitation(
        nextId(), workspaceId, invitedEmail, role, invitedBy)).block();
  }

  protected Invitation saveProjectInvitation(
      String projectId,
      String workspaceId,
      String invitedEmail,
      ProjectRole role,
      String invitedBy) {
    return invitationRepository.save(Invitation.createProjectInvitation(
        nextId(), projectId, workspaceId, invitedEmail, role, invitedBy))
        .block();
  }

  protected Invitation saveInvitation(Invitation invitation) {
    return invitationRepository.save(invitation).block();
  }

  protected Invitation acceptInvitation(Invitation invitation) {
    databaseClient.sql("""
        UPDATE invitations
        SET status = 'ACCEPTED',
            resolved_at = CURRENT_TIMESTAMP
        WHERE id = :id
        """)
        .bind("id", invitation.getId())
        .fetch()
        .rowsUpdated()
        .block();
    return invitationRepository.findById(invitation.getId()).block();
  }

  protected Invitation expireInvitation(
      Invitation invitation,
      Instant expiresAt) {
    databaseClient.sql("UPDATE invitations SET expires_at = :expiresAt WHERE id = :id")
        .bind("expiresAt", expiresAt)
        .bind("id", invitation.getId())
        .fetch()
        .rowsUpdated()
        .block();
    return invitationRepository.findById(invitation.getId()).block();
  }

  protected Invitation softDeleteInvitation(Invitation invitation) {
    databaseClient.sql(
        "UPDATE invitations SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id")
        .bind("id", invitation.getId())
        .fetch()
        .rowsUpdated()
        .block();
    return invitationRepository.findById(invitation.getId()).block();
  }

  protected ShareLink saveShareLink(String projectId, String code) {
    return saveShareLink(projectId, code, null);
  }

  protected ShareLink saveShareLink(
      String projectId,
      String code,
      Instant expiresAt) {
    ShareLink shareLink = expiresAt == null
        ? ShareLink.create(nextId(), projectId, code)
        : ShareLink.create(nextId(), projectId, code, expiresAt);
    return shareLinkRepository.save(shareLink).block();
  }

  protected ShareLink saveShareLink(ShareLink shareLink) {
    return shareLinkRepository.save(shareLink).block();
  }

  protected void assertInvalidPagination(
      WebTestClient webTestClient,
      String uri,
      String token) {
    webTestClient.get()
        .uri(uri)
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.reason").isEqualTo(CommonErrorCode.INVALID_PARAMETER.code())
        .jsonPath("$.detail").value(detail -> assertThat((String) detail)
            .isNotBlank());
  }

}
