package com.schemafy.core.testsupport.project;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;

import com.schemafy.core.testsupport.user.UserHttpTestSupport;
import com.schemafy.domain.project.adapter.out.persistence.DomainInvitationRepository;
import com.schemafy.domain.project.adapter.out.persistence.DomainProjectMemberRepository;
import com.schemafy.domain.project.adapter.out.persistence.DomainProjectRepository;
import com.schemafy.domain.project.adapter.out.persistence.DomainShareLinkRepository;
import com.schemafy.domain.project.adapter.out.persistence.DomainWorkspaceMemberRepository;
import com.schemafy.domain.project.adapter.out.persistence.DomainWorkspaceRepository;
import com.schemafy.domain.project.domain.Invitation;
import com.schemafy.domain.project.domain.Project;
import com.schemafy.domain.project.domain.ProjectMember;
import com.schemafy.domain.project.domain.ProjectRole;
import com.schemafy.domain.project.domain.ShareLink;
import com.schemafy.domain.project.domain.Workspace;
import com.schemafy.domain.project.domain.WorkspaceMember;
import com.schemafy.domain.project.domain.WorkspaceRole;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class ProjectHttpTestSupport extends UserHttpTestSupport {

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

}
