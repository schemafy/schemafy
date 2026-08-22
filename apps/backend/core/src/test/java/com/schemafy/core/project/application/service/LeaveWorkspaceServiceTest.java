package com.schemafy.core.project.application.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.in.LeaveWorkspaceCommand;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.InvitationType;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.project.application.service.MutationGuardTestSupport.invokeGuardAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("워크스페이스 탈퇴 서비스")
class LeaveWorkspaceServiceTest {

  private static final String WORKSPACE_ID = "workspace-id";
  private static final String USER_ID = "user-id";

  @Mock
  WorkspaceMutationGuard workspaceMutationGuard;

  @Mock
  WorkspacePort workspacePort;

  @Mock
  ProjectPort projectPort;

  @Mock
  WorkspaceMemberPort workspaceMemberPort;

  @Mock
  InvitationPort invitationPort;

  @Mock
  WorkspaceAccessHelper workspaceAccessHelper;

  @Mock
  ProjectCascadeHelper projectCascadeHelper;

  @Mock
  ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @InjectMocks
  LeaveWorkspaceService sut;

  @Test
  @DisplayName("워크스페이스 배타 락을 획득한 트랜잭션에서 최신 멤버 수를 확인하고 일반 멤버를 제거한다")
  void removesMemberAfterAcquiringExclusiveWorkspaceLock() {
    var command = new LeaveWorkspaceCommand(WORKSPACE_ID, USER_ID);
    var member = WorkspaceMember.create("member-id", WORKSPACE_ID, USER_ID,
        WorkspaceRole.MEMBER);
    var enteredGuard = new AtomicBoolean();

    given(workspaceMutationGuard.protectExclusive(eq(WORKSPACE_ID), any()))
        .willAnswer(invocation -> {
          enteredGuard.set(true);
          Supplier<Mono<Void>> action = invocation.getArgument(1);
          return action.get();
        });
    given(workspaceAccessHelper.findWorkspaceMember(USER_ID, WORKSPACE_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.just(member);
        });
    given(workspaceMemberPort.countByWorkspaceIdAndNotDeleted(WORKSPACE_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.just(2L);
        });
    given(workspaceAccessHelper.modifyMemberWithAdminGuard(eq(WORKSPACE_ID), eq(member), any()))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.just(member);
        });
    given(projectMembershipPropagationHelper.removeFromAllProjects(WORKSPACE_ID, USER_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.empty();
        });

    StepVerifier.create(sut.leaveWorkspace(command)).verifyComplete();

    then(workspaceMutationGuard).should().protectExclusive(eq(WORKSPACE_ID), any());
  }

  @Test
  @DisplayName("마지막 활성 멤버가 탈퇴하면 워크스페이스 배타 락을 획득한 트랜잭션에서 워크스페이스와 하위 항목을 삭제한다")
  void deletesWorkspaceWhenLastActiveMemberLeaves() {
    var command = new LeaveWorkspaceCommand(WORKSPACE_ID, USER_ID);
    var member = WorkspaceMember.create("member-id", WORKSPACE_ID, USER_ID,
        WorkspaceRole.MEMBER);
    var workspace = Workspace.create(WORKSPACE_ID, "Workspace", "Description");
    var project = Project.create("project-id", WORKSPACE_ID, "Project", "Description");

    given(workspaceMutationGuard.protectExclusive(eq(WORKSPACE_ID), any()))
        .willAnswer(invokeGuardAction());
    given(workspaceAccessHelper.findWorkspaceMember(USER_ID, WORKSPACE_ID))
        .willReturn(Mono.just(member));
    given(workspaceMemberPort.countByWorkspaceIdAndNotDeleted(WORKSPACE_ID))
        .willReturn(Mono.just(1L));
    given(workspaceAccessHelper.findWorkspaceOrThrow(WORKSPACE_ID)).willReturn(Mono.just(workspace));
    given(workspacePort.save(workspace)).willReturn(Mono.just(workspace));
    given(projectPort.findByWorkspaceId(WORKSPACE_ID)).willReturn(Flux.just(project));
    given(projectCascadeHelper.softDeleteProjectCascade(project)).willReturn(Mono.empty());
    given(workspaceMemberPort.softDeleteByWorkspaceId(WORKSPACE_ID)).willReturn(Mono.empty());
    given(invitationPort.softDeleteByTarget(InvitationType.WORKSPACE.name(), WORKSPACE_ID))
        .willReturn(Mono.just(0L));

    StepVerifier.create(sut.leaveWorkspace(command)).verifyComplete();

    assertThat(workspace.isDeleted()).isTrue();
    then(projectCascadeHelper).should().softDeleteProjectCascade(project);
  }

}
