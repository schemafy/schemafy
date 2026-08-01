package com.schemafy.core.project.application.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.project.application.service.MutationGuardTestSupport.invokeGuardAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("워크스페이스 초대 수락 서비스")
class AcceptWorkspaceInvitationServiceTest {

  private static final String WORKSPACE_ID = "workspace-id";
  private static final String INVITATION_ID = "invitation-id";
  private static final String USER_ID = "user-id";

  @Mock
  WorkspaceMutationGuard workspaceMutationGuard;

  @Mock
  InvitationPort invitationPort;

  @Mock
  WorkspaceInvitationHelper workspaceInvitationHelper;

  @Mock
  ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @Mock
  FindUserByIdPort findUserByIdPort;

  @InjectMocks
  AcceptWorkspaceInvitationService sut;

  @Test
  @DisplayName("워크스페이스 배타 락을 획득한 트랜잭션에서 초대를 다시 읽고 최신 상태로 멤버십을 만든다")
  void rereadsInvitationAfterAcquiringExclusiveWorkspaceLock() {
    var command = new AcceptWorkspaceInvitationCommand(INVITATION_ID, USER_ID);
    var user = User.signUp(USER_ID, "invitee@test.com", "Invitee", "password");
    var preLockInvitation = invitation();
    var freshInvitation = invitation();
    var savedMember = WorkspaceMember.create("member-id", WORKSPACE_ID, USER_ID,
        WorkspaceRole.MEMBER);
    var enteredGuard = new AtomicBoolean();

    given(findUserByIdPort.findUserById(USER_ID)).willReturn(Mono.just(user));
    given(workspaceInvitationHelper.findInvitationOrThrow(INVITATION_ID))
        .willReturn(Mono.just(preLockInvitation), Mono.just(freshInvitation));
    given(workspaceMutationGuard.protectExclusive(eq(WORKSPACE_ID), any()))
        .willAnswer(invocation -> {
          enteredGuard.set(true);
          Supplier<Mono<WorkspaceMember>> action = invocation.getArgument(1);
          return action.get();
        });
    given(workspaceInvitationHelper.findWorkspaceOrThrow(WORKSPACE_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.just(Workspace.create(WORKSPACE_ID, "Workspace", "Description"));
        });
    given(workspaceInvitationHelper.checkNotAlreadyMember(WORKSPACE_ID, USER_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.empty();
        });
    given(invitationPort.save(any(Invitation.class)))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          assertThat((Invitation) invocation.getArgument(0)).isSameAs(freshInvitation);
          return Mono.just(invocation.getArgument(0));
        });
    given(invitationPort.updateStatusByTargetAndEmail(any(), any(), any(), any(), any(), any()))
        .willReturn(Mono.just(0L));
    given(workspaceInvitationHelper.saveOrRestoreWorkspaceMember(
        WORKSPACE_ID, USER_ID, WorkspaceRole.MEMBER)).willReturn(Mono.just(savedMember));
    given(projectMembershipPropagationHelper.syncProjectMembershipsForWorkspaceRole(
        WORKSPACE_ID, USER_ID, WorkspaceRole.MEMBER)).willReturn(Mono.empty());

    StepVerifier.create(sut.acceptWorkspaceInvitation(command))
        .expectNext(savedMember)
        .verifyComplete();

    then(workspaceMutationGuard).should().protectExclusive(eq(WORKSPACE_ID), any());
    then(workspaceInvitationHelper).should(org.mockito.Mockito.times(2))
        .findInvitationOrThrow(INVITATION_ID);
  }

  @Test
  @DisplayName("프로젝트 초대를 워크스페이스 수락으로 처리하면 워크스페이스 배타 락을 요청하지 않고 타입 불일치 오류를 반환한다")
  void rejectsProjectInvitationBeforeAcquiringWorkspaceLock() {
    var command = new AcceptWorkspaceInvitationCommand(INVITATION_ID, USER_ID);
    var user = User.signUp(USER_ID, "invitee@test.com", "Invitee", "password");
    var projectInvitation = Invitation.createProjectInvitation(
        INVITATION_ID, "project-id", WORKSPACE_ID, "invitee@test.com",
        com.schemafy.core.project.domain.ProjectRole.EDITOR, "admin-id");

    given(findUserByIdPort.findUserById(USER_ID)).willReturn(Mono.just(user));
    given(workspaceInvitationHelper.findInvitationOrThrow(INVITATION_ID))
        .willReturn(Mono.just(projectInvitation));

    StepVerifier.create(sut.acceptWorkspaceInvitation(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ProjectErrorCode.INVITATION_TYPE_MISMATCH);
        })
        .verify();

    then(workspaceMutationGuard).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("워크스페이스 멤버 저장의 무결성 위반을 중복 멤버 오류로 변환한다")
  void mapsDuplicateWorkspaceMemberIntegrityViolation() {
    prepareAcceptanceWithinWorkspaceLock(new DataIntegrityViolationException("duplicate"));

    StepVerifier.create(sut.acceptWorkspaceInvitation(command()))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER);
        })
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 초대 저장이 계속 충돌하면 3회 재시도 후 INVITATION_CONCURRENT_PROCESSED로 변환한다")
  void mapsOptimisticLockFailureAfterRetries() {
    var attempts = new AtomicInteger();
    var user = User.signUp(USER_ID, "invitee@test.com", "Invitee", "password");
    given(findUserByIdPort.findUserById(USER_ID)).willReturn(Mono.just(user));
    given(workspaceInvitationHelper.findInvitationOrThrow(INVITATION_ID))
        .willAnswer(invocation -> Mono.just(invitation()));
    given(workspaceMutationGuard.protectExclusive(eq(WORKSPACE_ID), any()))
        .willAnswer(invokeGuardAction());
    given(workspaceInvitationHelper.findWorkspaceOrThrow(WORKSPACE_ID))
        .willReturn(Mono.just(Workspace.create(WORKSPACE_ID, "Workspace", "Description")));
    given(workspaceInvitationHelper.checkNotAlreadyMember(WORKSPACE_ID, USER_ID))
        .willReturn(Mono.empty());
    given(invitationPort.save(any(Invitation.class))).willAnswer(invocation -> {
      attempts.incrementAndGet();
      return Mono.error(new OptimisticLockingFailureException("conflict"));
    });
    given(invitationPort.updateStatusByTargetAndEmail(any(), any(), any(), any(), any(), any()))
        .willReturn(Mono.just(0L));
    given(workspaceInvitationHelper.saveOrRestoreWorkspaceMember(
        WORKSPACE_ID, USER_ID, WorkspaceRole.MEMBER))
        .willReturn(Mono.just(WorkspaceMember.create(
            "member-id", WORKSPACE_ID, USER_ID, WorkspaceRole.MEMBER)));

    StepVerifier.create(sut.acceptWorkspaceInvitation(command()))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ProjectErrorCode.INVITATION_CONCURRENT_PROCESSED);
        })
        .verify();

    assertThat(attempts).hasValue(4);
  }

  private void prepareAcceptanceWithinWorkspaceLock(Throwable memberSaveError) {
    var user = User.signUp(USER_ID, "invitee@test.com", "Invitee", "password");
    given(findUserByIdPort.findUserById(USER_ID)).willReturn(Mono.just(user));
    given(workspaceInvitationHelper.findInvitationOrThrow(INVITATION_ID))
        .willAnswer(invocation -> Mono.just(invitation()));
    given(workspaceMutationGuard.protectExclusive(eq(WORKSPACE_ID), any()))
        .willAnswer(invokeGuardAction());
    given(workspaceInvitationHelper.findWorkspaceOrThrow(WORKSPACE_ID))
        .willReturn(Mono.just(Workspace.create(WORKSPACE_ID, "Workspace", "Description")));
    given(workspaceInvitationHelper.checkNotAlreadyMember(WORKSPACE_ID, USER_ID))
        .willReturn(Mono.empty());
    given(invitationPort.save(any(Invitation.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    given(invitationPort.updateStatusByTargetAndEmail(any(), any(), any(), any(), any(), any()))
        .willReturn(Mono.just(0L));
    given(workspaceInvitationHelper.saveOrRestoreWorkspaceMember(
        WORKSPACE_ID, USER_ID, WorkspaceRole.MEMBER))
        .willReturn(Mono.error(memberSaveError));
  }

  private AcceptWorkspaceInvitationCommand command() {
    return new AcceptWorkspaceInvitationCommand(INVITATION_ID, USER_ID);
  }

  private Invitation invitation() {
    return Invitation.createWorkspaceInvitation(INVITATION_ID, WORKSPACE_ID,
        "invitee@test.com", WorkspaceRole.MEMBER, "admin-id");
  }

}
