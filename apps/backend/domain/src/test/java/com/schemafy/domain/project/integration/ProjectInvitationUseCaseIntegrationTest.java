package com.schemafy.domain.project.integration;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.common.PageResult;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.project.application.port.in.AcceptProjectInvitationCommand;
import com.schemafy.domain.project.application.port.in.AcceptProjectInvitationUseCase;
import com.schemafy.domain.project.application.port.in.CreateProjectInvitationCommand;
import com.schemafy.domain.project.application.port.in.CreateProjectInvitationUseCase;
import com.schemafy.domain.project.application.port.in.GetMyProjectInvitationsQuery;
import com.schemafy.domain.project.application.port.in.GetMyProjectInvitationsUseCase;
import com.schemafy.domain.project.application.port.in.GetProjectInvitationsQuery;
import com.schemafy.domain.project.application.port.in.GetProjectInvitationsUseCase;
import com.schemafy.domain.project.application.port.in.RejectProjectInvitationCommand;
import com.schemafy.domain.project.application.port.in.RejectProjectInvitationUseCase;
import com.schemafy.domain.project.domain.Invitation;
import com.schemafy.domain.project.domain.InvitationStatus;
import com.schemafy.domain.project.domain.ProjectMember;
import com.schemafy.domain.project.domain.ProjectRole;
import com.schemafy.domain.project.domain.WorkspaceRole;
import com.schemafy.domain.project.domain.exception.ProjectErrorCode;
import com.schemafy.domain.user.domain.User;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Project invitation usecase integration")
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
  @DisplayName("createProjectInvitation is visible in target and my invitation lists")
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
  @DisplayName("createProjectInvitation rejects users who are already project members")
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
  @DisplayName("acceptProjectInvitation restores soft-deleted membership and cancels siblings")
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
  @DisplayName("rejectProjectInvitation marks invitation as rejected")
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

}
