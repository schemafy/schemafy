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
import com.schemafy.core.project.application.port.in.CreateProjectCommand;
import com.schemafy.core.project.application.port.in.ProjectDetail;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
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
@DisplayName("프로젝트 생성 서비스")
class CreateProjectServiceTest {

  private static final String WORKSPACE_ID = "workspace-id";
  private static final String REQUESTER_ID = "requester-id";

  @Mock
  WorkspaceMutationGuard workspaceMutationGuard;

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  ProjectPort projectPort;

  @Mock
  ProjectMemberPort projectMemberPort;

  @Mock
  ProjectAccessHelper projectAccessHelper;

  @Mock
  ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @Mock
  WorkspaceAccessHelper workspaceAccessHelper;

  @InjectMocks
  CreateProjectService sut;

  @Test
  @DisplayName("워크스페이스 공유 락을 획득한 트랜잭션에서 프로젝트 생성과 멤버 전파를 수행한다")
  void createsProjectAndPropagatesMembersAfterAcquiringSharedWorkspaceLock() {
    var command = new CreateProjectCommand(
        WORKSPACE_ID, "Project", "Description", REQUESTER_ID);
    var enteredGuard = new AtomicBoolean();

    given(workspaceMutationGuard.protectShared(eq(WORKSPACE_ID), any()))
        .willAnswer(invocation -> {
          enteredGuard.set(true);
          Supplier<Mono<ProjectDetail>> action = invocation.getArgument(1);
          return action.get();
        });
    given(workspaceAccessHelper.findWorkspaceAdminMember(REQUESTER_ID, WORKSPACE_ID))
        .willReturn(Mono.just(WorkspaceMember.create(
            "requester-member-id", WORKSPACE_ID, REQUESTER_ID, WorkspaceRole.ADMIN)));
    given(ulidGeneratorPort.generate()).willReturn("project-id", "member-id");
    given(projectPort.save(any(Project.class))).willAnswer(invocation -> {
      assertThat(enteredGuard).isTrue();
      return Mono.just(invocation.getArgument(0));
    });
    given(projectMemberPort.save(any(ProjectMember.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    given(projectMembershipPropagationHelper.propagateWorkspaceMembersToProject(
        "project-id", WORKSPACE_ID, REQUESTER_ID)).willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.empty();
        });
    given(projectAccessHelper.buildProjectDetail(any(Project.class), eq(REQUESTER_ID)))
        .willAnswer(invocation -> Mono.just(new ProjectDetail(
            invocation.getArgument(0), ProjectRole.ADMIN.name())));

    StepVerifier.create(sut.createProject(command))
        .expectNextCount(1)
        .verifyComplete();

    then(workspaceMutationGuard).should().protectShared(eq(WORKSPACE_ID), any());
  }

  @Test
  @DisplayName("락 획득 뒤 요청자가 워크스페이스에서 제거되면 프로젝트를 생성하지 않는다")
  void rejectsRemovedRequesterAfterAcquiringSharedWorkspaceLock() {
    var command = new CreateProjectCommand(
        WORKSPACE_ID, "Project", "Description", REQUESTER_ID);
    var requesterWasRemoved = new AtomicBoolean();

    given(workspaceMutationGuard.protectShared(eq(WORKSPACE_ID), any()))
        .willAnswer(invocation -> {
          Supplier<Mono<ProjectDetail>> action = invocation.getArgument(1);
          return Mono.defer(() -> {
            requesterWasRemoved.set(true);
            return action.get();
          });
        });
    given(workspaceAccessHelper.findWorkspaceAdminMember(REQUESTER_ID, WORKSPACE_ID))
        .willAnswer(invocation -> requesterWasRemoved.get()
            ? Mono.error(new DomainException(WorkspaceErrorCode.ACCESS_DENIED))
            : Mono.just(WorkspaceMember.create(
                "requester-member-id", WORKSPACE_ID, REQUESTER_ID, WorkspaceRole.ADMIN)));

    StepVerifier.create(sut.createProject(command))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();

    then(ulidGeneratorPort).shouldHaveNoInteractions();
    then(projectPort).shouldHaveNoInteractions();
    then(projectMemberPort).shouldHaveNoInteractions();
    then(projectMembershipPropagationHelper).shouldHaveNoInteractions();
    then(projectAccessHelper).shouldHaveNoInteractions();
  }

}
