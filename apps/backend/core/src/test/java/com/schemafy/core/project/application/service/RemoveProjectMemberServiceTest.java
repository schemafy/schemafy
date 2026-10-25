package com.schemafy.core.project.application.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.RemoveProjectMemberCommand;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.project.application.service.MutationGuardTestSupport.invokeGuardAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("프로젝트 멤버 제거 서비스 테스트")
class RemoveProjectMemberServiceTest {

  private static final String PROJECT_ID = "project-id";
  private static final String TARGET_ID = "target-id";

  @Mock
  ProjectMutationGuard projectMutationGuard;

  @Mock
  ProjectAccessHelper projectAccessHelper;

  @InjectMocks
  RemoveProjectMemberService sut;

  @Test
  @DisplayName("프로젝트 변경 락을 획득한 트랜잭션에서 최신 대상 멤버를 읽고 제거한다")
  void removesCurrentMemberAfterAcquiringProjectMutationLock() {
    var command = new RemoveProjectMemberCommand(PROJECT_ID, TARGET_ID, "requester-id");
    var target = ProjectMember.create("member-id", PROJECT_ID, TARGET_ID, ProjectRole.EDITOR);
    var enteredGuard = new AtomicBoolean();

    given(projectMutationGuard.protectProjectMutation(eq(PROJECT_ID), any()))
        .willAnswer(invocation -> {
          enteredGuard.set(true);
          Supplier<Mono<Void>> action = invocation.getArgument(1);
          return action.get();
        });
    given(projectAccessHelper.findProjectMember(TARGET_ID, PROJECT_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.just(target);
        });
    given(projectAccessHelper.findProjectAdminMember("requester-id", PROJECT_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.just(ProjectMember.create("requester-member", PROJECT_ID, "requester-id",
              ProjectRole.ADMIN));
        });
    given(projectAccessHelper.validateWorkspaceAdminGuard(PROJECT_ID, target))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.empty();
        });
    given(projectAccessHelper.softDeleteMember(target))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.empty();
        });

    StepVerifier.create(sut.removeProjectMember(command)).verifyComplete();

    then(projectMutationGuard).should().protectProjectMutation(eq(PROJECT_ID), any());
  }

  @Test
  @DisplayName("조건부 삭제 결과가 0건이어도 멱등적으로 완료한다")
  void completesWhenConditionalDeleteAffectsNoRows() {
    var command = new RemoveProjectMemberCommand(PROJECT_ID, TARGET_ID, "requester-id");
    var target = ProjectMember.create("member-id", PROJECT_ID, TARGET_ID, ProjectRole.EDITOR);

    given(projectMutationGuard.protectProjectMutation(eq(PROJECT_ID), any()))
        .willAnswer(invokeGuardAction());
    given(projectAccessHelper.findProjectMember(TARGET_ID, PROJECT_ID)).willReturn(Mono.just(target));
    given(projectAccessHelper.findProjectAdminMember("requester-id", PROJECT_ID))
        .willReturn(Mono.just(ProjectMember.create("requester-member", PROJECT_ID, "requester-id",
            ProjectRole.ADMIN)));
    given(projectAccessHelper.validateWorkspaceAdminGuard(PROJECT_ID, target)).willReturn(Mono.empty());
    given(projectAccessHelper.softDeleteMember(target)).willReturn(Mono.empty());

    StepVerifier.create(sut.removeProjectMember(command)).verifyComplete();
  }

  @Test
  @DisplayName("워크스페이스 공유 락 대기 중 요청자가 비관리자로 변경되면 대상 멤버를 제거하지 않는다")
  void rejectsDemotedRequesterAfterAcquiringSharedWorkspaceLock() {
    var command = new RemoveProjectMemberCommand(PROJECT_ID, TARGET_ID, "requester-id");

    given(projectMutationGuard.protectProjectMutation(eq(PROJECT_ID), any()))
        .willAnswer(invokeGuardAction());
    given(projectAccessHelper.findProjectAdminMember("requester-id", PROJECT_ID))
        .willReturn(Mono.error(new DomainException(ProjectErrorCode.ADMIN_REQUIRED)));

    StepVerifier.create(sut.removeProjectMember(command))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ADMIN_REQUIRED))
        .verify();

    then(projectAccessHelper).should(never()).validateWorkspaceAdminGuard(eq(PROJECT_ID), any());
    then(projectAccessHelper).should(never()).softDeleteMember(any());
  }

  @Test
  @DisplayName("워크스페이스 공유 락 대기 중 요청자 멤버십이 삭제되면 대상 멤버를 제거하지 않는다")
  void rejectsRemovedRequesterAfterAcquiringSharedWorkspaceLock() {
    var command = new RemoveProjectMemberCommand(PROJECT_ID, TARGET_ID, "requester-id");

    given(projectMutationGuard.protectProjectMutation(eq(PROJECT_ID), any()))
        .willAnswer(invokeGuardAction());
    given(projectAccessHelper.findProjectAdminMember("requester-id", PROJECT_ID))
        .willReturn(Mono.error(new DomainException(ProjectErrorCode.ACCESS_DENIED)));

    StepVerifier.create(sut.removeProjectMember(command))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ACCESS_DENIED))
        .verify();

    then(projectAccessHelper).should(never()).validateWorkspaceAdminGuard(eq(PROJECT_ID), any());
    then(projectAccessHelper).should(never()).softDeleteMember(any());
  }

}
