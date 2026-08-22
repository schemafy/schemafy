package com.schemafy.core.project.application.service;

import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.in.DeleteWorkspaceCommand;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.InvitationType;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("워크스페이스 삭제 서비스")
class DeleteWorkspaceServiceTest {

  private static final String WORKSPACE_ID = "workspace-id";

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

  @InjectMocks
  DeleteWorkspaceService sut;

  @Test
  @DisplayName("워크스페이스 배타 락을 획득한 트랜잭션에서 워크스페이스와 각 프로젝트를 삭제한다")
  void deletesWorkspaceAndProjectsAfterAcquiringExclusiveWorkspaceLock() {
    var command = new DeleteWorkspaceCommand(WORKSPACE_ID, "requester-id");
    var workspace = Workspace.create(WORKSPACE_ID, "Workspace", "Description");
    var project = Project.create("project-id", WORKSPACE_ID, "Project", "Description");
    var enteredGuard = new java.util.concurrent.atomic.AtomicBoolean();

    given(workspaceMutationGuard.protectExclusive(eq(WORKSPACE_ID), any()))
        .willAnswer(invocation -> {
          enteredGuard.set(true);
          Supplier<Mono<Void>> action = invocation.getArgument(1);
          return action.get();
        });
    given(workspaceAccessHelper.findWorkspaceAdminMember("requester-id", WORKSPACE_ID))
        .willReturn(Mono.just(WorkspaceMember.create("requester-member-id", WORKSPACE_ID,
            "requester-id", WorkspaceRole.ADMIN)));
    given(workspaceAccessHelper.findWorkspaceOrThrow(WORKSPACE_ID)).willAnswer(invocation -> {
      assertThat(enteredGuard).isTrue();
      return Mono.just(workspace);
    });
    given(workspacePort.save(workspace)).willReturn(Mono.just(workspace));
    given(projectPort.findByWorkspaceId(WORKSPACE_ID)).willReturn(Flux.just(project));
    given(projectCascadeHelper.softDeleteProjectCascade(project)).willReturn(Mono.empty());
    given(workspaceMemberPort.softDeleteByWorkspaceId(WORKSPACE_ID)).willReturn(Mono.empty());
    given(invitationPort.softDeleteByTarget(InvitationType.WORKSPACE.name(), WORKSPACE_ID))
        .willReturn(Mono.just(0L));

    StepVerifier.create(sut.deleteWorkspace(command)).verifyComplete();

    assertThat(workspace.isDeleted()).isTrue();
    then(workspaceMutationGuard).should().protectExclusive(eq(WORKSPACE_ID), any());
    then(projectCascadeHelper).should().softDeleteProjectCascade(project);
  }

  @Test
  @DisplayName("잠금 획득 뒤 요청자가 관리자가 아니면 워크스페이스를 삭제하지 않는다")
  void rejectsNonAdminRequesterAfterAcquiringExclusiveWorkspaceLock() {
    var command = new DeleteWorkspaceCommand(WORKSPACE_ID, "requester-id");
    var requesterWasDemoted = new java.util.concurrent.atomic.AtomicBoolean();

    given(workspaceMutationGuard.protectExclusive(eq(WORKSPACE_ID), any()))
        .willAnswer(invocation -> {
          Supplier<Mono<Void>> action = invocation.getArgument(1);
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

    StepVerifier.create(sut.deleteWorkspace(command))
        .expectErrorMatches(com.schemafy.core.common.exception.DomainException
            .hasErrorCode(WorkspaceErrorCode.ADMIN_REQUIRED))
        .verify();

    then(workspaceAccessHelper).should(org.mockito.Mockito.never())
        .findWorkspaceOrThrow(any());
    then(workspacePort).shouldHaveNoInteractions();
    then(projectPort).shouldHaveNoInteractions();
    then(workspaceMemberPort).shouldHaveNoInteractions();
    then(invitationPort).shouldHaveNoInteractions();
    then(projectCascadeHelper).shouldHaveNoInteractions();
  }

}
