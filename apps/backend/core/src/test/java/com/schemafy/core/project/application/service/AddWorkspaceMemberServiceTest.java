package com.schemafy.core.project.application.service;

import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.in.AddWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("워크스페이스 멤버 추가 서비스")
class AddWorkspaceMemberServiceTest {

  private static final String WORKSPACE_ID = "workspace-id";
  private static final String USER_ID = "user-id";

  @Mock
  WorkspaceMutationGuard workspaceMutationGuard;

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  WorkspaceMemberPort workspaceMemberPort;

  @Mock
  WorkspaceAccessHelper workspaceAccessHelper;

  @Mock
  ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @InjectMocks
  AddWorkspaceMemberService sut;

  @Test
  @DisplayName("워크스페이스 배타 락을 획득한 트랜잭션에서 대상 멤버의 최신 상태를 조회하고 프로젝트 멤버십을 동기화한다")
  void readsLatestMemberAndSynchronizesProjectsAfterAcquiringExclusiveWorkspaceLock() {
    var command = new AddWorkspaceMemberCommand(
        WORKSPACE_ID, "target@test.com", WorkspaceRole.MEMBER, "requester-id");
    var targetUser = User.signUp(USER_ID, command.email(), "Target", "password");
    var enteredGuard = new java.util.concurrent.atomic.AtomicBoolean();

    given(workspaceAccessHelper.findUserByEmailOrThrow(any()))
        .willReturn(Mono.just(targetUser));
    given(workspaceAccessHelper.findWorkspaceAdminMember("requester-id", WORKSPACE_ID))
        .willReturn(Mono.just(WorkspaceMember.create("requester-member-id", WORKSPACE_ID,
            "requester-id", WorkspaceRole.ADMIN)));
    given(workspaceMutationGuard.protectExclusive(eq(WORKSPACE_ID), any()))
        .willAnswer(invocation -> {
          enteredGuard.set(true);
          Supplier<Mono<WorkspaceMember>> action = invocation.getArgument(1);
          return action.get();
        });
    given(workspaceMemberPort.findLatestByWorkspaceIdAndUserId(WORKSPACE_ID, USER_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.empty();
        });
    given(ulidGeneratorPort.generate()).willReturn("workspace-member-id");
    given(workspaceMemberPort.save(any(WorkspaceMember.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    given(projectMembershipPropagationHelper.syncProjectMembershipsForWorkspaceRole(
        WORKSPACE_ID, USER_ID, WorkspaceRole.MEMBER)).willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.empty();
        });
    StepVerifier.create(sut.addWorkspaceMember(command))
        .assertNext(member -> {
          assertThat(member.getUserId()).isEqualTo(USER_ID);
          assertThat(member.getRoleAsEnum()).isEqualTo(WorkspaceRole.MEMBER);
        })
        .verifyComplete();

    then(workspaceMutationGuard).should().protectExclusive(eq(WORKSPACE_ID), any());
  }

  @Test
  @DisplayName("잠금 획득 뒤 요청자가 워크스페이스에서 제거되면 대상 사용자를 조회하거나 멤버를 추가하지 않는다")
  void rejectsRemovedRequesterAfterAcquiringExclusiveWorkspaceLock() {
    var command = new AddWorkspaceMemberCommand(
        WORKSPACE_ID, "target@test.com", WorkspaceRole.MEMBER, "requester-id");
    var requesterWasRemoved = new java.util.concurrent.atomic.AtomicBoolean();

    given(workspaceMutationGuard.protectExclusive(eq(WORKSPACE_ID), any()))
        .willAnswer(invocation -> {
          Supplier<Mono<WorkspaceMember>> action = invocation.getArgument(1);
          return Mono.defer(() -> {
            requesterWasRemoved.set(true);
            return action.get();
          });
        });
    given(workspaceAccessHelper.findWorkspaceAdminMember("requester-id", WORKSPACE_ID))
        .willAnswer(invocation -> requesterWasRemoved.get()
            ? Mono.error(new com.schemafy.core.common.exception.DomainException(
                WorkspaceErrorCode.ACCESS_DENIED))
            : Mono.just(WorkspaceMember.create("requester-member-id", WORKSPACE_ID,
                "requester-id", WorkspaceRole.ADMIN)));

    StepVerifier.create(sut.addWorkspaceMember(command))
        .expectErrorMatches(com.schemafy.core.common.exception.DomainException
            .hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();

    then(workspaceAccessHelper).should(org.mockito.Mockito.never())
        .findUserByEmailOrThrow(any());
    then(workspaceMemberPort).shouldHaveNoInteractions();
    then(ulidGeneratorPort).shouldHaveNoInteractions();
    then(projectMembershipPropagationHelper).shouldHaveNoInteractions();
  }

}
