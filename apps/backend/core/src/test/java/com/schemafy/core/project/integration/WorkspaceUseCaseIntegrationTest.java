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
import com.schemafy.core.project.application.port.in.RemoveWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.in.RemoveWorkspaceMemberUseCase;
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

@DisplayName("워크스페이스 유스케이스 통합 테스트")
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
  private RemoveWorkspaceMemberUseCase removeWorkspaceMemberUseCase;

  @Autowired
  private LeaveWorkspaceUseCase leaveWorkspaceUseCase;

  @Test
  @DisplayName("워크스페이스 생성 시 상세 정보와 관리자 멤버십을 함께 만든다")
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
  @DisplayName("워크스페이스 삭제 시 프로젝트 관련 데이터와 schema를 함께 정리한다")
  void deleteWorkspace_cascadesRelatedRows() {
    User admin = signUpUser("admin-delete@test.com", "Admin");
    User invitee = signUpUser("invitee-delete@test.com", "Invitee");
    Workspace workspace = saveWorkspace("Delete WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    WorkspaceMember inviteeMember = saveWorkspaceMember(workspace, invitee,
        WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Cascade Project");
    var secondProject = saveProject(workspace, "Cascade Project 2");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    saveProjectMember(secondProject, admin, ProjectRole.ADMIN);
    createSchema(project, "workspace_delete_schema_1");
    createSchema(secondProject, "workspace_delete_schema_2");
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
    assertThat(projectRepository.findByIdAndNotDeleted(secondProject.getId()).block()).isNull();
    assertThat(findSchemasByProjectId(project.getId())).isEmpty();
    assertThat(findSchemasByProjectId(secondProject.getId())).isEmpty();

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
  @DisplayName("워크스페이스 멤버 추가 시 삭제된 멤버와 프로젝트 멤버십을 복원한다")
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
  @DisplayName("워크스페이스 멤버 추가 시 대문자 이메일 입력을 정규화한다")
  void addWorkspaceMember_normalizesUppercaseEmail() {
    User admin = signUpUser("admin-upper@test.com", "Admin");
    User target = signUpUser("target-upper@test.com", "Target");
    Workspace workspace = saveWorkspace("Upper WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);

    WorkspaceMember addedMember = addWorkspaceMemberUseCase.addWorkspaceMember(
        new AddWorkspaceMemberCommand(workspace.getId(), "TARGET-UPPER@TEST.COM",
            WorkspaceRole.MEMBER, admin.id()))
        .block();

    assertThat(addedMember).isNotNull();
    assertThat(addedMember.getUserId()).isEqualTo(target.id());
  }

  @Test
  @DisplayName("워크스페이스 역할을 낮춰도 프로젝트 편집자 역할은 유지한다")
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
  @DisplayName("프로젝트 관리자여도 워크스페이스에서는 관리자 등급을 내릴 수 있다")
  void updateWorkspaceMemberRole_allowsProjectAdminTarget() {
    User requester = signUpUser("requester-workspace-demote@test.com", "Requester");
    User target = signUpUser("target-workspace-demote@test.com", "Target");
    Workspace workspace = saveWorkspace("Workspace Demote WS", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.ADMIN);
    WorkspaceMember targetWorkspaceMember = saveWorkspaceMember(workspace, target,
        WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Workspace Demote Project");
    saveProjectMember(project, target, ProjectRole.ADMIN);

    WorkspaceMember updatedMember = updateWorkspaceMemberRoleUseCase.updateWorkspaceMemberRole(
        new UpdateWorkspaceMemberRoleCommand(workspace.getId(), target.id(), WorkspaceRole.MEMBER,
            requester.id()))
        .block();

    assertThat(updatedMember.getId()).isEqualTo(targetWorkspaceMember.getId());
    assertThat(updatedMember.getRoleAsEnum()).isEqualTo(WorkspaceRole.MEMBER);
  }

  @Test
  @DisplayName("프로젝트 관리자여도 워크스페이스에서는 멤버를 제거할 수 있다")
  void removeWorkspaceMember_allowsProjectAdminTarget() {
    User requester = signUpUser("requester-workspace-remove@test.com", "Requester");
    User target = signUpUser("target-workspace-remove@test.com", "Target");
    Workspace workspace = saveWorkspace("Workspace Remove WS", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.ADMIN);
    WorkspaceMember targetWorkspaceMember = saveWorkspaceMember(workspace, target,
        WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Workspace Remove Project");
    saveProjectMember(project, target, ProjectRole.ADMIN);

    removeWorkspaceMemberUseCase.removeWorkspaceMember(new RemoveWorkspaceMemberCommand(
        workspace.getId(), target.id(), requester.id())).block();

    WorkspaceMember deletedMember = workspaceMemberRepository.findById(targetWorkspaceMember.getId())
        .block();
    assertThat(deletedMember).isNotNull();
    assertThat(deletedMember.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("워크스페이스에서 멤버를 추방하면 같은 워크스페이스의 프로젝트 멤버십도 모두 삭제한다")
  void removeWorkspaceMember_removesAllSameWorkspaceProjectMemberships() {
    User requester = signUpUser("requester-workspace-cascade-remove@test.com", "Requester");
    User target = signUpUser("target-workspace-cascade-remove@test.com", "Target");
    Workspace workspace = saveWorkspace("Workspace Cascade Remove WS", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.ADMIN);
    WorkspaceMember targetWorkspaceMember = saveWorkspaceMember(workspace, target,
        WorkspaceRole.ADMIN);
    var firstProject = saveProject(workspace, "Workspace Cascade Remove Project 1");
    var secondProject = saveProject(workspace, "Workspace Cascade Remove Project 2");
    ProjectMember firstProjectMember = saveProjectMember(firstProject, target, ProjectRole.ADMIN);
    ProjectMember secondProjectMember = saveProjectMember(secondProject, target,
        ProjectRole.EDITOR);

    removeWorkspaceMemberUseCase.removeWorkspaceMember(new RemoveWorkspaceMemberCommand(
        workspace.getId(), target.id(), requester.id())).block();

    WorkspaceMember deletedWorkspaceMember = workspaceMemberRepository
        .findById(targetWorkspaceMember.getId()).block();
    ProjectMember deletedFirstProjectMember = projectMemberRepository
        .findById(firstProjectMember.getId()).block();
    ProjectMember deletedSecondProjectMember = projectMemberRepository
        .findById(secondProjectMember.getId()).block();

    assertThat(deletedWorkspaceMember).isNotNull();
    assertThat(deletedWorkspaceMember.isDeleted()).isTrue();
    assertThat(deletedFirstProjectMember).isNotNull();
    assertThat(deletedFirstProjectMember.isDeleted()).isTrue();
    assertThat(deletedSecondProjectMember).isNotNull();
    assertThat(deletedSecondProjectMember.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("마지막 워크스페이스 멤버가 나가면 schema까지 포함해 워크스페이스를 삭제한다")
  void leaveWorkspace_lastMemberDeletesWorkspace() {
    User admin = signUpUser("admin-leave@test.com", "Admin");
    User outsider = signUpUser("outsider-leave@test.com", "Outsider");
    Workspace workspace = saveWorkspace("Leave WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Leave Project");
    createSchema(project, "leave_workspace_schema");
    saveProjectMember(project, outsider, ProjectRole.VIEWER);

    leaveWorkspaceUseCase.leaveWorkspace(new LeaveWorkspaceCommand(
        workspace.getId(),
        admin.id())).block();

    assertThat(workspaceRepository.findByIdAndNotDeleted(workspace.getId()).block()).isNull();
    assertThat(projectRepository.findByIdAndNotDeleted(project.getId()).block()).isNull();
    assertThat(projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(), outsider.id())
        .block()).isNull();
    assertThat(findSchemasByProjectId(project.getId())).isEmpty();
  }

  @Test
  @DisplayName("다른 멤버가 남아 있으면 마지막 관리자의 탈퇴를 거부한다")
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

  @Test
  @DisplayName("워크스페이스를 직접 나가면 같은 워크스페이스의 프로젝트 멤버십도 모두 삭제한다")
  void leaveWorkspace_removesAllSameWorkspaceProjectMemberships() {
    User admin = signUpUser("admin-workspace-cascade-leave@test.com", "Admin");
    User requester = signUpUser("requester-workspace-cascade-leave@test.com", "Requester");
    Workspace workspace = saveWorkspace("Workspace Cascade Leave WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    WorkspaceMember requesterWorkspaceMember = saveWorkspaceMember(workspace, requester,
        WorkspaceRole.MEMBER);
    var firstProject = saveProject(workspace, "Workspace Cascade Leave Project 1");
    var secondProject = saveProject(workspace, "Workspace Cascade Leave Project 2");
    ProjectMember firstProjectMember = saveProjectMember(firstProject, requester,
        ProjectRole.EDITOR);
    ProjectMember secondProjectMember = saveProjectMember(secondProject, requester,
        ProjectRole.VIEWER);

    leaveWorkspaceUseCase.leaveWorkspace(new LeaveWorkspaceCommand(workspace.getId(),
        requester.id())).block();

    WorkspaceMember deletedWorkspaceMember = workspaceMemberRepository
        .findById(requesterWorkspaceMember.getId()).block();
    ProjectMember deletedFirstProjectMember = projectMemberRepository
        .findById(firstProjectMember.getId()).block();
    ProjectMember deletedSecondProjectMember = projectMemberRepository
        .findById(secondProjectMember.getId()).block();

    assertThat(deletedWorkspaceMember).isNotNull();
    assertThat(deletedWorkspaceMember.isDeleted()).isTrue();
    assertThat(deletedFirstProjectMember).isNotNull();
    assertThat(deletedFirstProjectMember.isDeleted()).isTrue();
    assertThat(deletedSecondProjectMember).isNotNull();
    assertThat(deletedSecondProjectMember.isDeleted()).isTrue();
  }

}
