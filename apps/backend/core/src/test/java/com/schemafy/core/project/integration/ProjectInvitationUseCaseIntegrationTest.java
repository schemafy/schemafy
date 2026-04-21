package com.schemafy.core.project.integration;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationUseCase;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationUseCase;
import com.schemafy.core.project.application.port.in.GetMyProjectInvitationsQuery;
import com.schemafy.core.project.application.port.in.GetMyProjectInvitationsUseCase;
import com.schemafy.core.project.application.port.in.GetProjectInvitationsQuery;
import com.schemafy.core.project.application.port.in.GetProjectInvitationsUseCase;
import com.schemafy.core.project.application.port.in.RejectProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.RejectProjectInvitationUseCase;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.user.domain.User;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("프로젝트 초대 유스케이스 통합 테스트")
class ProjectInvitationUseCaseIntegrationTest extends ProjectDomainIntegrationSupport {

  @Autowired
  private CreateProjectInvitationUseCase createProjectInvitationUseCase;

  @Autowired
  private GetProjectInvitationsUseCase getProjectInvitationsUseCase;

  @Autowired
  private GetMyProjectInvitationsUseCase getMyProjectInvitationsUseCase;

  @Autowired
  private AcceptProjectInvitationUseCase acceptProjectInvitationUseCase;

  @Autowired
  private RejectProjectInvitationUseCase rejectProjectInvitationUseCase;

  @Test
  @DisplayName("프로젝트 초대 생성 후 대상 목록과 내 초대 목록에서 모두 조회된다")
  void createProjectInvitation_listsForAdminAndInvitee() {
    User admin = signUpUser("admin-pi@test.com", "Admin");
    User invitee = signUpUser("invitee-pi@test.com", "Invitee");
    var workspace = saveWorkspace("Project Invitation WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, invitee, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Invitation Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);

    Invitation invitation = createProjectInvitationUseCase.createProjectInvitation(
        new CreateProjectInvitationCommand(project.getId(), invitee.email(), ProjectRole.EDITOR,
            admin.id()))
        .block();

    PageResult<Invitation> targetInvitations = getProjectInvitationsUseCase.getProjectInvitations(
        new GetProjectInvitationsQuery(project.getId(), admin.id(), 0, 10))
        .block();
    PageResult<Invitation> myInvitations = getMyProjectInvitationsUseCase.getMyProjectInvitations(
        new GetMyProjectInvitationsQuery(invitee.id(), 0, 10))
        .block();

    assertThat(invitation).isNotNull();
    assertThat(targetInvitations.content()).extracting(Invitation::getId)
        .contains(invitation.getId());
    assertThat(myInvitations.content()).extracting(Invitation::getId)
        .contains(invitation.getId());
  }

  @Test
  @DisplayName("프로젝트 초대 생성 시 이미 프로젝트 멤버인 사용자는 거부한다")
  void createProjectInvitation_rejectsDuplicateMembership() {
    User admin = signUpUser("admin-pi-dup@test.com", "Admin");
    User invitee = signUpUser("invitee-pi-dup@test.com", "Invitee");
    var workspace = saveWorkspace("Dup WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, invitee, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Dup Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    saveProjectMember(project, invitee, ProjectRole.VIEWER);

    StepVerifier.create(createProjectInvitationUseCase.createProjectInvitation(
        new CreateProjectInvitationCommand(project.getId(), invitee.email(), ProjectRole.EDITOR,
            admin.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT))
        .verify();
  }

  @Test
  @DisplayName("프로젝트 초대 생성 시 대문자 중복 이메일도 같은 초대로 처리한다")
  void createProjectInvitation_duplicatePending_caseInsensitive() {
    User admin = signUpUser("admin-pi-case@test.com", "Admin");
    User invitee = signUpUser("invitee-pi-case@test.com", "Invitee");
    var workspace = saveWorkspace("Case Project WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, invitee, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Case Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);

    createProjectInvitationUseCase.createProjectInvitation(
        new CreateProjectInvitationCommand(project.getId(), invitee.email(),
            ProjectRole.EDITOR, admin.id()))
        .block();

    StepVerifier.create(createProjectInvitationUseCase.createProjectInvitation(
        new CreateProjectInvitationCommand(project.getId(), "INVITEE-PI-CASE@TEST.COM",
            ProjectRole.EDITOR, admin.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            ProjectErrorCode.INVITATION_ALREADY_EXISTS))
        .verify();
  }

  @Test
  @DisplayName("프로젝트 초대 생성 시 권한 없는 요청자는 이메일 형식 검증보다 먼저 거부한다")
  void createProjectInvitation_rejectsUnauthorizedRequesterBeforeInvalidEmail() {
    User admin = signUpUser("admin-pi-auth-order@test.com", "Admin");
    User requester = signUpUser("requester-pi-auth-order@test.com", "Requester");
    var workspace = saveWorkspace("Auth Order Project WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Auth Order Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);

    StepVerifier.create(createProjectInvitationUseCase.createProjectInvitation(
        new CreateProjectInvitationCommand(project.getId(), "not-an-email",
            ProjectRole.EDITOR, requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ACCESS_DENIED))
        .verify();
  }

  @Test
  @DisplayName("프로젝트 초대 목록 조회 시 프로젝트 관리자가 아닌 요청자는 거부한다")
  void getProjectInvitations_rejectsNonAdminRequester() {
    User admin = signUpUser("admin-pi-list-auth@test.com", "Admin");
    User viewer = signUpUser("viewer-pi-list-auth@test.com", "Viewer");
    var workspace = saveWorkspace("List Auth Project WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, viewer, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "List Auth Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    saveProjectMember(project, viewer, ProjectRole.VIEWER);

    StepVerifier.create(getProjectInvitationsUseCase.getProjectInvitations(
        new GetProjectInvitationsQuery(project.getId(), viewer.id(), 0, 10)))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("프로젝트 초대 수락 시 soft delete 된 멤버십을 복원하고 형제 초대를 취소한다")
  void acceptProjectInvitation_restoresMembership() {
    User admin = signUpUser("admin-pi-restore@test.com", "Admin");
    User invitee = signUpUser("invitee-pi-restore@test.com", "Invitee");
    var workspace = saveWorkspace("Restore Project WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, invitee, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Restore Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);

    ProjectMember deletedMember = saveProjectMember(project, invitee, ProjectRole.VIEWER);
    softDeleteProjectMember(deletedMember.getId());

    Invitation acceptedInvitation = saveProjectInvitation(project, workspace, invitee.email(),
        ProjectRole.EDITOR, admin);
    Invitation siblingInvitation = saveProjectInvitation(project, workspace, invitee.email(),
        ProjectRole.VIEWER, admin);

    ProjectMember restoredMember = acceptProjectInvitationUseCase.acceptProjectInvitation(
        new AcceptProjectInvitationCommand(acceptedInvitation.getId(), invitee.id()))
        .block();

    Invitation cancelledSibling = invitationRepository.findById(siblingInvitation.getId()).block();

    assertThat(restoredMember.getId()).isEqualTo(deletedMember.getId());
    assertThat(restoredMember.getRoleAsEnum()).isEqualTo(ProjectRole.EDITOR);
    assertThat(cancelledSibling.getStatusAsEnum()).isEqualTo(InvitationStatus.CANCELLED);
  }

  @Test
  @DisplayName("프로젝트 초대 수락 시 초대 이메일과 다른 요청자는 거부한다")
  void acceptProjectInvitation_rejectsRequesterWithDifferentEmail() {
    User admin = signUpUser("admin-pi-accept-owner@test.com", "Admin");
    User invitee = signUpUser("invitee-pi-accept-owner@test.com", "Invitee");
    User requester = signUpUser("requester-pi-accept-owner@test.com", "Requester");
    var workspace = saveWorkspace("Accept Owner Project WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, invitee, WorkspaceRole.MEMBER);
    saveWorkspaceMember(workspace, requester, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Accept Owner Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    Invitation invitation = saveProjectInvitation(project, workspace, invitee.email(),
        ProjectRole.VIEWER, admin);

    StepVerifier.create(acceptProjectInvitationUseCase.acceptProjectInvitation(
        new AcceptProjectInvitationCommand(invitation.getId(), requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            ProjectErrorCode.INVITATION_EMAIL_MISMATCH))
        .verify();
  }

  @Test
  @DisplayName("프로젝트 초대 거절 시 초대 상태를 rejected로 바꾼다")
  void rejectProjectInvitation_marksResolved() {
    User admin = signUpUser("admin-pi-reject@test.com", "Admin");
    User invitee = signUpUser("invitee-pi-reject@test.com", "Invitee");
    var workspace = saveWorkspace("Reject Project WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, invitee, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Reject Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    Invitation invitation = saveProjectInvitation(project, workspace, invitee.email(),
        ProjectRole.VIEWER, admin);

    rejectProjectInvitationUseCase.rejectProjectInvitation(
        new RejectProjectInvitationCommand(invitation.getId(), invitee.id()))
        .block();

    Invitation rejected = invitationRepository.findById(invitation.getId()).block();
    assertThat(rejected.getStatusAsEnum()).isEqualTo(InvitationStatus.REJECTED);
    assertThat(rejected.getResolvedAt()).isNotNull();
  }

  @Test
  @DisplayName("프로젝트 초대 거절 시 초대 이메일과 다른 요청자는 거부한다")
  void rejectProjectInvitation_rejectsRequesterWithDifferentEmail() {
    User admin = signUpUser("admin-pi-reject-owner@test.com", "Admin");
    User invitee = signUpUser("invitee-pi-reject-owner@test.com", "Invitee");
    User requester = signUpUser("requester-pi-reject-owner@test.com", "Requester");
    var workspace = saveWorkspace("Reject Owner Project WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, invitee, WorkspaceRole.MEMBER);
    saveWorkspaceMember(workspace, requester, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Reject Owner Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    Invitation invitation = saveProjectInvitation(project, workspace, invitee.email(),
        ProjectRole.VIEWER, admin);

    StepVerifier.create(rejectProjectInvitationUseCase.rejectProjectInvitation(
        new RejectProjectInvitationCommand(invitation.getId(), requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            ProjectErrorCode.INVITATION_EMAIL_MISMATCH))
        .verify();
  }

}
