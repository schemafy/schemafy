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
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;
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
  @DisplayName("워크스페이스 초대 생성 시 관리자가 아닌 요청자는 이메일 형식 검증보다 먼저 거부한다")
  void createWorkspaceInvitation_rejectsNonAdminRequesterBeforeInvalidEmail() {
    User admin = signUpUser("admin-wi-auth-order@test.com", "Admin");
    User requester = signUpUser("requester-wi-auth-order@test.com", "Requester");
    var workspace = saveWorkspace("Auth Order WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, requester, WorkspaceRole.MEMBER);

    StepVerifier.create(createWorkspaceInvitationUseCase.createWorkspaceInvitation(
        new CreateWorkspaceInvitationCommand(workspace.getId(), "not-an-email",
            WorkspaceRole.MEMBER, requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 초대 목록 조회 시 관리자가 아닌 요청자는 거부한다")
  void getWorkspaceInvitations_rejectsNonAdminRequester() {
    User admin = signUpUser("admin-wi-list-auth@test.com", "Admin");
    User requester = signUpUser("requester-wi-list-auth@test.com", "Requester");
    var workspace = saveWorkspace("List Auth WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, requester, WorkspaceRole.MEMBER);

    StepVerifier.create(getWorkspaceInvitationsUseCase.getWorkspaceInvitations(
        new GetWorkspaceInvitationsQuery(workspace.getId(), requester.id(), 0, 10)))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ADMIN_REQUIRED))
        .verify();
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
    Invitation siblingInvitation = saveWorkspaceInvitation(workspace, invitee.email(),
        WorkspaceRole.MEMBER, admin);

    WorkspaceMember restoredMember = acceptWorkspaceInvitationUseCase.acceptWorkspaceInvitation(
        new AcceptWorkspaceInvitationCommand(acceptedInvitation.getId(), invitee.id()))
        .block();

    ProjectMember restoredProjectMember = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(), invitee.id())
        .block();
    Invitation cancelledSibling = invitationRepository.findById(siblingInvitation.getId()).block();

    assertThat(restoredMember.getId()).isEqualTo(deletedWorkspaceMember.getId());
    assertThat(restoredMember.getRoleAsEnum()).isEqualTo(WorkspaceRole.ADMIN);
    assertThat(restoredProjectMember.getId()).isEqualTo(deletedProjectMember.getId());
    assertThat(restoredProjectMember.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
    assertThat(cancelledSibling.getStatusAsEnum()).isEqualTo(InvitationStatus.CANCELLED);
  }

  @Test
  @DisplayName("워크스페이스 초대 수락 시 초대 이메일과 다른 요청자는 거부한다")
  void acceptWorkspaceInvitation_rejectsRequesterWithDifferentEmail() {
    User admin = signUpUser("admin-wi-accept-owner@test.com", "Admin");
    User invitee = signUpUser("invitee-wi-accept-owner@test.com", "Invitee");
    User requester = signUpUser("requester-wi-accept-owner@test.com", "Requester");
    var workspace = saveWorkspace("Accept Owner WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Invitation invitation = saveWorkspaceInvitation(workspace, invitee.email(),
        WorkspaceRole.MEMBER, admin);

    StepVerifier.create(acceptWorkspaceInvitationUseCase.acceptWorkspaceInvitation(
        new AcceptWorkspaceInvitationCommand(invitation.getId(), requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            ProjectErrorCode.INVITATION_EMAIL_MISMATCH))
        .verify();
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
  @DisplayName("워크스페이스 초대 거절 시 초대 이메일과 다른 요청자는 거부한다")
  void rejectWorkspaceInvitation_rejectsRequesterWithDifferentEmail() {
    User admin = signUpUser("admin-wi-reject-owner@test.com", "Admin");
    User invitee = signUpUser("invitee-wi-reject-owner@test.com", "Invitee");
    User requester = signUpUser("requester-wi-reject-owner@test.com", "Requester");
    var workspace = saveWorkspace("Reject Owner WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Invitation invitation = saveWorkspaceInvitation(workspace, invitee.email(),
        WorkspaceRole.MEMBER, admin);

    StepVerifier.create(rejectWorkspaceInvitationUseCase.rejectWorkspaceInvitation(
        new RejectWorkspaceInvitationCommand(invitation.getId(), requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            ProjectErrorCode.INVITATION_EMAIL_MISMATCH))
        .verify();
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

}
