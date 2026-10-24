package com.schemafy.core.user.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.mcp.application.port.out.RevokeMcpTokenPort;
import com.schemafy.core.project.application.port.in.LeaveWorkspaceCommand;
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
import com.schemafy.core.user.application.port.in.WithdrawUserCommand;
import com.schemafy.core.user.application.port.in.WithdrawUserUseCase;
import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.application.port.out.UpdateUserPort;
import com.schemafy.core.user.domain.AuthTokenType;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.UserStatus;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Service
@RequiredArgsConstructor
class WithdrawUserService implements WithdrawUserUseCase {

  private static final Duration WITHDRAWAL_TIMEOUT = Duration.ofSeconds(10);
  private static final int MAX_SCOPE_RETRIES = 3;

  private final TransactionalOperator transactionalOperator;
  private final FindUserByIdPort findUserByIdPort;
  private final UpdateUserPort updateUserPort;
  private final WorkspacePort workspacePort;
  private final ProjectPort projectPort;
  private final WorkspaceMemberPort workspaceMemberPort;
  private final ProjectMemberPort projectMemberPort;
  private final LeaveWorkspaceUseCase leaveWorkspaceUseCase;
  private final InvitationPort invitationPort;
  private final RevokeMcpTokenPort revokeMcpTokenPort;
  private final AuthTokenPort authTokenPort;

  @Override
  public Mono<Void> withdrawUser(WithdrawUserCommand command) {
    Mono<String> withdrawal = Mono.defer(() -> discoverScope(command.userId())
            .flatMap(scope -> withdrawInTransaction(command.userId(), scope)
                .as(transactionalOperator::transactional)))
        .retryWhen(Retry.max(MAX_SCOPE_RETRIES)
            .filter(DomainException.hasErrorCode(
                UserErrorCode.WITHDRAWAL_CONCURRENTLY_CHANGED))
            .doBeforeRetry(signal -> log.info(
                "Retrying user withdrawal after scope change: userId={}, retry={}",
                command.userId(), signal.totalRetries() + 1)))
        .onErrorMap(Exceptions::isRetryExhausted,
            error -> new DomainException(UserErrorCode.WITHDRAWAL_CONCURRENTLY_CHANGED))
        .timeout(WITHDRAWAL_TIMEOUT)
        .onErrorMap(TimeoutException.class,
            error -> new DomainException(UserErrorCode.WITHDRAWAL_TIMEOUT))
        .doOnError(error -> log.warn(
            "User withdrawal failed: userId={}, errorType={}",
            command.userId(),
            error.getClass().getSimpleName()));

    return withdrawal
        .flatMap(this::clearAuthTokens);
  }

  private Mono<WithdrawalScope> discoverScope(String userId) {
    return workspacePort.findByUserId(userId)
        .map(Workspace::getId)
        .distinct()
        .sort()
        .collectList()
        .flatMap(workspaceIds -> projectPort
            .findByUserIdAndWorkspaceMembershipOrProjectMembershipAndNotDeleted(
                userId)
            .map(Project::getId)
            .distinct()
            .sort()
            .collectList()
            .map(projectIds -> new WithdrawalScope(workspaceIds, projectIds)));
  }

  private Mono<String> withdrawInTransaction(
      String userId,
      WithdrawalScope candidateScope) {
    return lockWorkspaces(candidateScope.workspaceIds())
        .then(lockProjects(candidateScope.projectIds()))
        .then(findUserByIdPort
            .findUserByIdForUpdate(userId)
            .filter(user -> user.status() == UserStatus.ACTIVE)
            .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)))
            .flatMap(user -> rereadScope(user.id(), candidateScope)
                .flatMap(workspaces -> validateWorkspaceDepartures(user.id(),
                    workspaces)
                    .then(applyWithdrawal(user, workspaces)
                        .thenReturn(user.email())))));
  }

  private Mono<Void> lockWorkspaces(List<String> workspaceIds) {
    return Flux.fromIterable(workspaceIds)
        .concatMap(workspaceId -> workspacePort.findByIdAndNotDeletedForUpdate(
            workspaceId)
            .switchIfEmpty(Mono.error(new DomainException(
                UserErrorCode.WITHDRAWAL_CONCURRENTLY_CHANGED))))
        .then();
  }

  private Mono<Void> lockProjects(List<String> projectIds) {
    return Flux.fromIterable(projectIds)
        .concatMap(projectId -> projectPort.findByIdAndNotDeletedForUpdate(
            projectId)
            .switchIfEmpty(Mono.error(new DomainException(
                UserErrorCode.WITHDRAWAL_CONCURRENTLY_CHANGED))))
        .then();
  }

  private Mono<List<Workspace>> rereadScope(
      String userId,
      WithdrawalScope candidateScope) {
    return workspacePort.findByUserId(userId)
        .sort((left, right) -> left.getId().compareTo(right.getId()))
        .collectList()
        .flatMap(workspaces -> projectPort
            .findByUserIdAndWorkspaceMembershipOrProjectMembershipAndNotDeleted(
                userId)
            .map(Project::getId)
            .distinct()
            .sort()
            .collectList()
            .flatMap(projectIds -> {
              WithdrawalScope currentScope = new WithdrawalScope(
                  workspaces.stream().map(Workspace::getId).toList(),
                  projectIds);
              if (!candidateScope.equals(currentScope)) {
                return Mono.error(new DomainException(
                    UserErrorCode.WITHDRAWAL_CONCURRENTLY_CHANGED));
              }
              return Mono.just(workspaces);
            }));
  }

  private Mono<Void> validateWorkspaceDepartures(
      String userId,
      List<Workspace> workspaces) {
    return Flux.fromIterable(workspaces)
        .concatMap(workspace -> Mono.zip(
            workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(
                workspace.getId(), userId)
                .switchIfEmpty(Mono.error(new DomainException(
                    WorkspaceErrorCode.MEMBER_NOT_FOUND))),
            workspaceMemberPort.countByWorkspaceIdAndNotDeleted(workspace.getId()),
            workspaceMemberPort.countByWorkspaceIdAndRoleAndNotDeleted(
                workspace.getId(), WorkspaceRole.ADMIN.name())))
        .doOnNext(tuple -> validateWorkspaceDeparture(
            tuple.getT1(), tuple.getT2(), tuple.getT3())))
        .then();
  }

  private void validateWorkspaceDeparture(
      WorkspaceMember member,
      long totalMembers,
      long adminCount) {
    if (member.isAdmin() && totalMembers > 1 && adminCount <= 1) {
      throw new DomainException(WorkspaceErrorCode.LAST_ADMIN_CANNOT_LEAVE);
    }
  }

  private Mono<Void> applyWithdrawal(
      User user,
      List<Workspace> workspaces) {
    return Mono.defer(() -> invitationPort.cancelPendingByInvitedBy(user.id()))
        .then(Mono.defer(() -> invitationPort.cancelPendingByInvitedEmail(
            user.email())))
        .then(Mono.defer(() -> revokeMcpTokenPort.revokeAllMcpTokensByUserId(
            user.id(), Instant.now())))
        .thenMany(Flux.fromIterable(workspaces))
        .concatMap(workspace -> leaveWorkspaceUseCase.leaveWorkspace(
            new LeaveWorkspaceCommand(workspace.getId(), user.id())))
        .then(Mono.defer(() -> projectMemberPort.softDeleteByUserId(user.id())))
        .then(Mono.defer(() -> updateUserPort.updateUser(user.withdraw(Instant.now()))))
        .then();
  }

  private Mono<Void> clearAuthTokens(String email) {
    return clearAuthToken(AuthTokenType.EMAIL_VERIFICATION, email)
        .then(clearAuthToken(AuthTokenType.SIGNUP_VERIFICATION, email))
        .timeout(Duration.ofSeconds(5))
        .onErrorResume(error -> {
          log.warn("Failed to clear post-withdrawal auth tokens", error);
          return Mono.empty();
        });
  }

  private Mono<Void> clearAuthToken(AuthTokenType tokenType, String email) {
    return authTokenPort.delete(tokenType, email)
        .onErrorResume(error -> {
          log.warn("Failed to clear post-withdrawal auth token: type={}",
              tokenType, error);
          return Mono.empty();
        });
  }

  private record WithdrawalScope(
      List<String> workspaceIds,
      List<String> projectIds) {
  }

}
