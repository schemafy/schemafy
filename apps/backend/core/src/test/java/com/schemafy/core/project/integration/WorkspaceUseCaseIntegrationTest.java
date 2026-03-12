package com.schemafy.core.project.integration;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberUseCase;
import com.schemafy.core.project.application.port.in.CreateWorkspaceCommand;
import com.schemafy.core.project.application.port.in.CreateWorkspaceUseCase;
import com.schemafy.core.project.application.port.in.DeleteWorkspaceCommand;
import com.schemafy.core.project.application.port.in.DeleteWorkspaceUseCase;
import com.schemafy.core.project.application.port.in.LeaveWorkspaceCommand;
import com.schemafy.core.project.application.port.in.LeaveWorkspaceUseCase;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleCommand;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleUseCase;
import com.schemafy.core.project.application.port.in.WorkspaceDetail;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;
import com.schemafy.core.user.domain.User;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Workspace usecase integration")
class WorkspaceUseCaseIntegrationTest extends ProjectDomainIntegrationSupport {

  @Autowired
  private CreateWorkspaceUseCase createWorkspaceUseCase;

  @Autowired
  private DeleteWorkspaceUseCase deleteWorkspaceUseCase;

  @Autowired
  private AddWorkspaceMemberUseCase addWorkspaceMemberUseCase;

  @Autowired
  private UpdateWorkspaceMemberRoleUseCase updateWorkspaceMemberRoleUseCase;

  @Autowired
  private LeaveWorkspaceUseCase leaveWorkspaceUseCase;

  @Test
  @DisplayName("createWorkspace creates workspace detail and admin membership")
  void createWorkspace_createsAdminMembership() {
    User admin = signUpUser("admin-workspace@test.com", "Admin");

    WorkspaceDetail detail = createWorkspaceUseCase.createWorkspace(
        new CreateWorkspaceCommand("Workspace", "Description", admin.id()))
        .block();

    assertThat(detail).isNotNull();
    assertThat(detail.workspace().getName()).isEqualTo("Workspace");
    assertThat(detail.projectCount()).isZero();
    assertThat(detail.currentUserRole()).isEqualTo(WorkspaceRole.ADMIN.name());

    WorkspaceMember member = workspaceMemberRepository
        .findByWorkspaceIdAndUserIdAndNotDeleted(detail.workspace().getId(), admin.id())
        .block();
    assertThat(member).isNotNull();
    assertThat(member.getRoleAsEnum()).isEqualTo(WorkspaceRole.ADMIN);
  }

  @Test
  @DisplayName("deleteWorkspace soft-deletes projects, memberships, invitations, and share links")
  void deleteWorkspace_cascadesRelatedRows() {
    User admin = signUpUser("admin-delete@test.com", "Admin");
    User invitee = signUpUser("invitee-delete@test.com", "Invitee");
    Workspace workspace = saveWorkspace("Delete WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    WorkspaceMember inviteeMember = saveWorkspaceMember(workspace, invitee,
        WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Cascade Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    Invitation workspaceInvitation = saveWorkspaceInvitation(workspace, invitee.email(),
        WorkspaceRole.MEMBER, admin);
    Invitation projectInvitation = saveProjectInvitation(project, workspace, invitee.email(),
        ProjectRole.EDITOR, admin);
    ShareLink shareLink = saveShareLink(project);

    deleteWorkspaceUseCase.deleteWorkspace(new DeleteWorkspaceCommand(
        workspace.getId(),
        admin.id())).block();

    assertThat(workspaceRepository.findByIdAndNotDeleted(workspace.getId()).block()).isNull();
    assertThat(projectRepository.findByIdAndNotDeleted(project.getId()).block()).isNull();

    WorkspaceMember deletedWorkspaceMember = workspaceMemberRepository
        .findById(inviteeMember.getId())
        .block();
    assertThat(deletedWorkspaceMember).isNotNull();
    assertThat(deletedWorkspaceMember.isDeleted()).isTrue();

    Invitation deletedWorkspaceInvitation = invitationRepository
        .findById(workspaceInvitation.getId()).block();
    Invitation deletedProjectInvitation = invitationRepository
        .findById(projectInvitation.getId()).block();
    ShareLink deletedShareLink = shareLinkRepository.findById(shareLink.getId()).block();
    assertThat(deletedWorkspaceInvitation.isDeleted()).isTrue();
    assertThat(deletedProjectInvitation.isDeleted()).isTrue();
    assertThat(deletedShareLink.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("addWorkspaceMember restores deleted member and project membership")
  void addWorkspaceMember_restoresDeletedRows() {
    User admin = signUpUser("admin-add@test.com", "Admin");
    User target = signUpUser("target-add@test.com", "Target");
    Workspace workspace = saveWorkspace("Restore WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Restore Project");

    WorkspaceMember deletedWorkspaceMember = saveWorkspaceMember(workspace, target,
        WorkspaceRole.MEMBER);
    softDeleteWorkspaceMember(deletedWorkspaceMember.getId());

    ProjectMember deletedProjectMember = saveProjectMember(project, target, ProjectRole.ADMIN);
    softDeleteProjectMember(deletedProjectMember.getId());

    WorkspaceMember restoredMember = addWorkspaceMemberUseCase.addWorkspaceMember(
        new AddWorkspaceMemberCommand(workspace.getId(), target.email(), WorkspaceRole.MEMBER,
            admin.id()))
        .block();

    ProjectMember restoredProjectMember = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(), target.id())
        .block();

    assertThat(restoredMember.getId()).isEqualTo(deletedWorkspaceMember.getId());
    assertThat(restoredMember.getRoleAsEnum()).isEqualTo(WorkspaceRole.MEMBER);
    assertThat(restoredProjectMember.getId()).isEqualTo(deletedProjectMember.getId());
    assertThat(restoredProjectMember.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
    assertThat(restoredProjectMember.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("updateWorkspaceMemberRole downgrades workspace role but preserves project editor role")
  void updateWorkspaceMemberRole_preservesEditorMembership() {
    User requester = signUpUser("requester-role@test.com", "Requester");
    User target = signUpUser("target-role@test.com", "Target");
    Workspace workspace = saveWorkspace("Role WS", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.ADMIN);
    WorkspaceMember targetWorkspaceMember = saveWorkspaceMember(workspace, target,
        WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Editor Project");
    ProjectMember projectMember = saveProjectMember(project, target, ProjectRole.EDITOR);

    WorkspaceMember updatedMember = updateWorkspaceMemberRoleUseCase.updateWorkspaceMemberRole(
        new UpdateWorkspaceMemberRoleCommand(workspace.getId(), target.id(), WorkspaceRole.MEMBER,
            requester.id()))
        .block();

    ProjectMember updatedProjectMember = projectMemberRepository.findById(projectMember.getId())
        .block();

    assertThat(updatedMember.getId()).isEqualTo(targetWorkspaceMember.getId());
    assertThat(updatedMember.getRoleAsEnum()).isEqualTo(WorkspaceRole.MEMBER);
    assertThat(updatedProjectMember.getRoleAsEnum()).isEqualTo(ProjectRole.EDITOR);
  }

  @Test
  @DisplayName("leaveWorkspace deletes workspace when last member leaves even with project-only members")
  void leaveWorkspace_lastMemberDeletesWorkspace() {
    User admin = signUpUser("admin-leave@test.com", "Admin");
    User outsider = signUpUser("outsider-leave@test.com", "Outsider");
    Workspace workspace = saveWorkspace("Leave WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Leave Project");
    saveProjectMember(project, outsider, ProjectRole.VIEWER);

    leaveWorkspaceUseCase.leaveWorkspace(new LeaveWorkspaceCommand(
        workspace.getId(),
        admin.id())).block();

    assertThat(workspaceRepository.findByIdAndNotDeleted(workspace.getId()).block()).isNull();
    assertThat(projectRepository.findByIdAndNotDeleted(project.getId()).block()).isNull();
    assertThat(projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(), outsider.id())
        .block()).isNull();
  }

  @Test
  @DisplayName("leaveWorkspace rejects last admin leaving when other members still exist")
  void leaveWorkspace_lastAdminCannotLeave() {
    User admin = signUpUser("admin-guard@test.com", "Admin");
    User member = signUpUser("member-guard@test.com", "Member");
    Workspace workspace = saveWorkspace("Guard WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, member, WorkspaceRole.MEMBER);

    StepVerifier.create(leaveWorkspaceUseCase.leaveWorkspace(
        new LeaveWorkspaceCommand(workspace.getId(), admin.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            WorkspaceErrorCode.LAST_ADMIN_CANNOT_LEAVE))
        .verify();
  }

}
