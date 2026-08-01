package com.schemafy.core.project.application.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.in.CreateWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("워크스페이스 초대 생성 서비스")
class CreateWorkspaceInvitationServiceTest {

  private static final String WORKSPACE_ID = "workspace-id";

  @Mock
  WorkspaceMutationGuard workspaceMutationGuard;

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  InvitationPort invitationPort;

  @Mock
  WorkspaceInvitationHelper workspaceInvitationHelper;

  @Mock
  WorkspaceAccessHelper workspaceAccessHelper;

  @InjectMocks
  CreateWorkspaceInvitationService sut;

  @Test
  @DisplayName("워크스페이스 공유 락을 획득한 트랜잭션에서 최신 워크스페이스와 중복 상태를 확인한 뒤 초대를 저장한다")
  void createsInvitationAfterAcquiringSharedWorkspaceLock() {
    var command = new CreateWorkspaceInvitationCommand(
        WORKSPACE_ID, "invitee@test.com", WorkspaceRole.MEMBER, "requester-id");
    var enteredGuard = new AtomicBoolean();

    given(workspaceMutationGuard.protectShared(eq(WORKSPACE_ID), any()))
        .willAnswer(invocation -> {
          enteredGuard.set(true);
          Supplier<Mono<Invitation>> action = invocation.getArgument(1);
          return action.get();
        });
    given(workspaceAccessHelper.findWorkspaceAdminMember("requester-id", WORKSPACE_ID))
        .willReturn(Mono.just(WorkspaceMember.create("requester-member-id", WORKSPACE_ID,
            "requester-id", WorkspaceRole.ADMIN)));
    given(workspaceInvitationHelper.findWorkspaceOrThrow(WORKSPACE_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.just(Workspace.create(WORKSPACE_ID, "Workspace", "Description"));
        });
    given(workspaceInvitationHelper.checkNotAlreadyMemberByEmail(eq(WORKSPACE_ID), any()))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.empty();
        });
    given(workspaceInvitationHelper.checkDuplicatePendingInvitation(eq(WORKSPACE_ID), any()))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.empty();
        });
    given(ulidGeneratorPort.generate()).willReturn("invitation-id");
    given(invitationPort.save(any(Invitation.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(sut.createWorkspaceInvitation(command))
        .assertNext(invitation -> {
          assertThat(invitation.getId()).isEqualTo("invitation-id");
          assertThat(invitation.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        })
        .verifyComplete();

    then(workspaceMutationGuard).should().protectShared(eq(WORKSPACE_ID), any());
  }

  @Test
  @DisplayName("잠금 획득 뒤 요청자가 관리자가 아니면 초대 중복을 확인하거나 저장하지 않는다")
  void rejectsNonAdminRequesterAfterAcquiringSharedWorkspaceLock() {
    var command = new CreateWorkspaceInvitationCommand(
        WORKSPACE_ID, "invitee@test.com", WorkspaceRole.MEMBER, "requester-id");
    var requesterWasDemoted = new AtomicBoolean();

    given(workspaceMutationGuard.protectShared(eq(WORKSPACE_ID), any()))
        .willAnswer(invocation -> {
          Supplier<Mono<Invitation>> action = invocation.getArgument(1);
          return Mono.defer(() -> {
            requesterWasDemoted.set(true);
            return action.get();
          });
        });
    given(workspaceAccessHelper.findWorkspaceAdminMember("requester-id", WORKSPACE_ID))
        .willAnswer(invocation -> requesterWasDemoted.get()
            ? Mono.error(new com.schemafy.core.common.exception.DomainException(
                WorkspaceErrorCode.ADMIN_REQUIRED))
            : Mono.just(WorkspaceMember.create("requester-member-id", WORKSPACE_ID,
                "requester-id", WorkspaceRole.ADMIN)));

    StepVerifier.create(sut.createWorkspaceInvitation(command))
        .expectErrorMatches(com.schemafy.core.common.exception.DomainException
            .hasErrorCode(WorkspaceErrorCode.ADMIN_REQUIRED))
        .verify();

    then(workspaceInvitationHelper).shouldHaveNoInteractions();
    then(ulidGeneratorPort).shouldHaveNoInteractions();
    then(invitationPort).shouldHaveNoInteractions();
  }

}
