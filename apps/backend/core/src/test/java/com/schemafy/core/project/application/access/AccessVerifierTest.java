package com.schemafy.core.project.application.access;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessVerifier")
class AccessVerifierTest {

  private static final String WORKSPACE_ID = "workspace-id";
  private static final String PROJECT_ID = "project-id";
  private static final String USER_ID = "user-id";

  @Mock
  private WorkspaceMemberPort workspaceMemberPort;

  @Mock
  private ProjectMemberPort projectMemberPort;

  @InjectMocks
  private AccessVerifier accessVerifier;

  @Nested
  @DisplayName("requireProjectAccess")
  class RequireProjectAccessTest {

    @Test
    @DisplayName("VIEWER 권한은 프로젝트 멤버면 허용한다")
    void viewerAccess_allowsMember() {
      when(projectMemberPort.findByProjectIdAndUserIdAndNotDeleted(PROJECT_ID, USER_ID))
          .thenReturn(Mono.just(ProjectMember.create("member-id", PROJECT_ID, USER_ID,
              ProjectRole.VIEWER)));

      StepVerifier.create(
          accessVerifier.requireProjectAccess(PROJECT_ID, USER_ID, ProjectRole.VIEWER))
          .verifyComplete();
    }

    @Test
    @DisplayName("VIEWER 권한은 비회원이면 ACCESS_DENIED를 반환한다")
    void viewerAccess_deniesNonMember() {
      when(projectMemberPort.findByProjectIdAndUserIdAndNotDeleted(PROJECT_ID, USER_ID))
          .thenReturn(Mono.empty());

      StepVerifier.create(
          accessVerifier.requireProjectAccess(PROJECT_ID, USER_ID, ProjectRole.VIEWER))
          .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ACCESS_DENIED))
          .verify();
    }

    @Test
    @DisplayName("EDITOR 권한은 VIEWER를 거부한다")
    void editorAccess_deniesViewer() {
      when(projectMemberPort.findByProjectIdAndUserIdAndNotDeleted(PROJECT_ID, USER_ID))
          .thenReturn(Mono.just(ProjectMember.create("member-id", PROJECT_ID, USER_ID,
              ProjectRole.VIEWER)));

      StepVerifier.create(
          accessVerifier.requireProjectAccess(PROJECT_ID, USER_ID, ProjectRole.EDITOR))
          .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ACCESS_DENIED))
          .verify();
    }

    @Test
    @DisplayName("EDITOR 권한은 EDITOR를 허용한다")
    void editorAccess_allowsEditor() {
      when(projectMemberPort.findByProjectIdAndUserIdAndNotDeleted(PROJECT_ID, USER_ID))
          .thenReturn(Mono.just(ProjectMember.create("member-id", PROJECT_ID, USER_ID,
              ProjectRole.EDITOR)));

      StepVerifier.create(
          accessVerifier.requireProjectAccess(PROJECT_ID, USER_ID, ProjectRole.EDITOR))
          .verifyComplete();
    }

    @Test
    @DisplayName("ADMIN 권한은 비회원이면 ACCESS_DENIED를 반환한다")
    void adminAccess_deniesNonMember() {
      when(projectMemberPort.findByProjectIdAndUserIdAndNotDeleted(PROJECT_ID, USER_ID))
          .thenReturn(Mono.empty());

      StepVerifier.create(
          accessVerifier.requireProjectAccess(PROJECT_ID, USER_ID, ProjectRole.ADMIN))
          .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ACCESS_DENIED))
          .verify();
    }

    @Test
    @DisplayName("ADMIN 권한은 비관리자 멤버면 ADMIN_REQUIRED를 반환한다")
    void adminAccess_requiresAdmin() {
      when(projectMemberPort.findByProjectIdAndUserIdAndNotDeleted(PROJECT_ID, USER_ID))
          .thenReturn(Mono.just(ProjectMember.create("member-id", PROJECT_ID, USER_ID,
              ProjectRole.EDITOR)));

      StepVerifier.create(
          accessVerifier.requireProjectAccess(PROJECT_ID, USER_ID, ProjectRole.ADMIN))
          .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ADMIN_REQUIRED))
          .verify();
    }

    @Test
    @DisplayName("ADMIN 권한은 관리자면 허용한다")
    void adminAccess_allowsAdmin() {
      when(projectMemberPort.findByProjectIdAndUserIdAndNotDeleted(PROJECT_ID, USER_ID))
          .thenReturn(Mono.just(ProjectMember.create("member-id", PROJECT_ID, USER_ID,
              ProjectRole.ADMIN)));

      StepVerifier.create(
          accessVerifier.requireProjectAccess(PROJECT_ID, USER_ID, ProjectRole.ADMIN))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("requireWorkspaceAccess")
  class RequireWorkspaceAccessTest {

    @Test
    @DisplayName("MEMBER 권한은 워크스페이스 멤버면 허용한다")
    void memberAccess_allowsMember() {
      when(workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(WORKSPACE_ID, USER_ID))
          .thenReturn(Mono.just(WorkspaceMember.create("member-id", WORKSPACE_ID, USER_ID,
              WorkspaceRole.MEMBER)));

      StepVerifier.create(
          accessVerifier.requireWorkspaceAccess(WORKSPACE_ID, USER_ID, WorkspaceRole.MEMBER))
          .verifyComplete();
    }

    @Test
    @DisplayName("MEMBER 권한은 상위 권한 멤버도 허용한다")
    void memberAccess_allowsAdmin() {
      when(workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(WORKSPACE_ID, USER_ID))
          .thenReturn(Mono.just(WorkspaceMember.create("member-id", WORKSPACE_ID, USER_ID,
              WorkspaceRole.ADMIN)));

      StepVerifier.create(
          accessVerifier.requireWorkspaceAccess(WORKSPACE_ID, USER_ID, WorkspaceRole.MEMBER))
          .verifyComplete();
    }

    @Test
    @DisplayName("MEMBER 권한은 비회원이면 ACCESS_DENIED를 반환한다")
    void memberAccess_deniesNonMember() {
      when(workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(WORKSPACE_ID, USER_ID))
          .thenReturn(Mono.empty());

      StepVerifier.create(
          accessVerifier.requireWorkspaceAccess(WORKSPACE_ID, USER_ID, WorkspaceRole.MEMBER))
          .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
          .verify();
    }

    @Test
    @DisplayName("ADMIN 권한은 비회원이면 ACCESS_DENIED를 반환한다")
    void adminAccess_deniesNonMember() {
      when(workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(WORKSPACE_ID, USER_ID))
          .thenReturn(Mono.empty());

      StepVerifier.create(
          accessVerifier.requireWorkspaceAccess(WORKSPACE_ID, USER_ID, WorkspaceRole.ADMIN))
          .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
          .verify();
    }

    @Test
    @DisplayName("ADMIN 권한은 비관리자면 ADMIN_REQUIRED를 반환한다")
    void adminAccess_requiresAdmin() {
      when(workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(WORKSPACE_ID, USER_ID))
          .thenReturn(Mono.just(WorkspaceMember.create("member-id", WORKSPACE_ID, USER_ID,
              WorkspaceRole.MEMBER)));

      StepVerifier.create(
          accessVerifier.requireWorkspaceAccess(WORKSPACE_ID, USER_ID, WorkspaceRole.ADMIN))
          .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ADMIN_REQUIRED))
          .verify();
    }

    @Test
    @DisplayName("ADMIN 권한은 관리자면 허용한다")
    void adminAccess_allowsAdmin() {
      when(workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(WORKSPACE_ID, USER_ID))
          .thenReturn(Mono.just(WorkspaceMember.create("member-id", WORKSPACE_ID, USER_ID,
              WorkspaceRole.ADMIN)));

      StepVerifier.create(
          accessVerifier.requireWorkspaceAccess(WORKSPACE_ID, USER_ID, WorkspaceRole.ADMIN))
          .verifyComplete();
    }

  }

}
