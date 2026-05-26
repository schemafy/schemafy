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
import com.schemafy.core.project.application.port.in.GetProjectsQuery;
import com.schemafy.core.project.application.port.in.GetProjectsUseCase;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersQuery;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersUseCase;
import com.schemafy.core.project.application.port.in.GetWorkspaceQuery;
import com.schemafy.core.project.application.port.in.GetWorkspaceUseCase;
import com.schemafy.core.project.application.port.in.LeaveWorkspaceCommand;
import com.schemafy.core.project.application.port.in.LeaveWorkspaceUseCase;
import com.schemafy.core.project.application.port.in.RemoveWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.in.RemoveWorkspaceMemberUseCase;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceCommand;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleCommand;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleUseCase;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceUseCase;
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
  private GetWorkspaceUseCase getWorkspaceUseCase;

  @Autowired
  private GetWorkspaceMembersUseCase getWorkspaceMembersUseCase;

  @Autowired
  private GetProjectsUseCase getProjectsUseCase;

  @Autowired
  private UpdateWorkspaceUseCase updateWorkspaceUseCase;

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
  @DisplayName("워크스페이스 삭제는 비관리자면 거부된다")
  void deleteWorkspace_deniesNonAdmin() {
    User member = signUpUser("member-delete-workspace-denied@test.com", "Member");
    Workspace workspace = saveWorkspace("Delete Workspace Denied WS", "Description");
    saveWorkspaceMember(workspace, member, WorkspaceRole.MEMBER);

    StepVerifier.create(deleteWorkspaceUseCase.deleteWorkspace(new DeleteWorkspaceCommand(
        workspace.getId(),
        member.id())))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 삭제는 비회원이면 거부된다")
  void deleteWorkspace_deniesNonMember() {
    User requester = signUpUser("non-member-delete-workspace@test.com", "Requester");
    Workspace workspace = saveWorkspace("Delete Workspace Non Member WS", "Description");

    StepVerifier.create(deleteWorkspaceUseCase.deleteWorkspace(new DeleteWorkspaceCommand(
        workspace.getId(),
        requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 조회는 비회원이면 거부된다")
  void getWorkspace_deniesNonMember() {
    User requester = signUpUser("non-member-get-workspace@test.com", "Requester");
    Workspace workspace = saveWorkspace("Get Workspace Non Member WS", "Description");

    StepVerifier.create(getWorkspaceUseCase.getWorkspace(new GetWorkspaceQuery(
        workspace.getId(),
        requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 멤버 조회는 비회원이면 거부된다")
  void getWorkspaceMembers_deniesNonMember() {
    User requester = signUpUser("non-member-get-workspace-members@test.com", "Requester");
    Workspace workspace = saveWorkspace("Get Workspace Members Non Member WS", "Description");

    StepVerifier.create(getWorkspaceMembersUseCase.getWorkspaceMembers(
        new GetWorkspaceMembersQuery(workspace.getId(), requester.id(), 0, 10)))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();
  }

  @Test
  @DisplayName("프로젝트 목록 조회는 워크스페이스 비회원이면 거부된다")
  void getProjects_deniesWorkspaceNonMember() {
    User requester = signUpUser("non-member-get-projects@test.com", "Requester");
    Workspace workspace = saveWorkspace("Get Projects Non Member WS", "Description");

    StepVerifier.create(getProjectsUseCase.getProjects(
        new GetProjectsQuery(workspace.getId(), requester.id(), 0, 10)))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 수정은 요청자가 비관리자면 거부된다")
  void updateWorkspace_deniesNonAdminRequester() {
    User member = signUpUser("member-update-workspace-denied@test.com", "Member");
    Workspace workspace = saveWorkspace("Update Workspace Denied WS", "Description");
    saveWorkspaceMember(workspace, member, WorkspaceRole.MEMBER);

    StepVerifier.create(updateWorkspaceUseCase.updateWorkspace(
        new UpdateWorkspaceCommand(workspace.getId(), "Forbidden", "Forbidden", member.id())))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 수정은 요청자가 비회원이면 거부된다")
  void updateWorkspace_deniesNonMemberRequester() {
    User requester = signUpUser("non-member-update-workspace@test.com", "Requester");
    Workspace workspace = saveWorkspace("Update Workspace Non Member WS", "Description");

    StepVerifier.create(updateWorkspaceUseCase.updateWorkspace(
        new UpdateWorkspaceCommand(workspace.getId(), "Forbidden", "Forbidden", requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();
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
  @DisplayName("워크스페이스 ADMIN으로 합류하면 기존 참여 프로젝트는 ADMIN으로 승격하고 미참여 프로젝트는 ADMIN으로 추가한다")
  void addWorkspaceMember_promotesJoinedProjectsAndCreatesMissingProjectsForWorkspaceAdmin() {
    User admin = signUpUser("admin-add-admin@test.com", "Admin");
    User target = signUpUser("target-add-admin@test.com", "Target");
    Workspace workspace = saveWorkspace("Admin Join WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var joinedProject = saveProject(workspace, "Joined Project");
    var missingProject = saveProject(workspace, "Missing Project");
    ProjectMember existingProjectMember = saveProjectMember(joinedProject, target,
        ProjectRole.VIEWER);

    WorkspaceMember addedMember = addWorkspaceMemberUseCase.addWorkspaceMember(
        new AddWorkspaceMemberCommand(workspace.getId(), target.email(), WorkspaceRole.ADMIN,
            admin.id()))
        .block();

    ProjectMember upgradedProjectMember = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(joinedProject.getId(), target.id())
        .block();
    ProjectMember createdProjectMember = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(missingProject.getId(), target.id())
        .block();

    assertThat(addedMember).isNotNull();
    assertThat(addedMember.getRoleAsEnum()).isEqualTo(WorkspaceRole.ADMIN);
    assertThat(upgradedProjectMember.getId()).isEqualTo(existingProjectMember.getId());
    assertThat(upgradedProjectMember.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
    assertThat(createdProjectMember).isNotNull();
    assertThat(createdProjectMember.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
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
  @DisplayName("워크스페이스 멤버 추가는 요청자가 비관리자면 거부된다")
  void addWorkspaceMember_deniesNonAdminRequester() {
    User member = signUpUser("member-add-workspace-denied@test.com", "Member");
    User target = signUpUser("target-add-workspace-denied@test.com", "Target");
    Workspace workspace = saveWorkspace("Add Workspace Denied WS", "Description");
    saveWorkspaceMember(workspace, member, WorkspaceRole.MEMBER);

    StepVerifier.create(addWorkspaceMemberUseCase.addWorkspaceMember(
        new AddWorkspaceMemberCommand(workspace.getId(), target.email(), WorkspaceRole.MEMBER,
            member.id())))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 멤버 추가는 요청자가 비회원이면 거부된다")
  void addWorkspaceMember_deniesNonMemberRequester() {
    User requester = signUpUser("non-member-add-workspace-denied@test.com", "Requester");
    User target = signUpUser("target-add-workspace-non-member-denied@test.com", "Target");
    Workspace workspace = saveWorkspace("Add Workspace Non Member Denied WS", "Description");

    StepVerifier.create(addWorkspaceMemberUseCase.addWorkspaceMember(
        new AddWorkspaceMemberCommand(workspace.getId(), target.email(), WorkspaceRole.MEMBER,
            requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();
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
  @DisplayName("워크스페이스 역할 변경은 요청자가 비관리자면 거부된다")
  void updateWorkspaceMemberRole_deniesNonAdminRequester() {
    User member = signUpUser("member-workspace-role-denied@test.com", "Member");
    User target = signUpUser("target-workspace-role-denied@test.com", "Target");
    Workspace workspace = saveWorkspace("Workspace Role Denied WS", "Description");
    saveWorkspaceMember(workspace, member, WorkspaceRole.MEMBER);
    saveWorkspaceMember(workspace, target, WorkspaceRole.MEMBER);

    StepVerifier.create(updateWorkspaceMemberRoleUseCase.updateWorkspaceMemberRole(
        new UpdateWorkspaceMemberRoleCommand(workspace.getId(), target.id(), WorkspaceRole.ADMIN,
            member.id())))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 역할 변경은 요청자가 비회원이면 거부된다")
  void updateWorkspaceMemberRole_deniesNonMemberRequester() {
    User requester = signUpUser("non-member-workspace-role-denied@test.com", "Requester");
    User target = signUpUser("target-workspace-role-non-member-denied@test.com", "Target");
    Workspace workspace = saveWorkspace("Workspace Role Non Member Denied WS", "Description");
    saveWorkspaceMember(workspace, target, WorkspaceRole.MEMBER);

    StepVerifier.create(updateWorkspaceMemberRoleUseCase.updateWorkspaceMemberRole(
        new UpdateWorkspaceMemberRoleCommand(workspace.getId(), target.id(), WorkspaceRole.ADMIN,
            requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 멤버를 ADMIN으로 승격하면 활성, 삭제, 누락 프로젝트 멤버십을 모두 ADMIN으로 맞춘다")
  void updateWorkspaceMemberRole_promotesAndRestoresProjectMembershipsForAdmin() {
    User requester = signUpUser("requester-workspace-promote@test.com", "Requester");
    User target = signUpUser("target-workspace-promote@test.com", "Target");
    Workspace workspace = saveWorkspace("Workspace Promote WS", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.ADMIN);
    WorkspaceMember targetWorkspaceMember = saveWorkspaceMember(workspace, target,
        WorkspaceRole.MEMBER);
    var activeProject = saveProject(workspace, "Workspace Promote Active Project");
    var deletedProject = saveProject(workspace, "Workspace Promote Deleted Project");
    var missingProject = saveProject(workspace, "Workspace Promote Missing Project");
    ProjectMember activeProjectMember = saveProjectMember(activeProject, target, ProjectRole.VIEWER);
    ProjectMember deletedProjectMember = saveProjectMember(deletedProject, target,
        ProjectRole.VIEWER);
    softDeleteProjectMember(deletedProjectMember.getId());

    WorkspaceMember updatedMember = updateWorkspaceMemberRoleUseCase.updateWorkspaceMemberRole(
        new UpdateWorkspaceMemberRoleCommand(workspace.getId(), target.id(), WorkspaceRole.ADMIN,
            requester.id()))
        .block();

    ProjectMember promotedProjectMember = projectMemberRepository.findById(activeProjectMember.getId())
        .block();
    ProjectMember restoredProjectMember = projectMemberRepository.findById(deletedProjectMember.getId())
        .block();
    ProjectMember createdProjectMember = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(missingProject.getId(), target.id())
        .block();

    assertThat(updatedMember.getId()).isEqualTo(targetWorkspaceMember.getId());
    assertThat(updatedMember.getRoleAsEnum()).isEqualTo(WorkspaceRole.ADMIN);
    assertThat(promotedProjectMember).isNotNull();
    assertThat(promotedProjectMember.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
    assertThat(restoredProjectMember).isNotNull();
    assertThat(restoredProjectMember.isDeleted()).isFalse();
    assertThat(restoredProjectMember.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
    assertThat(createdProjectMember).isNotNull();
    assertThat(createdProjectMember.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
  }

  @Test
  @DisplayName("워크스페이스 ADMIN이자 프로젝트 ADMIN인 대상자를 워크스페이스 MEMBER로 강등하면 프로젝트 역할도 VIEWER로 내려간다")
  void updateWorkspaceMemberRole_demotesWorkspaceAndProjectAdminToViewer() {
    User requester = signUpUser("requester-workspace-demote@test.com", "Requester");
    User target = signUpUser("target-workspace-demote@test.com", "Target");
    Workspace workspace = saveWorkspace("Workspace Demote WS", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.ADMIN);
    WorkspaceMember targetWorkspaceMember = saveWorkspaceMember(workspace, target,
        WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Workspace Demote Project");
    ProjectMember targetProjectMember = saveProjectMember(project, target, ProjectRole.ADMIN);

    WorkspaceMember updatedMember = updateWorkspaceMemberRoleUseCase.updateWorkspaceMemberRole(
        new UpdateWorkspaceMemberRoleCommand(workspace.getId(), target.id(), WorkspaceRole.MEMBER,
            requester.id()))
        .block();

    ProjectMember updatedProjectMember = projectMemberRepository.findById(targetProjectMember.getId())
        .block();

    assertThat(updatedMember.getId()).isEqualTo(targetWorkspaceMember.getId());
    assertThat(updatedMember.getRoleAsEnum()).isEqualTo(WorkspaceRole.MEMBER);
    assertThat(updatedProjectMember.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
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
  @DisplayName("워크스페이스 멤버 제거는 요청자가 비관리자면 거부된다")
  void removeWorkspaceMember_deniesNonAdminRequester() {
    User member = signUpUser("member-workspace-remove-denied@test.com", "Member");
    User target = signUpUser("target-workspace-remove-denied@test.com", "Target");
    Workspace workspace = saveWorkspace("Workspace Remove Denied WS", "Description");
    saveWorkspaceMember(workspace, member, WorkspaceRole.MEMBER);
    saveWorkspaceMember(workspace, target, WorkspaceRole.MEMBER);

    StepVerifier.create(removeWorkspaceMemberUseCase.removeWorkspaceMember(
        new RemoveWorkspaceMemberCommand(workspace.getId(), target.id(), member.id())))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 멤버 제거는 요청자가 비회원이면 거부된다")
  void removeWorkspaceMember_deniesNonMemberRequester() {
    User requester = signUpUser("non-member-workspace-remove-denied@test.com", "Requester");
    User target = signUpUser("target-workspace-remove-non-member-denied@test.com", "Target");
    Workspace workspace = saveWorkspace("Workspace Remove Non Member Denied WS", "Description");
    saveWorkspaceMember(workspace, target, WorkspaceRole.MEMBER);

    StepVerifier.create(removeWorkspaceMemberUseCase.removeWorkspaceMember(
        new RemoveWorkspaceMemberCommand(workspace.getId(), target.id(), requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();
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

  @Test
  @DisplayName("워크스페이스 탈퇴는 비회원이면 거부된다")
  void leaveWorkspace_deniesNonMember() {
    User requester = signUpUser("non-member-workspace-leave@test.com", "Requester");
    User member = signUpUser("member-workspace-leave-non-member@test.com", "Member");
    Workspace workspace = saveWorkspace("Workspace Leave Non Member WS", "Description");
    saveWorkspaceMember(workspace, member, WorkspaceRole.ADMIN);

    StepVerifier.create(leaveWorkspaceUseCase.leaveWorkspace(new LeaveWorkspaceCommand(
        workspace.getId(),
        requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();

    assertThat(workspaceRepository.findByIdAndNotDeleted(workspace.getId()).block()).isNotNull();
  }

}
