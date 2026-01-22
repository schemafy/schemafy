package com.schemafy.core.project.service;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.CreateWorkspaceInvitationRequest;
import com.schemafy.core.project.controller.dto.response.WorkspaceInvitationResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceMemberResponse;
import com.schemafy.core.project.repository.WorkspaceInvitationRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceInvitation;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
public class WorkspaceInvitationService {

  private static final Logger log = LoggerFactory.getLogger(
      WorkspaceInvitationService.class);
  private static final int WORKSPACE_MAX_MEMBERS_COUNT = 30;

  private final TransactionalOperator transactionalOperator;
  private final WorkspaceInvitationRepository invitationRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository memberRepository;
  private final UserRepository userRepository;

  public Mono<WorkspaceInvitation> createInvitation(
      String workspaceId,
      CreateWorkspaceInvitationRequest request,
      String currentUserId) {
    return validateAdmin(workspaceId, currentUserId)
        .then(findWorkspaceOrThrow(workspaceId))
        .flatMap(workspace -> checkNotAlreadyMemberByEmail(
            workspaceId, request.email())
            .then(checkDuplicatePendingInvitation(
                workspaceId, request.email()))
            .thenReturn(workspace))
        .flatMap(workspace -> {
          WorkspaceInvitation invitation = WorkspaceInvitation.create(
              workspaceId,
              request.email(),
              request.role(),
              currentUserId);

          return invitationRepository.save(invitation);
        })
        .as(transactionalOperator::transactional);
  }

  public Mono<PageResponse<WorkspaceInvitationResponse>> listInvitations(
      String workspaceId,
      String currentUserId,
      int page,
      int size) {
    return validateAdmin(workspaceId, currentUserId)
        .then(invitationRepository.countByWorkspaceIdAndNotDeleted(workspaceId))
        .flatMap(totalElements -> invitationRepository
            .findByWorkspaceIdAndNotDeleted(workspaceId, size, page * size)
            .map(WorkspaceInvitationResponse::of)
            .collectList()
            .map(invitations -> PageResponse.of(
                invitations, page, size, totalElements)));
  }

  public Mono<PageResponse<WorkspaceInvitationResponse>> listMyInvitations(
      String currentUserId,
      int page,
      int size) {
    return userRepository.findById(currentUserId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.USER_NOT_FOUND)))
        .flatMap(user -> {
          String email = user.getEmail();
          return invitationRepository.countPendingByEmail(email)
              .flatMap(totalElements -> invitationRepository
                  .findPendingByEmail(email, size, page * size)
                  .map(WorkspaceInvitationResponse::of)
                  .collectList()
                  .map(invitations -> PageResponse.of(
                      invitations, page, size, totalElements)));
        });
  }

  public Mono<WorkspaceMemberResponse> acceptInvitation(
      String invitationId,
      String currentUserId) {
    return userRepository.findById(currentUserId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.USER_NOT_FOUND)))
        .flatMap(user -> findInvitationOrThrow(invitationId)
            .flatMap(invitation -> {
              invitation.validateInvitedEmailMatches(user.getEmail());
              return checkNotAlreadyMember(invitation.getWorkspaceId(), currentUserId)
                  .then(checkMemberLimit(invitation.getWorkspaceId()))
                  .then(Mono.defer(() -> {
                    invitation.accept();

                    WorkspaceMember member = WorkspaceMember.create(
                        invitation.getWorkspaceId(),
                        currentUserId,
                        invitation.getRoleAsEnum());

                    return invitationRepository.save(invitation)
                        .then(memberRepository.save(member))
                        .flatMap(this::buildMemberResponse);
                  }));
            }))
        .retryWhen(Retry.max(3)
            .filter(OptimisticLockingFailureException.class::isInstance)
            .doBeforeRetry(signal -> log.warn(
                "Retrying due to concurrent modification: invitationId={}", invitationId)))
        .as(transactionalOperator::transactional)
        .onErrorMap(OptimisticLockingFailureException.class, e -> new BusinessException(
            ErrorCode.INVITATION_CONCURRENT_MODIFICATION));
  }

  public Mono<Void> rejectInvitation(
      String invitationId,
      String currentUserId) {
    return userRepository.findById(currentUserId)
        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
        .flatMap(user -> findInvitationOrThrow(invitationId)
            .flatMap(invitation -> {
              invitation.validateInvitedEmailMatches(user.getEmail());
              invitation.reject();
              return invitationRepository.save(invitation);
            }))
        .retryWhen(Retry.max(3)
            .filter(OptimisticLockingFailureException.class::isInstance)
            .doBeforeRetry(signal -> log.warn(
                "Retrying due to concurrent modification: invitationId={}", invitationId)))
        .as(transactionalOperator::transactional)
        .onErrorMap(OptimisticLockingFailureException.class, e -> new BusinessException(
            ErrorCode.INVITATION_CONCURRENT_MODIFICATION)).then();
  }

  private Mono<WorkspaceInvitation> findInvitationOrThrow(String invitationId) {
    return invitationRepository.findByIdAndNotDeleted(invitationId)
        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVITATION_NOT_FOUND)));
  }

  private Mono<Workspace> findWorkspaceOrThrow(String workspaceId) {
    return workspaceRepository.findByIdAndNotDeleted(workspaceId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND)));
  }

  private Mono<Void> validateAdmin(String workspaceId, String userId) {
    return memberRepository
        .findByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED)))
        .flatMap(member -> {
          if (!member.isAdmin()) {
            return Mono.error(new BusinessException(
                ErrorCode.WORKSPACE_ADMIN_REQUIRED));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> checkDuplicatePendingInvitation(
      String workspaceId,
      String email) {
    return invitationRepository
        .countPendingByWorkspaceAndEmail(workspaceId, email)
        .flatMap(count -> {
          if (count > 0) {
            log.info("Duplicate pending invitation: workspace={}, email={}",
                workspaceId, email);
            return Mono.error(new BusinessException(
                ErrorCode.INVITATION_ALREADY_EXISTS));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> checkNotAlreadyMember(String workspaceId, String userId) {
    return memberRepository
        .existsByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .flatMap(exists -> {
          if (exists) {
            return Mono.error(new BusinessException(
                ErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> checkMemberLimit(String workspaceId) {
    return memberRepository.countByWorkspaceIdAndNotDeleted(workspaceId)
        .flatMap(count -> {
          if (count >= WORKSPACE_MAX_MEMBERS_COUNT) {
            log.info("Workspace member limit exceeded: workspaceId={}",
                workspaceId);
            return Mono.error(new BusinessException(
                ErrorCode.WORKSPACE_MEMBER_LIMIT_EXCEED));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> checkNotAlreadyMemberByEmail(
      String workspaceId,
      String email) {
    return userRepository.findByEmail(email.toLowerCase())
        .flatMap(user -> memberRepository
            .existsByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, user.getId())
            .flatMap(exists -> {
              if (exists) {
                return Mono.error(new BusinessException(
                    ErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER));
              }
              return Mono.empty();
            }))
        .then();
  }

  private Mono<WorkspaceMemberResponse> buildMemberResponse(
      WorkspaceMember member) {
    return userRepository.findById(member.getUserId())
        .map(user -> WorkspaceMemberResponse.of(member, user));
  }

}
