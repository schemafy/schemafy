package com.schemafy.core.project.application.service;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/** 주의: 재시도 시 {@code action}이 다시 실행될 수 있으므로 외부 부수효과를 포함하지 않는다. */
@Component
@RequiredArgsConstructor
public class WorkspaceMutationGuard {

  private static final Logger log = LoggerFactory.getLogger(
      WorkspaceMutationGuard.class);

  private final WorkspacePort workspacePort;
  private final TransactionalOperator transactionalOperator;

  public <T> Mono<T> protectShared(String workspaceId, Supplier<Mono<T>> action) {
    return Mono.defer(() -> lockInShareMode(workspaceId)
        .then(Mono.defer(action)))
        .as(transactionalOperator::transactional)
        .retryWhen(lockRetry(workspaceId));
  }

  public <T> Mono<T> protectExclusive(String workspaceId, Supplier<Mono<T>> action) {
    return Mono.defer(() -> lockForUpdate(workspaceId)
        .then(Mono.defer(action)))
        .as(transactionalOperator::transactional)
        .retryWhen(lockRetry(workspaceId));
  }

  private Mono<Void> lockInShareMode(String workspaceId) {
    return workspacePort.findByIdAndNotDeletedInShareMode(workspaceId)
        .switchIfEmpty(Mono.error(new DomainException(
            WorkspaceErrorCode.NOT_FOUND)))
        .then();
  }

  private Mono<Void> lockForUpdate(String workspaceId) {
    return workspacePort.findByIdAndNotDeletedForUpdate(workspaceId)
        .switchIfEmpty(Mono.error(new DomainException(
            WorkspaceErrorCode.NOT_FOUND)))
        .then();
  }

  private Retry lockRetry(String workspaceId) {
    return Retry.backoff(3, Duration.ofMillis(25))
        .maxBackoff(Duration.ofMillis(250))
        .jitter(0.5)
        .filter(PessimisticLockingFailureException.class::isInstance)
        .onRetryExhaustedThrow((spec, signal) -> signal.failure())
        .doBeforeRetry(signal -> log.warn(
            "Retrying workspace mutation transaction: workspaceId={}, retry={}",
            workspaceId, signal.totalRetries() + 1));
  }

}
