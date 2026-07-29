package com.schemafy.core.project.application.service;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/** 주의: 재시도 시 {@code action}이 다시 실행될 수 있으므로 롤백되지 않는 외부 부수효과를 포함하지 않는다. */
@Component
@RequiredArgsConstructor
public class ProjectMutationGuard {

  private static final Logger log = LoggerFactory.getLogger(
      ProjectMutationGuard.class);

  private final ProjectPort projectPort;
  private final WorkspacePort workspacePort;
  private final TransactionalOperator transactionalOperator;

  public <T> Mono<T> protectChildCreation(
      String projectId,
      Supplier<Mono<T>> action) {
    return Mono.defer(() -> lockProjectInShareMode(projectId)
        .then(Mono.defer(action)))
        .as(transactionalOperator::transactional)
        .retryWhen(lockRetry(projectId));
  }

  public <T> Mono<T> protectProjectDeletion(
      String projectId,
      Supplier<Mono<T>> action) {
    return projectPort.findByIdAndNotDeleted(projectId)
        .switchIfEmpty(Mono.error(new DomainException(
            ProjectErrorCode.NOT_FOUND)))
        .flatMap(project -> Mono.defer(() -> lockWorkspaceInShareMode(project.getWorkspaceId())
            .then(lockProjectForUpdate(projectId))
            .then(Mono.defer(action)))
            .as(transactionalOperator::transactional)
            .retryWhen(lockRetry(projectId)));
  }

  private Mono<Void> lockProjectInShareMode(String projectId) {
    return projectPort.lockByIdAndNotDeletedInShareMode(projectId)
        .switchIfEmpty(Mono.error(new DomainException(
            ProjectErrorCode.NOT_FOUND)))
        .then();
  }

  private Mono<Void> lockProjectForUpdate(String projectId) {
    return projectPort.lockByIdAndNotDeletedForUpdate(projectId)
        .switchIfEmpty(Mono.error(new DomainException(
            ProjectErrorCode.NOT_FOUND)))
        .then();
  }

  private Mono<Void> lockWorkspaceInShareMode(String workspaceId) {
    return workspacePort.findByIdAndNotDeletedInShareMode(workspaceId)
        .switchIfEmpty(Mono.error(new DomainException(
            WorkspaceErrorCode.NOT_FOUND)))
        .then();
  }

  private Retry lockRetry(String projectId) {
    return Retry.backoff(3, Duration.ofMillis(25))
        .maxBackoff(Duration.ofMillis(250))
        .jitter(0.5)
        .filter(PessimisticLockingFailureException.class::isInstance)
        .onRetryExhaustedThrow((spec, signal) -> signal.failure())
        .doBeforeRetry(signal -> log.warn(
            "Retrying project mutation transaction: projectId={}, retry={}",
            projectId, signal.totalRetries() + 1));
  }

}
