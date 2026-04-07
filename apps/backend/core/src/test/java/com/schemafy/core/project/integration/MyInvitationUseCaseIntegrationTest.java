package com.schemafy.core.project.integration;

import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.CursorResult;
import com.schemafy.core.project.application.port.in.GetMyInvitationsQuery;
import com.schemafy.core.project.application.port.in.GetMyInvitationsUseCase;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.ulid.application.service.UlidGenerator;
import com.schemafy.core.user.domain.User;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("통합 내 초대 유스케이스 통합 테스트")
class MyInvitationUseCaseIntegrationTest extends ProjectDomainIntegrationSupport {

  @Autowired
  private GetMyInvitationsUseCase getMyInvitationsUseCase;

  @Test
  @DisplayName("현재 사용자의 워크스페이스/프로젝트 초대를 ULID 내림차순으로 통합 조회한다")
  void getMyInvitations_returnsMixedInvitationsInUlidDescOrder() {
    User admin = signUpUser("admin-mi@test.com", "Admin");
    User invitee = signUpUser("invitee-mi@test.com", "Invitee");
    Workspace workspace = saveWorkspace("Invitation Workspace", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Project project = saveProject(workspace, "Invitation Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);

    Invitation workspaceInvitation = saveWorkspaceInvitation(workspace,
        invitee.email(), WorkspaceRole.MEMBER, admin);
    Invitation projectInvitation = saveProjectInvitation(project, workspace,
        invitee.email(), ProjectRole.EDITOR, admin);

    CursorResult<Invitation> result = getMyInvitationsUseCase.getMyInvitations(
        new GetMyInvitationsQuery(invitee.id(), null, 5))
        .block();

    assertThat(result.content()).extracting(Invitation::getId)
        .containsExactly(projectInvitation.getId(), workspaceInvitation.getId());
    assertThat(result.size()).isEqualTo(5);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursorId()).isNull();
  }

  @Test
  @DisplayName("cursorId 기준으로 다음 페이지를 이어서 조회한다")
  void getMyInvitations_returnsNextPageByCursor() {
    User admin = signUpUser("admin-mi-cursor@test.com", "Admin");
    User invitee = signUpUser("invitee-mi-cursor@test.com", "Invitee");

    Workspace workspace = saveWorkspace("Cursor Workspace", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);

    Invitation[] invitations = new Invitation[6];
    for (int i = 0; i < 6; i++) {
      Workspace inviteWorkspace = saveWorkspace("Cursor WS " + i, "Description " + i);
      saveWorkspaceMember(inviteWorkspace, admin, WorkspaceRole.ADMIN);
      invitations[i] = saveWorkspaceInvitation(inviteWorkspace,
          invitee.email(), WorkspaceRole.MEMBER, admin);
    }

    CursorResult<Invitation> firstPage = getMyInvitationsUseCase
        .getMyInvitations(new GetMyInvitationsQuery(invitee.id(), null, 3))
        .block();
    CursorResult<Invitation> secondPage = getMyInvitationsUseCase
        .getMyInvitations(new GetMyInvitationsQuery(invitee.id(),
            firstPage.nextCursorId(), 3))
        .block();

    assertThat(firstPage.content()).extracting(Invitation::getId)
        .containsExactly(
            invitations[5].getId(),
            invitations[4].getId(),
            invitations[3].getId());
    assertThat(firstPage.hasNext()).isTrue();
    assertThat(firstPage.nextCursorId()).isEqualTo(invitations[3].getId());

    assertThat(secondPage.content()).extracting(Invitation::getId)
        .containsExactly(
            invitations[2].getId(),
            invitations[1].getId(),
            invitations[0].getId());
    assertThat(secondPage.hasNext()).isFalse();
    assertThat(secondPage.nextCursorId()).isNull();
  }

  @Test
  @DisplayName("소문자 cursorId도 같은 다음 페이지 경계로 정규화한다")
  void getMyInvitations_normalizesLowercaseCursorId() {
    User admin = signUpUser("admin-mi-lowercase@test.com", "Admin");
    User invitee = signUpUser("invitee-mi-lowercase@test.com", "Invitee");

    Workspace workspace = saveWorkspace("Lowercase Cursor Workspace", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);

    Invitation[] invitations = new Invitation[6];
    for (int i = 0; i < 6; i++) {
      Workspace inviteWorkspace = saveWorkspace("Lowercase WS " + i,
          "Description " + i);
      saveWorkspaceMember(inviteWorkspace, admin, WorkspaceRole.ADMIN);
      invitations[i] = saveWorkspaceInvitation(inviteWorkspace,
          invitee.email(), WorkspaceRole.MEMBER, admin);
    }

    CursorResult<Invitation> result = getMyInvitationsUseCase
        .getMyInvitations(new GetMyInvitationsQuery(invitee.id(),
            invitations[3].getId().toLowerCase(Locale.ROOT), 3))
        .block();

    assertThat(result.content()).extracting(Invitation::getId)
        .containsExactly(
            invitations[2].getId(),
            invitations[1].getId(),
            invitations[0].getId());
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursorId()).isNull();
  }

  @Test
  @DisplayName("공백이 섞인 유효한 cursorId도 trim 후 같은 다음 페이지 경계로 정규화한다")
  void getMyInvitations_normalizesTrimmedCursorId() {
    User admin = signUpUser("admin-mi-trimmed@test.com", "Admin");
    User invitee = signUpUser("invitee-mi-trimmed@test.com", "Invitee");

    Workspace workspace = saveWorkspace("Trimmed Cursor Workspace", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);

    Invitation[] invitations = new Invitation[6];
    for (int i = 0; i < 6; i++) {
      Workspace inviteWorkspace = saveWorkspace("Trimmed WS " + i,
          "Description " + i);
      saveWorkspaceMember(inviteWorkspace, admin, WorkspaceRole.ADMIN);
      invitations[i] = saveWorkspaceInvitation(inviteWorkspace,
          invitee.email(), WorkspaceRole.MEMBER, admin);
    }

    CursorResult<Invitation> result = getMyInvitationsUseCase
        .getMyInvitations(new GetMyInvitationsQuery(invitee.id(),
            "  " + invitations[3].getId() + "  ", 3))
        .block();

    assertThat(result.content()).extracting(Invitation::getId)
        .containsExactly(
            invitations[2].getId(),
            invitations[1].getId(),
            invitations[0].getId());
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursorId()).isNull();
  }

  @Test
  @DisplayName("워크스페이스와 프로젝트 초대가 섞여 있어도 다음 페이지를 이어서 조회한다")
  void getMyInvitations_returnsMixedNextPageByCursor() {
    User admin = signUpUser("admin-mi-mixed-cursor@test.com", "Admin");
    User invitee = signUpUser("invitee-mi-mixed-cursor@test.com", "Invitee");

    Invitation[] invitations = new Invitation[6];
    for (int i = 0; i < invitations.length; i++) {
      if (i % 2 == 0) {
        Workspace inviteWorkspace = saveWorkspace("Mixed WS " + i,
            "Description " + i);
        saveWorkspaceMember(inviteWorkspace, admin, WorkspaceRole.ADMIN);
        invitations[i] = saveWorkspaceInvitation(inviteWorkspace,
            invitee.email(), WorkspaceRole.MEMBER, admin);
      } else {
        Workspace inviteWorkspace = saveWorkspace("Mixed Project WS " + i,
            "Description " + i);
        saveWorkspaceMember(inviteWorkspace, admin, WorkspaceRole.ADMIN);
        Project project = saveProject(inviteWorkspace, "Mixed Project " + i);
        saveProjectMember(project, admin, ProjectRole.ADMIN);
        invitations[i] = saveProjectInvitation(project, inviteWorkspace,
            invitee.email(), ProjectRole.EDITOR, admin);
      }
    }

    CursorResult<Invitation> firstPage = getMyInvitationsUseCase
        .getMyInvitations(new GetMyInvitationsQuery(invitee.id(), null, 4))
        .block();
    CursorResult<Invitation> secondPage = getMyInvitationsUseCase
        .getMyInvitations(new GetMyInvitationsQuery(invitee.id(),
            firstPage.nextCursorId(), 4))
        .block();

    assertThat(firstPage.content()).extracting(Invitation::getId)
        .containsExactly(
            invitations[5].getId(),
            invitations[4].getId(),
            invitations[3].getId(),
            invitations[2].getId());
    assertThat(firstPage.hasNext()).isTrue();
    assertThat(firstPage.nextCursorId()).isEqualTo(invitations[2].getId());

    assertThat(secondPage.content()).extracting(Invitation::getId)
        .containsExactly(
            invitations[1].getId(),
            invitations[0].getId());
    assertThat(secondPage.hasNext()).isFalse();
    assertThat(secondPage.nextCursorId()).isNull();
  }

  @Test
  @DisplayName("pending가 아니거나 만료되거나 삭제된 초대는 제외한다")
  void getMyInvitations_excludesNonPendingExpiredAndDeletedInvitations() {
    User admin = signUpUser("admin-mi-filter@test.com", "Admin");
    User invitee = signUpUser("invitee-mi-filter@test.com", "Invitee");
    Workspace workspace = saveWorkspace("Filter Workspace", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Project project = saveProject(workspace, "Filter Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);

    Invitation pendingInvitation = saveWorkspaceInvitation(workspace,
        invitee.email(), WorkspaceRole.MEMBER, admin);
    acceptInvitation(saveProjectInvitation(project, workspace,
        invitee.email(), ProjectRole.VIEWER, admin));

    expireInvitation(saveWorkspaceInvitation(saveWorkspace(
        "Expired Workspace", "Description"), invitee.email(),
        WorkspaceRole.MEMBER, admin), Instant.now().minusSeconds(60));

    softDeleteInvitation(saveWorkspaceInvitation(saveWorkspace(
        "Deleted Workspace", "Description"), invitee.email(),
        WorkspaceRole.MEMBER, admin));

    CursorResult<Invitation> result = getMyInvitationsUseCase.getMyInvitations(
        new GetMyInvitationsQuery(invitee.id(), UlidGenerator.generate(), 10))
        .block();

    assertThat(result.content()).extracting(Invitation::getId)
        .containsExactly(pendingInvitation.getId());
  }

}
