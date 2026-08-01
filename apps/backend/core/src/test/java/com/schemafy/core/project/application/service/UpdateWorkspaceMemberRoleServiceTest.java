package com.schemafy.core.project.application.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleCommand;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("워크스페이스 멤버 역할 변경 서비스")
class UpdateWorkspaceMemberRoleServiceTest {

  private static final String WORKSPACE_ID = "workspace-id";
  private static final String TARGET_USER_ID = "target-user-id";

  @Mock
  WorkspaceMutationGuard workspaceMutationGuard;

  @Mock
  WorkspaceAccessHelper workspaceAccessHelper;

  @Mock
  ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @InjectMocks
  UpdateWorkspaceMemberRoleService sut;

  @Test
  @DisplayName("워크스페이스 배타 락을 획득한 트랜잭션에서 대상과 마지막 관리자 조건을 확인하고 프로젝트 역할을 반영한다")
  void updatesRoleAndProjectMembershipsAfterAcquiringExclusiveWorkspaceLock() {
    var command = new UpdateWorkspaceMemberRoleCommand(
        WORKSPACE_ID, TARGET_USER_ID, WorkspaceRole.MEMBER, "requester-id");
    var targetMember = WorkspaceMember.create(
        "member-id", WORKSPACE_ID, TARGET_USER_ID, WorkspaceRole.ADMIN);
    var enteredGuard = new java.util.concurrent.atomic.AtomicBoolean();

    given(workspaceMutationGuard.protectExclusive(eq(WORKSPACE_ID), any()))
        .willAnswer(invocation -> {
          enteredGuard.set(true);
          Supplier<Mono<WorkspaceMember>> action = invocation.getArgument(1);
          return action.get();
        });
    given(workspaceAccessHelper.findWorkspaceAdminMember("requester-id", WORKSPACE_ID))
        .willReturn(Mono.just(WorkspaceMember.create("requester-member-id", WORKSPACE_ID,
            "requester-id", WorkspaceRole.ADMIN)));
    given(workspaceAccessHelper.findWorkspaceMember(TARGET_USER_ID, WORKSPACE_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.just(targetMember);
        });
    given(workspaceAccessHelper.modifyMemberWithAdminGuard(eq(WORKSPACE_ID),
        eq(targetMember), any())).willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          Consumer<WorkspaceMember> action = invocation.getArgument(2);
          action.accept(targetMember);
          return Mono.just(targetMember);
        });
    given(projectMembershipPropagationHelper.updateActiveProjectMembershipRoles(
        WORKSPACE_ID, TARGET_USER_ID, WorkspaceRole.MEMBER)).willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.empty();
        });
    StepVerifier.create(sut.updateWorkspaceMemberRole(command))
        .assertNext(member -> assertThat(member.getRoleAsEnum()).isEqualTo(WorkspaceRole.MEMBER))
        .verifyComplete();

    then(workspaceMutationGuard).should().protectExclusive(eq(WORKSPACE_ID), any());
  }

  @Test
  @DisplayName("잠금 획득 뒤 요청자가 워크스페이스에서 제거되면 대상 역할을 변경하지 않는다")
  void rejectsRemovedRequesterAfterAcquiringExclusiveWorkspaceLock() {
    var command = new UpdateWorkspaceMemberRoleCommand(
        WORKSPACE_ID, TARGET_USER_ID, WorkspaceRole.MEMBER, "requester-id");
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

    StepVerifier.create(sut.updateWorkspaceMemberRole(command))
        .expectErrorMatches(com.schemafy.core.common.exception.DomainException
            .hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();

    then(workspaceAccessHelper).should(org.mockito.Mockito.never())
        .findWorkspaceMember(any(), any());
    then(projectMembershipPropagationHelper).shouldHaveNoInteractions();
  }

}
