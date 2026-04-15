package com.schemafy.core.project.integration;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.in.CreateWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.in.GetMyWorkspaceInvitationsQuery;
import com.schemafy.core.project.application.port.in.GetMyWorkspaceInvitationsUseCase;
import com.schemafy.core.project.application.port.in.GetWorkspaceInvitationsQuery;
import com.schemafy.core.project.application.port.in.GetWorkspaceInvitationsUseCase;
import com.schemafy.core.project.application.port.in.RejectWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.RejectWorkspaceInvitationUseCase;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("워크스페이스 초대 유스케이스 통합 테스트")
class WorkspaceInvitationUseCaseIntegrationTest
    extends ProjectDomainIntegrationSupport {

  @Autowired
  private CreateWorkspaceInvitationUseCase createWorkspaceInvitationUseCase;

  @Autowired
  private GetWorkspaceInvitationsUseCase getWorkspaceInvitationsUseCase;

  @Autowired
  private GetMyWorkspaceInvitationsUseCase getMyWorkspaceInvitationsUseCase;

  @Autowired
  private AcceptWorkspaceInvitationUseCase acceptWorkspaceInvitationUseCase;

  @Autowired
  private RejectWorkspaceInvitationUseCase rejectWorkspaceInvitationUseCase;

  @Test
  @DisplayName("워크스페이스 초대 생성 후 대상 목록과 내 초대 목록에서 모두 조회된다")
  void createWorkspaceInvitation_listsForAdminAndInvitee() {
    User admin = signUpUser("admin-wi@test.com", "Admin");
    User invitee = signUpUser("invitee-wi@test.com", "Invitee");
    var workspace = saveWorkspace("Invitation WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);

    Invitation invitation = createWorkspaceInvitationUseCase.createWorkspaceInvitation(
        new CreateWorkspaceInvitationCommand(workspace.getId(), invitee.email(), WorkspaceRole.MEMBER,
            admin.id()))
        .block();

    PageResult<Invitation> targetInvitations = getWorkspaceInvitationsUseCase.getWorkspaceInvitations(
        new GetWorkspaceInvitationsQuery(workspace.getId(), admin.id(), 0, 10))
        .block();
    PageResult<Invitation> myInvitations = getMyWorkspaceInvitationsUseCase.getMyWorkspaceInvitations(
        new GetMyWorkspaceInvitationsQuery(invitee.id(), 0, 10))
        .block();

    assertThat(invitation).isNotNull();
    assertThat(targetInvitations.content()).extracting(Invitation::getId)
        .contains(invitation.getId());
    assertThat(myInvitations.content()).extracting(Invitation::getId)
        .contains(invitation.getId());
  }

  @Test
  @DisplayName("워크스페이스 초대 생성 시 미등록 이메일은 거부한다")
  void createWorkspaceInvitation_rejectsUnknownUser() {
    User admin = signUpUser("admin-wi-missing@test.com", "Admin");
    var workspace = saveWorkspace("Missing WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);

    StepVerifier.create(createWorkspaceInvitationUseCase.createWorkspaceInvitation(
        new CreateWorkspaceInvitationCommand(workspace.getId(), "missing@test.com",
            WorkspaceRole.MEMBER, admin.id())))
        .expectErrorMatches(DomainException.hasErrorCode(UserErrorCode.NOT_FOUND))
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 초대 생성 시 대문자 중복 이메일도 같은 초대로 처리한다")
  void createWorkspaceInvitation_duplicatePending_caseInsensitive() {
    User admin = signUpUser("admin-wi-case@test.com", "Admin");
    User invitee = signUpUser("invitee-wi-case@test.com", "Invitee");
    var workspace = saveWorkspace("Case WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);

    createWorkspaceInvitationUseCase.createWorkspaceInvitation(
        new CreateWorkspaceInvitationCommand(workspace.getId(), invitee.email(),
            WorkspaceRole.MEMBER, admin.id()))
        .block();

    StepVerifier.create(createWorkspaceInvitationUseCase.createWorkspaceInvitation(
        new CreateWorkspaceInvitationCommand(workspace.getId(), "INVITEE-WI-CASE@TEST.COM",
            WorkspaceRole.MEMBER, admin.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            ProjectErrorCode.INVITATION_ALREADY_EXISTS))
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 중복 pending 초대는 하나를 수락하면 나머지가 cancelled로 수렴한다")
  void acceptWorkspaceInvitation_duplicatePendingFixtures_convergeOnAccept() {
    User admin = signUpUser("admin-wi-duplicate-accept@test.com", "Admin");
    User invitee = signUpUser("invitee-wi-duplicate-accept@test.com", "Invitee");
    var workspace = saveWorkspace("Duplicate Accept WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Duplicate Accept Project");

    Invitation acceptedInvitation = saveWorkspaceInvitation(workspace, invitee.email(),
        WorkspaceRole.MEMBER, admin);
    Invitation siblingInvitation = saveWorkspaceInvitation(workspace, invitee.email(),
        WorkspaceRole.MEMBER, admin);

    WorkspaceMember restoredMember = acceptWorkspaceInvitationUseCase.acceptWorkspaceInvitation(
        new AcceptWorkspaceInvitationCommand(acceptedInvitation.getId(), invitee.id()))
        .block();

    Invitation accepted = invitationRepository.findById(acceptedInvitation.getId()).block();
    Invitation sibling = invitationRepository.findById(siblingInvitation.getId()).block();

    assertThat(accepted).isNotNull();
    assertThat(accepted.getStatusAsEnum()).isEqualTo(InvitationStatus.ACCEPTED);
    assertThat(sibling).isNotNull();
    assertThat(sibling.getStatusAsEnum()).isEqualTo(InvitationStatus.CANCELLED);
    assertThat(restoredMember).isNotNull();
    assertThat(countActiveWorkspaceMemberRows(workspace.getId(), invitee.id())).isEqualTo(1);
    assertThat(countActiveProjectMemberRows(project.getId(), invitee.id())).isEqualTo(1);

    StepVerifier.create(acceptWorkspaceInvitationUseCase.acceptWorkspaceInvitation(
        new AcceptWorkspaceInvitationCommand(siblingInvitation.getId(), invitee.id())))
        .expectErrorMatches(error -> error instanceof DomainException de
            && (de.getErrorCode() == ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER
                || de.getErrorCode() == ProjectErrorCode.WORKSPACE_INVITATION_ALREADY_PROCESSED))
        .verify();
    assertThat(countActiveWorkspaceMemberRows(workspace.getId(), invitee.id())).isEqualTo(1);
    assertThat(countActiveProjectMemberRows(project.getId(), invitee.id())).isEqualTo(1);
  }

  @Test
  @DisplayName("워크스페이스 초대 수락 시 soft delete 된 워크스페이스와 프로젝트 멤버십을 복원한다")
  void acceptWorkspaceInvitation_restoresMemberships() {
    User admin = signUpUser("admin-wi-restore@test.com", "Admin");
    User invitee = signUpUser("invitee-wi-restore@test.com", "Invitee");
    var workspace = saveWorkspace("Restore Invitation WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Restore Invitation Project");

    WorkspaceMember deletedWorkspaceMember = saveWorkspaceMember(workspace, invitee,
        WorkspaceRole.MEMBER);
    softDeleteWorkspaceMember(deletedWorkspaceMember.getId());

    ProjectMember deletedProjectMember = saveProjectMember(project, invitee, ProjectRole.VIEWER);
    softDeleteProjectMember(deletedProjectMember.getId());

    Invitation acceptedInvitation = saveWorkspaceInvitation(workspace, invitee.email(),
        WorkspaceRole.ADMIN, admin);

    WorkspaceMember restoredMember = acceptWorkspaceInvitationUseCase.acceptWorkspaceInvitation(
        new AcceptWorkspaceInvitationCommand(acceptedInvitation.getId(), invitee.id()))
        .block();

    ProjectMember restoredProjectMember = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(), invitee.id())
        .block();

    assertThat(restoredMember.getId()).isEqualTo(deletedWorkspaceMember.getId());
    assertThat(restoredMember.getRoleAsEnum()).isEqualTo(WorkspaceRole.ADMIN);
    assertThat(restoredProjectMember.getId()).isEqualTo(deletedProjectMember.getId());
    assertThat(restoredProjectMember.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
  }

  @Test
  @DisplayName("워크스페이스 초대 수락 시 같은 워크스페이스의 pending 프로젝트 초대를 취소한다")
  void acceptWorkspaceInvitation_cancelsPendingProjectInvitationsInSameWorkspace() {
    User admin = signUpUser("admin-wi-cancel-project-inv@test.com", "Admin");
    User invitee = signUpUser("invitee-wi-cancel-project-inv@test.com",
        "Invitee");
    var workspace = saveWorkspace("Accept Cancel Project Invitation WS",
        "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Accept Cancel Project Invitation");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    Invitation workspaceInvitation = saveWorkspaceInvitation(workspace,
        invitee.email(), WorkspaceRole.MEMBER, admin);
    Invitation projectInvitation = saveProjectInvitation(project, workspace,
        invitee.email(), ProjectRole.EDITOR, admin);

    WorkspaceMember acceptedMember = acceptWorkspaceInvitationUseCase
        .acceptWorkspaceInvitation(new AcceptWorkspaceInvitationCommand(
            workspaceInvitation.getId(), invitee.id()))
        .block();

    Invitation cancelledInvitation = invitationRepository
        .findById(projectInvitation.getId())
        .block();
    ProjectMember materializedMember = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(), invitee.id())
        .block();

    assertThat(acceptedMember).isNotNull();
    assertThat(cancelledInvitation).isNotNull();
    assertThat(cancelledInvitation.getStatusAsEnum())
        .isEqualTo(InvitationStatus.CANCELLED);
    assertThat(materializedMember).isNotNull();
    assertThat(materializedMember.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
  }

  @Test
  @DisplayName("워크스페이스 초대 거절 시 초대 상태를 rejected로 바꾼다")
  void rejectWorkspaceInvitation_marksResolved() {
    User admin = signUpUser("admin-wi-reject@test.com", "Admin");
    User invitee = signUpUser("invitee-wi-reject@test.com", "Invitee");
    var workspace = saveWorkspace("Reject WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Invitation invitation = saveWorkspaceInvitation(workspace, invitee.email(), WorkspaceRole.MEMBER,
        admin);

    rejectWorkspaceInvitationUseCase.rejectWorkspaceInvitation(
        new RejectWorkspaceInvitationCommand(invitation.getId(), invitee.id()))
        .block();

    Invitation rejected = invitationRepository.findById(invitation.getId()).block();
    assertThat(rejected.getStatusAsEnum()).isEqualTo(InvitationStatus.REJECTED);
    assertThat(rejected.getResolvedAt()).isNotNull();
  }

  @Test
  @DisplayName("워크스페이스 초대 거절 시 타입이 다른 초대는 거부한다")
  void rejectWorkspaceInvitation_rejectsTypeMismatch() {
    User admin = signUpUser("admin-wi-type@test.com", "Admin");
    User invitee = signUpUser("invitee-wi-type@test.com", "Invitee");
    var workspace = saveWorkspace("Type WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Type Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    Invitation projectInvitation = saveProjectInvitation(project, workspace, invitee.email(),
        ProjectRole.EDITOR, admin);

    StepVerifier.create(rejectWorkspaceInvitationUseCase.rejectWorkspaceInvitation(
        new RejectWorkspaceInvitationCommand(projectInvitation.getId(), invitee.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            ProjectErrorCode.INVITATION_TYPE_MISMATCH))
        .verify();
  }

  private long countActiveWorkspaceMemberRows(String workspaceId, String userId) {
    return databaseClient.sql("""
        SELECT COUNT(*) FROM workspace_members
        WHERE workspace_id = :workspaceId
          AND user_id = :userId
          AND deleted_at IS NULL
        """)
        .bind("workspaceId", workspaceId)
        .bind("userId", userId)
        .map(row -> row.get(0, Long.class))
        .one()
        .block();
  }

  private long countActiveProjectMemberRows(String projectId, String userId) {
    return databaseClient.sql("""
        SELECT COUNT(*) FROM project_members
        WHERE project_id = :projectId
          AND user_id = :userId
          AND deleted_at IS NULL
        """)
        .bind("projectId", projectId)
        .bind("userId", userId)
        .map(row -> row.get(0, Long.class))
        .one()
        .block();
  }

}
