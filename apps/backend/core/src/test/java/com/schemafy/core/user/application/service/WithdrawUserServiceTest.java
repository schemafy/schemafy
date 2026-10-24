package com.schemafy.core.user.application.service;

import java.time.Duration;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.mcp.application.port.out.RevokeMcpTokenPort;
import com.schemafy.core.project.application.port.in.LeaveWorkspaceUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;
import com.schemafy.core.ulid.application.service.UlidGenerator;
import com.schemafy.core.user.application.port.in.WithdrawUserCommand;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.application.port.out.UpdateUserPort;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("계정 탈퇴 서비스")
class WithdrawUserServiceTest {

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  FindUserByIdPort findUserByIdPort;

  @Mock
  UpdateUserPort updateUserPort;

  @Mock
  WorkspacePort workspacePort;

  @Mock
  ProjectPort projectPort;

  @Mock
  WorkspaceMemberPort workspaceMemberPort;

  @Mock
  ProjectMemberPort projectMemberPort;

  @Mock
  LeaveWorkspaceUseCase leaveWorkspaceUseCase;

  @Mock
  InvitationPort invitationPort;

  @Mock
  RevokeMcpTokenPort revokeMcpTokenPort;

  @Mock
  AuthTokenPort authTokenPort;

  @InjectMocks
  WithdrawUserService sut;

  @BeforeEach
  void setUp() {
    lenient().when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(invitationPort.cancelPendingByInvitedBy(any()))
        .thenReturn(Mono.just(0L));
    lenient().when(invitationPort.cancelPendingByInvitedEmail(any()))
        .thenReturn(Mono.just(0L));
    lenient().when(revokeMcpTokenPort.revokeAllMcpTokensByUserId(any(), any()))
        .thenReturn(Mono.just(0L));
    lenient().when(projectMemberPort.softDeleteByUserId(any()))
        .thenReturn(Mono.just(0L));
    lenient().when(projectPort
        .findByUserIdAndWorkspaceMembershipOrProjectMembershipAndNotDeleted(any()))
        .thenReturn(Flux.empty());
    lenient().when(updateUserPort.updateUser(any()))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, User.class)));
    lenient().when(authTokenPort.delete(any(), any())).thenReturn(Mono.empty());
  }

  @Test
  @DisplayName("다른 멤버가 있는 유일 ADMIN workspace가 있으면 아무 변경 없이 차단한다")
  void withdrawUser_blocksSoleAdminWorkspaceBeforeMutation() {
    User user = activeUser();
    Workspace workspace = Workspace.create(UlidGenerator.generate(), "Shared", "description");
    WorkspaceMember member = WorkspaceMember.create(
        UlidGenerator.generate(), workspace.getId(), user.id(), WorkspaceRole.ADMIN);

    given(findUserByIdPort.findUserByIdForUpdate(user.id()))
        .willReturn(Mono.just(user));
    given(workspacePort.findByUserId(user.id())).willReturn(Flux.just(workspace));
    given(workspacePort.findByIdAndNotDeletedForUpdate(workspace.getId()))
        .willReturn(Mono.just(workspace));
    given(workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(
        workspace.getId(), user.id())).willReturn(Mono.just(member));
    given(workspaceMemberPort.countByWorkspaceIdAndNotDeleted(workspace.getId()))
        .willReturn(Mono.just(2L));
    given(workspaceMemberPort.countByWorkspaceIdAndRoleAndNotDeleted(
        workspace.getId(), WorkspaceRole.ADMIN.name())).willReturn(Mono.just(1L));

    StepVerifier.create(sut.withdrawUser(new WithdrawUserCommand(user.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            WorkspaceErrorCode.LAST_ADMIN_CANNOT_LEAVE))
        .verify();

    verify(leaveWorkspaceUseCase, never()).leaveWorkspace(any());
    verify(projectMemberPort, never()).softDeleteByUserId(any());
    verify(updateUserPort, never()).updateUser(any());
    verify(invitationPort, never()).cancelPendingByInvitedBy(any());
    verify(revokeMcpTokenPort, never()).revokeAllMcpTokensByUserId(any(), any());
  }

  @Test
  @DisplayName("workspace를 모두 처리하고 프로젝트 전용 membership과 인증 상태를 정리한다")
  void withdrawUser_appliesAllAccountChangesAfterPreflight() {
    User user = activeUser();
    Workspace workspace = Workspace.create(UlidGenerator.generate(), "Shared", "description");
    Project project = Project.create(UlidGenerator.generate(), workspace.getId(),
        1, "Project", "description");
    WorkspaceMember member = WorkspaceMember.create(
        UlidGenerator.generate(), workspace.getId(), user.id(), WorkspaceRole.MEMBER);

    given(findUserByIdPort.findUserByIdForUpdate(user.id()))
        .willReturn(Mono.just(user));
    given(workspacePort.findByUserId(user.id())).willReturn(Flux.just(workspace));
    given(workspacePort.findByIdAndNotDeletedForUpdate(workspace.getId()))
        .willReturn(Mono.just(workspace));
    given(projectPort
        .findByUserIdAndWorkspaceMembershipOrProjectMembershipAndNotDeleted(user.id()))
        .willReturn(Flux.just(project));
    given(projectPort.findByIdAndNotDeletedForUpdate(project.getId()))
        .willReturn(Mono.just(project));
    given(workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(
        workspace.getId(), user.id())).willReturn(Mono.just(member));
    given(workspaceMemberPort.countByWorkspaceIdAndNotDeleted(workspace.getId()))
        .willReturn(Mono.just(2L));
    given(workspaceMemberPort.countByWorkspaceIdAndRoleAndNotDeleted(
        workspace.getId(), WorkspaceRole.ADMIN.name())).willReturn(Mono.just(1L));
    given(leaveWorkspaceUseCase.leaveWorkspace(any())).willReturn(Mono.empty());

    StepVerifier.create(sut.withdrawUser(new WithdrawUserCommand(user.id())))
        .verifyComplete();

    verify(leaveWorkspaceUseCase).leaveWorkspace(any());
    verify(projectMemberPort).softDeleteByUserId(user.id());
    verify(projectPort).findByIdAndNotDeletedForUpdate(project.getId());
    InOrder lockOrder = inOrder(workspacePort, projectPort,
        findUserByIdPort);
    lockOrder.verify(workspacePort).findByIdAndNotDeletedForUpdate(
        workspace.getId());
    lockOrder.verify(projectPort).findByIdAndNotDeletedForUpdate(
        project.getId());
    lockOrder.verify(findUserByIdPort)
        .findUserByIdForUpdate(user.id());
    verify(invitationPort).cancelPendingByInvitedBy(user.id());
    verify(invitationPort).cancelPendingByInvitedEmail(user.email());
    verify(revokeMcpTokenPort).revokeAllMcpTokensByUserId(eq(user.id()), any());
    verify(updateUserPort).updateUser(any(User.class));
    verify(authTokenPort, Mockito.times(2)).delete(any(), eq(user.email()));
  }

  @Test
  @DisplayName("탈퇴 도중 workspace 처리가 실패하면 사용자 상태 전환을 실행하지 않는다")
  void withdrawUser_doesNotUpdateUserWhenWorkspaceDepartureFails() {
    User user = activeUser();
    Workspace workspace = Workspace.create(UlidGenerator.generate(), "Shared", "description");
    WorkspaceMember member = WorkspaceMember.create(
        UlidGenerator.generate(), workspace.getId(), user.id(), WorkspaceRole.MEMBER);

    given(findUserByIdPort.findUserByIdForUpdate(user.id()))
        .willReturn(Mono.just(user));
    given(workspacePort.findByUserId(user.id())).willReturn(Flux.just(workspace));
    given(workspacePort.findByIdAndNotDeletedForUpdate(workspace.getId()))
        .willReturn(Mono.just(workspace));
    given(workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(
        workspace.getId(), user.id())).willReturn(Mono.just(member));
    given(workspaceMemberPort.countByWorkspaceIdAndNotDeleted(workspace.getId()))
        .willReturn(Mono.just(2L));
    given(workspaceMemberPort.countByWorkspaceIdAndRoleAndNotDeleted(
        workspace.getId(), WorkspaceRole.ADMIN.name())).willReturn(Mono.just(2L));
    given(leaveWorkspaceUseCase.leaveWorkspace(any()))
        .willReturn(Mono.error(new IllegalStateException("cascade failed")));

    StepVerifier.create(sut.withdrawUser(new WithdrawUserCommand(user.id())))
        .expectErrorMessage("cascade failed")
        .verify();

    verify(updateUserPort, never()).updateUser(any());
  }

  @Test
  @DisplayName("탈퇴 transaction이 제한 시간을 넘기면 timeout으로 실패한다")
  void withdrawUser_timesOutWhenTransactionExceedsBudget() {
    User user = activeUser();
    given(workspacePort.findByUserId(user.id())).willReturn(Flux.never());

    StepVerifier.withVirtualTime(
        () -> sut.withdrawUser(new WithdrawUserCommand(user.id())))
        .thenAwait(Duration.ofSeconds(10))
        .expectErrorMatches(DomainException.hasErrorCode(
            UserErrorCode.WITHDRAWAL_TIMEOUT))
        .verify();

    verify(updateUserPort, never()).updateUser(any());
  }

  @Test
  @DisplayName("workspace가 잠금 전에 사라지면 scope 변경으로 재시도 후 실패한다")
  void withdrawUser_retriesWhenWorkspaceDisappearsBeforeLock() {
    User user = activeUser();
    Workspace workspace = Workspace.create(UlidGenerator.generate(), "Missing", "description");

    given(workspacePort.findByUserId(user.id())).willReturn(Flux.just(workspace));
    given(workspacePort.findByIdAndNotDeletedForUpdate(workspace.getId()))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.withdrawUser(new WithdrawUserCommand(user.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            UserErrorCode.WITHDRAWAL_CONCURRENTLY_CHANGED))
        .verify();

    verify(workspacePort, Mockito.times(4))
        .findByIdAndNotDeletedForUpdate(workspace.getId());
    verify(findUserByIdPort, never()).findUserByIdForUpdate(any());
    verify(updateUserPort, never()).updateUser(any());
  }

  @Test
  @DisplayName("project scope가 잠금 뒤 바뀌면 transaction을 끝내고 전체 scope부터 재시도한다")
  void withdrawUser_retriesWhenProjectScopeChangesAfterLocks() {
    User user = activeUser();
    Project project = Project.create(UlidGenerator.generate(), "workspace-1",
        1, "Project", "description");
    given(workspacePort.findByUserId(user.id())).willReturn(Flux.empty());
    given(projectPort
        .findByUserIdAndWorkspaceMembershipOrProjectMembershipAndNotDeleted(
            user.id()))
        .willReturn(Flux.just(project), Flux.empty(), Flux.empty(),
            Flux.empty());
    given(projectPort.findByIdAndNotDeletedForUpdate(project.getId()))
        .willReturn(Mono.just(project));
    given(findUserByIdPort.findUserByIdForUpdate(user.id()))
        .willReturn(Mono.just(user));

    StepVerifier.create(sut.withdrawUser(new WithdrawUserCommand(user.id())))
        .verifyComplete();

    verify(projectPort).findByIdAndNotDeletedForUpdate(project.getId());
    verify(findUserByIdPort, Mockito.times(2))
        .findUserByIdForUpdate(user.id());
    verify(updateUserPort).updateUser(any(User.class));
    verify(invitationPort).cancelPendingByInvitedBy(user.id());
  }

  private User activeUser() {
    return User.signUp("user-1", "user@example.com", "User", "encoded");
  }

}
