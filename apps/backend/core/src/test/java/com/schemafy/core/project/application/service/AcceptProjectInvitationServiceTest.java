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
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationCommand;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
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
@DisplayName("프로젝트 초대 수락 서비스 테스트")
class AcceptProjectInvitationServiceTest {

  private static final String PROJECT_ID = "project-id";
  private static final String WORKSPACE_ID = "workspace-id";
  private static final String INVITATION_ID = "invitation-id";
  private static final String USER_ID = "user-id";

  @Mock
  ProjectMutationGuard projectMutationGuard;

  @Mock
  InvitationPort invitationPort;

  @Mock
  ProjectInvitationHelper projectInvitationHelper;

  @Mock
  FindUserByIdPort findUserByIdPort;

  @InjectMocks
  AcceptProjectInvitationService sut;

  @Test
  @DisplayName("프로젝트 공유 락을 획득한 트랜잭션에서 초대를 다시 읽고 최신 상태로 멤버십을 만든다")
  void rereadsInvitationAfterAcquiringSharedProjectLock() {
    var command = command();
    var user = user();
    var lockKeyInvitation = invitation();
    var freshInvitation = invitation();
    var savedMember = ProjectMember.create("member-id", PROJECT_ID, USER_ID,
        ProjectRole.EDITOR);
    var enteredGuard = new AtomicBoolean();

    given(findUserByIdPort.findUserById(USER_ID)).willReturn(Mono.just(user));
    given(projectInvitationHelper.findInvitationOrThrow(INVITATION_ID))
        .willReturn(Mono.just(lockKeyInvitation), Mono.just(freshInvitation));
    given(projectMutationGuard.protectChildCreation(eq(PROJECT_ID), any()))
        .willAnswer(invocation -> {
          enteredGuard.set(true);
          Supplier<Mono<ProjectMember>> action = invocation.getArgument(1);
          return action.get();
        });
    given(projectInvitationHelper.findProjectOrThrow(PROJECT_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.just(Project.create(PROJECT_ID, WORKSPACE_ID, "Project", "Description"));
        });
    given(projectInvitationHelper.checkNotAlreadyProjectMember(PROJECT_ID, USER_ID))
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
    given(projectInvitationHelper.saveOrRestoreProjectMember(
        PROJECT_ID, USER_ID, ProjectRole.EDITOR)).willReturn(Mono.just(savedMember));

    StepVerifier.create(sut.acceptProjectInvitation(command))
        .expectNext(savedMember)
        .verifyComplete();

    then(projectMutationGuard).should().protectChildCreation(eq(PROJECT_ID), any());
    then(projectInvitationHelper).should(org.mockito.Mockito.times(2))
        .findInvitationOrThrow(INVITATION_ID);
  }

  @Test
  @DisplayName("워크스페이스 초대를 프로젝트 수락으로 처리하면 프로젝트 공유 락을 요청하지 않고 타입 불일치 오류를 반환한다")
  void rejectsWorkspaceInvitationBeforeAcquiringProjectLock() {
    given(findUserByIdPort.findUserById(USER_ID)).willReturn(Mono.just(user()));
    given(projectInvitationHelper.findInvitationOrThrow(INVITATION_ID))
        .willReturn(Mono.just(Invitation.createWorkspaceInvitation(
            INVITATION_ID, WORKSPACE_ID, "invitee@test.com", WorkspaceRole.MEMBER,
            "admin-id")));

    StepVerifier.create(sut.acceptProjectInvitation(command()))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ProjectErrorCode.INVITATION_TYPE_MISMATCH);
        })
        .verify();

    then(projectMutationGuard).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("프로젝트 멤버 저장의 무결성 위반을 중복 멤버 오류로 변환한다")
  void mapsDuplicateProjectMemberIntegrityViolation() {
    prepareAcceptanceWithinProjectLock(new DataIntegrityViolationException("duplicate"));

    StepVerifier.create(sut.acceptProjectInvitation(command()))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT);
        })
        .verify();
  }

  @Test
  @DisplayName("프로젝트 초대 저장이 계속 충돌하면 3회 재시도 후 INVITATION_CONCURRENT_PROCESSED로 변환한다")
  void mapsOptimisticLockFailureAfterRetries() {
    var attempts = new AtomicInteger();
    given(findUserByIdPort.findUserById(USER_ID)).willReturn(Mono.just(user()));
    given(projectInvitationHelper.findInvitationOrThrow(INVITATION_ID))
        .willAnswer(invocation -> Mono.just(invitation()));
    given(projectMutationGuard.protectChildCreation(eq(PROJECT_ID), any()))
        .willAnswer(invokeGuardAction());
    given(projectInvitationHelper.findProjectOrThrow(PROJECT_ID))
        .willReturn(Mono.just(Project.create(PROJECT_ID, WORKSPACE_ID, "Project", "Description")));
    given(projectInvitationHelper.checkNotAlreadyProjectMember(PROJECT_ID, USER_ID))
        .willReturn(Mono.empty());
    given(invitationPort.save(any(Invitation.class))).willAnswer(invocation -> {
      attempts.incrementAndGet();
      return Mono.error(new OptimisticLockingFailureException("conflict"));
    });
    given(invitationPort.updateStatusByTargetAndEmail(any(), any(), any(), any(), any(), any()))
        .willReturn(Mono.just(0L));
    given(projectInvitationHelper.saveOrRestoreProjectMember(
        PROJECT_ID, USER_ID, ProjectRole.EDITOR)).willReturn(Mono.just(
            ProjectMember.create("member-id", PROJECT_ID, USER_ID, ProjectRole.EDITOR)));

    StepVerifier.create(sut.acceptProjectInvitation(command()))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ProjectErrorCode.INVITATION_CONCURRENT_PROCESSED);
        })
        .verify();

    assertThat(attempts).hasValue(4);
  }

  private void prepareAcceptanceWithinProjectLock(Throwable memberSaveError) {
    given(findUserByIdPort.findUserById(USER_ID)).willReturn(Mono.just(user()));
    given(projectInvitationHelper.findInvitationOrThrow(INVITATION_ID))
        .willAnswer(invocation -> Mono.just(invitation()));
    given(projectMutationGuard.protectChildCreation(eq(PROJECT_ID), any()))
        .willAnswer(invokeGuardAction());
    given(projectInvitationHelper.findProjectOrThrow(PROJECT_ID))
        .willReturn(Mono.just(Project.create(PROJECT_ID, WORKSPACE_ID, "Project", "Description")));
    given(projectInvitationHelper.checkNotAlreadyProjectMember(PROJECT_ID, USER_ID))
        .willReturn(Mono.empty());
    given(invitationPort.save(any(Invitation.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    given(invitationPort.updateStatusByTargetAndEmail(any(), any(), any(), any(), any(), any()))
        .willReturn(Mono.just(0L));
    given(projectInvitationHelper.saveOrRestoreProjectMember(
        PROJECT_ID, USER_ID, ProjectRole.EDITOR))
        .willReturn(Mono.error(memberSaveError));
  }

  private AcceptProjectInvitationCommand command() {
    return new AcceptProjectInvitationCommand(INVITATION_ID, USER_ID);
  }

  private User user() {
    return User.signUp(USER_ID, "invitee@test.com", "Invitee", "password");
  }

  private Invitation invitation() {
    return Invitation.createProjectInvitation(INVITATION_ID, PROJECT_ID, WORKSPACE_ID,
        "invitee@test.com", ProjectRole.EDITOR, "admin-id");
  }

}
