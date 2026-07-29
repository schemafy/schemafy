package com.schemafy.core.project.application.service;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.Workspace;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("프로젝트 변경 보호 테스트")
class ProjectMutationGuardTest {

  @Mock
  private ProjectPort projectPort;

  @Mock
  private WorkspacePort workspacePort;

  @Mock
  private TransactionalOperator transactionalOperator;

  private ProjectMutationGuard sut;

  @BeforeEach
  void setUp() {
    sut = new ProjectMutationGuard(projectPort, workspacePort,
        transactionalOperator);
    lenient().when(transactionalOperator.<Object>transactional(
        ArgumentMatchers.<Mono<Object>>any()))
        .thenAnswer(invocation -> invocation.<Mono<Object>>getArgument(0));
  }

  @Test
  @DisplayName("공유 잠금 획득 실패 시 잠금부터 전체 작업을 재시도한다")
  void protectChildCreation_retriesFromLockWhenLockCannotBeAcquired() {
    when(projectPort.lockByIdAndNotDeletedInShareMode("project-id"))
        .thenReturn(Mono.error(new CannotAcquireLockException("lock timeout")))
        .thenReturn(Mono.just("project-id"));

    StepVerifier.create(sut.protectChildCreation(
        "project-id", () -> Mono.just("done")))
        .expectNext("done")
        .verifyComplete();

    verify(projectPort, times(2))
        .lockByIdAndNotDeletedInShareMode("project-id");
  }

  @Test
  @DisplayName("작업 중 잠금 실패 시 잠금부터 전체 작업을 재시도한다")
  void protectChildCreation_retriesActionFromLock() {
    AtomicInteger actionAttempts = new AtomicInteger();
    when(projectPort.lockByIdAndNotDeletedInShareMode("project-id"))
        .thenReturn(Mono.just("project-id"));

    StepVerifier.create(sut.protectChildCreation("project-id", () -> Mono.defer(
        () -> actionAttempts.incrementAndGet() == 1
            ? Mono.error(new CannotAcquireLockException("lock timeout"))
            : Mono.just("done"))))
        .expectNext("done")
        .verifyComplete();

    assertThat(actionAttempts).hasValue(2);
    verify(projectPort, times(2))
        .lockByIdAndNotDeletedInShareMode("project-id");
  }

  @Test
  @DisplayName("잠금 재시도 소진 시 마지막 잠금 예외를 그대로 전달한다")
  void protectChildCreation_propagatesLastLockFailureWhenRetriesExhausted() {
    CannotAcquireLockException failure = new CannotAcquireLockException(
        "lock timeout");
    when(projectPort.lockByIdAndNotDeletedInShareMode("project-id"))
        .thenReturn(Mono.error(failure));

    StepVerifier.create(sut.protectChildCreation(
        "project-id", () -> Mono.just("done")))
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
        .verify();

    verify(projectPort, times(4))
        .lockByIdAndNotDeletedInShareMode("project-id");
  }

  @Test
  @DisplayName("프로젝트 삭제는 워크스페이스 공유 잠금 후 프로젝트 배타 잠금을 획득한다")
  void protectProjectDeletion_locksWorkspaceSharedThenProjectExclusive() {
    Project project = Project.create("project-id", "workspace-id", "Project",
        "Description");
    Workspace workspace = Workspace.create("workspace-id", "Workspace",
        "Description");

    when(projectPort.findByIdAndNotDeleted("project-id"))
        .thenReturn(Mono.just(project));
    when(workspacePort.findByIdAndNotDeletedInShareMode("workspace-id"))
        .thenReturn(Mono.just(workspace));
    when(projectPort.lockByIdAndNotDeletedForUpdate("project-id"))
        .thenReturn(Mono.just("project-id"));

    StepVerifier.create(sut.protectProjectDeletion(
        "project-id", () -> Mono.just("done")))
        .expectNext("done")
        .verifyComplete();

    verify(workspacePort).findByIdAndNotDeletedInShareMode("workspace-id");
    verify(projectPort).lockByIdAndNotDeletedForUpdate("project-id");
    InOrder lockOrder = inOrder(workspacePort, projectPort);
    lockOrder.verify(workspacePort)
        .findByIdAndNotDeletedInShareMode("workspace-id");
    lockOrder.verify(projectPort).lockByIdAndNotDeletedForUpdate("project-id");
  }

  @Test
  @DisplayName("프로젝트 삭제 대상이 없으면 작업을 실행하지 않는다")
  void protectProjectDeletion_rejectsMissingProject() {
    when(projectPort.findByIdAndNotDeleted("project-id"))
        .thenReturn(Mono.empty());

    StepVerifier.create(sut.protectProjectDeletion(
        "project-id", () -> Mono.just("done")))
        .expectError()
        .verify();

    verifyNoInteractions(workspacePort);
  }

  @Test
  @DisplayName("하위 생성은 프로젝트 공유 잠금만 획득한다")
  void protectChildCreation_locksOnlyProject() {
    when(projectPort.lockByIdAndNotDeletedInShareMode("project-id"))
        .thenReturn(Mono.just("project-id"));

    StepVerifier.create(sut.protectChildCreation(
        "project-id", () -> Mono.just("done")))
        .expectNext("done")
        .verifyComplete();

    verify(projectPort).lockByIdAndNotDeletedInShareMode("project-id");
    verifyNoInteractions(workspacePort);
  }

}
