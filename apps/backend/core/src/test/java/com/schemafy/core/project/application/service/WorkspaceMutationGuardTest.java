package com.schemafy.core.project.application.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("워크스페이스 변경 잠금과 재시도")
class WorkspaceMutationGuardTest {

  @Mock
  private WorkspacePort workspacePort;

  @Mock
  private TransactionalOperator transactionalOperator;

  private final AtomicInteger transactionSubscriptions = new AtomicInteger();

  private WorkspaceMutationGuard sut;

  @BeforeEach
  void setUp() {
    sut = new WorkspaceMutationGuard(workspacePort, transactionalOperator);
    transactionSubscriptions.set(0);
    lenient().when(transactionalOperator.<Object>transactional(
        ArgumentMatchers.<Mono<Object>>any()))
        .thenAnswer(invocation -> Mono.defer(() -> {
          transactionSubscriptions.incrementAndGet();
          return invocation.<Mono<Object>>getArgument(0);
        }));
  }

  @Test
  @DisplayName("워크스페이스 공유 락을 획득한 뒤 상태 변경을 실행한다")
  void protectShared_locksInShareModeBeforeRunningAction() {
    AtomicBoolean locked = new AtomicBoolean();
    when(workspacePort.findByIdAndNotDeletedInShareMode("workspace-id"))
        .thenReturn(Mono.defer(() -> {
          locked.set(true);
          return Mono.just(workspace());
        }));

    StepVerifier.create(sut.protectShared("workspace-id", () -> Mono.defer(() -> {
      assertThat(locked).isTrue();
      return Mono.just("done");
    })))
        .expectNext("done")
        .verifyComplete();

    verify(workspacePort).findByIdAndNotDeletedInShareMode("workspace-id");
    verify(workspacePort, never()).findByIdAndNotDeletedForUpdate("workspace-id");
  }

  @Test
  @DisplayName("워크스페이스 배타 락을 획득한 뒤 상태 변경을 실행한다")
  void protectExclusive_locksForUpdate() {
    when(workspacePort.findByIdAndNotDeletedForUpdate("workspace-id"))
        .thenReturn(Mono.just(workspace()));

    StepVerifier.create(sut.protectExclusive("workspace-id", () -> Mono.just("done")))
        .expectNext("done")
        .verifyComplete();

    verify(workspacePort).findByIdAndNotDeletedForUpdate("workspace-id");
    verify(workspacePort, never())
        .findByIdAndNotDeletedInShareMode("workspace-id");
  }

  @Test
  @DisplayName("잠금 대상이 없으면 작업을 실행하지 않고 워크스페이스 없음 오류를 반환한다")
  void protectShared_rejectsMissingWorkspaceWithoutRunningAction() {
    AtomicBoolean actionCalled = new AtomicBoolean();
    when(workspacePort.findByIdAndNotDeletedInShareMode("workspace-id"))
        .thenReturn(Mono.empty());

    StepVerifier.create(sut.protectShared("workspace-id", () -> {
      actionCalled.set(true);
      return Mono.just("done");
    }))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(WorkspaceErrorCode.NOT_FOUND);
        })
        .verify();

    assertThat(actionCalled).isFalse();
  }

  @Test
  @DisplayName("공유 잠금 실패 시 잠금부터 전체 작업을 재시도한다")
  void protectShared_retriesFromLockWhenLockCannotBeAcquired() {
    when(workspacePort.findByIdAndNotDeletedInShareMode("workspace-id"))
        .thenReturn(Mono.error(new CannotAcquireLockException("lock timeout")))
        .thenReturn(Mono.just(workspace()));

    StepVerifier.create(sut.protectShared("workspace-id", () -> Mono.just("done")))
        .expectNext("done")
        .verifyComplete();

    verify(workspacePort, times(2))
        .findByIdAndNotDeletedInShareMode("workspace-id");
    assertThat(transactionSubscriptions).hasValue(2);
  }

  @Test
  @DisplayName("작업 중 잠금 실패 시 배타 잠금부터 전체 작업을 재시도한다")
  void protectExclusive_retriesActionFromLock() {
    AtomicInteger actionAttempts = new AtomicInteger();
    when(workspacePort.findByIdAndNotDeletedForUpdate("workspace-id"))
        .thenReturn(Mono.just(workspace()));

    StepVerifier.create(sut.protectExclusive("workspace-id", () -> Mono.defer(
        () -> actionAttempts.incrementAndGet() == 1
            ? Mono.error(new CannotAcquireLockException("lock timeout"))
            : Mono.just("done"))))
        .expectNext("done")
        .verifyComplete();

    assertThat(actionAttempts).hasValue(2);
    verify(workspacePort, times(2))
        .findByIdAndNotDeletedForUpdate("workspace-id");
    assertThat(transactionSubscriptions).hasValue(2);
  }

  @Test
  @DisplayName("도메인 오류는 재시도하지 않고 그대로 전달한다")
  void protectExclusive_doesNotRetryDomainException() {
    DomainException failure = new DomainException(WorkspaceErrorCode.NOT_FOUND);
    when(workspacePort.findByIdAndNotDeletedForUpdate("workspace-id"))
        .thenReturn(Mono.error(failure));

    StepVerifier.create(sut.protectExclusive("workspace-id", () -> Mono.just("done")))
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
        .verify();

    verify(workspacePort).findByIdAndNotDeletedForUpdate("workspace-id");
  }

  @Test
  @DisplayName("잠금 재시도 소진 시 마지막 잠금 예외를 그대로 전달한다")
  void protectShared_propagatesLastLockFailureWhenRetriesExhausted() {
    CannotAcquireLockException failure = new CannotAcquireLockException("lock timeout");
    when(workspacePort.findByIdAndNotDeletedInShareMode("workspace-id"))
        .thenReturn(Mono.error(failure));

    StepVerifier.create(sut.protectShared("workspace-id", () -> Mono.just("done")))
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
        .verify();

    verify(workspacePort, times(4))
        .findByIdAndNotDeletedInShareMode("workspace-id");
    assertThat(transactionSubscriptions).hasValue(4);
  }

  private Workspace workspace() {
    return Workspace.create("workspace-id", "워크스페이스", "설명");
  }

}
