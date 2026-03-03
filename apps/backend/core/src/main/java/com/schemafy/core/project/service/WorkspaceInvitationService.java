package com.schemafy.core.project.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.exception.ProjectErrorCode;
import com.schemafy.core.project.exception.WorkspaceErrorCode;
import com.schemafy.core.project.repository.InvitationRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Invitation;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.InvitationStatus;
import com.schemafy.core.project.repository.vo.InvitationType;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.project.service.dto.WorkspaceMemberDetail;
import com.schemafy.core.user.exception.UserErrorCode;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.domain.common.exception.DomainException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
public class WorkspaceInvitationService {

  private static final Logger log = LoggerFactory.getLogger(
      WorkspaceInvitationService.class);

  private final TransactionalOperator transactionalOperator;
  private final InvitationRepository invitationRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository memberRepository;
  private final UserRepository userRepository;
  private final ProjectService projectService;

  public Mono<Invitation> createInvitation(
      String workspaceId,
      String email,
      WorkspaceRole role,
      String currentUserId) {
    return validateAdmin(workspaceId, currentUserId)
        .then(findWorkspaceOrThrow(workspaceId))
        .flatMap(workspace -> checkNotAlreadyMemberByEmail(
            workspaceId, email)
            .then(checkDuplicatePendingInvitation(
                workspaceId, email))
            .thenReturn(workspace))
        .flatMap(workspace -> {
          Invitation invitation = Invitation.createWorkspaceInvitation(
              workspaceId,
              email,
              role,
              currentUserId);

          return invitationRepository.save(invitation);
        })
        .as(transactionalOperator::transactional);
  }

  public Mono<PageResponse<Invitation>> listInvitations(
      String workspaceId,
      String currentUserId,
      int page,
      int size) {
    String targetType = InvitationType.WORKSPACE.name();

    return validateAdmin(workspaceId, currentUserId)
        .then(invitationRepository.countByTarget(
            targetType,
            workspaceId))
        .flatMap(totalElements -> invitationRepository
            .findInvitationsByTargetAndId(
                targetType,
                workspaceId,
                size,
                page * size)
            .collectList()
            .map(invitations -> PageResponse.of(
                invitations, page, size, totalElements)));
  }

  public Mono<PageResponse<Invitation>> listMyInvitations(
      String currentUserId,
      int page,
      int size) {
    return userRepository.findById(currentUserId)
        .switchIfEmpty(Mono.error(
            new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> {
          String email = user.getEmail();
          String targetType = InvitationType.WORKSPACE.name();
          String pendingStatus = InvitationStatus.PENDING.name();

          return invitationRepository.countByEmailAndTypeAndStatus(
              email,
              targetType,
              pendingStatus)
              .flatMap(totalElements -> invitationRepository
                  .findByEmailAndTypeAndStatus(
                      email,
                      targetType,
                      pendingStatus,
                      size,
                      page * size)
                  .collectList()
                  .map(invitations -> PageResponse.of(
                      invitations, page, size, totalElements)));
        });
  }

  public Mono<WorkspaceMemberDetail> acceptInvitation(
      String invitationId,
      String currentUserId) {
    return userRepository.findById(currentUserId)
        .switchIfEmpty(Mono.error(
            new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> findInvitationOrThrow(invitationId)
            .flatMap(invitation -> {
              if (!invitation.getTargetTypeAsEnum().isWorkspace()) {
                return Mono.error(new DomainException(ProjectErrorCode.INVITATION_TYPE_MISMATCH));
              }

              invitation.validateInvitedEmailMatches(user.getEmail());
              return checkNotAlreadyMember(invitation.getWorkspaceId(), currentUserId)
                  .then(Mono.defer(() -> {
                    invitation.accept();

                    return invitationRepository.save(invitation)
                        .then(invitationRepository.updateStatusByTargetAndEmail(
                            invitation.getTargetType(),
                            invitation.getTargetId(),
                            invitation.getInvitedEmail(),
                            InvitationStatus.CANCELLED.name(),
                            InvitationStatus.PENDING.name(),
                            invitation.getId()))
                        .then(saveOrRestoreWorkspaceMember(invitation.getWorkspaceId(), currentUserId, invitation
                            .getWorkspaceRole()))
                        .onErrorResume(DataIntegrityViolationException.class, e -> {
                          log.warn("Concurrent member creation on invitation accept: invitationId={}", invitationId);
                          return Mono.error(new DomainException(
                              ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER));
                        })
                        .flatMap(savedMember -> projectService.propagateToExistingProjects(
                            invitation.getWorkspaceId(),
                            currentUserId,
                            invitation.getWorkspaceRole())
                            .then(Mono.just(savedMember)))
                        .flatMap(this::buildMemberDetail);
                  }));
            }))
        .as(transactionalOperator::transactional)
        .retryWhen(Retry.max(3)
            .filter(OptimisticLockingFailureException.class::isInstance)
            .doBeforeRetry(signal -> log.warn(
                "Retrying due to concurrent modification: invitationId={}", invitationId)))
        .onErrorMap(OptimisticLockingFailureException.class, e -> new DomainException(
            ProjectErrorCode.INVITATION_CONCURRENT_PROCESSED));
  }

  public Mono<Void> rejectInvitation(
      String invitationId,
      String currentUserId) {
    return userRepository.findById(currentUserId)
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> findInvitationOrThrow(invitationId)
            .flatMap(invitation -> {
              if (!invitation.getTargetTypeAsEnum().isWorkspace()) {
                return Mono.error(new DomainException(ProjectErrorCode.INVITATION_TYPE_MISMATCH));
              }

              invitation.validateInvitedEmailMatches(user.getEmail());
              invitation.reject();
              return invitationRepository.save(invitation);
            }))
        .as(transactionalOperator::transactional)
        .retryWhen(Retry.max(3)
            .filter(OptimisticLockingFailureException.class::isInstance)
            .doBeforeRetry(signal -> log.warn(
                "Retrying due to concurrent modification: invitationId={}", invitationId)))
        .onErrorMap(OptimisticLockingFailureException.class, e -> new DomainException(
            ProjectErrorCode.INVITATION_CONCURRENT_PROCESSED)).then();
  }

  private Mono<Invitation> findInvitationOrThrow(String invitationId) {
    return invitationRepository.findByIdAndNotDeleted(invitationId)
        .switchIfEmpty(Mono.error(new DomainException(ProjectErrorCode.INVITATION_NOT_FOUND)));
  }

  private Mono<Workspace> findWorkspaceOrThrow(String workspaceId) {
    return workspaceRepository.findByIdAndNotDeleted(workspaceId)
        .switchIfEmpty(Mono.error(
            new DomainException(WorkspaceErrorCode.NOT_FOUND)));
  }

  private Mono<Void> validateAdmin(String workspaceId, String userId) {
    return memberRepository
        .findByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .switchIfEmpty(Mono.error(
            new DomainException(WorkspaceErrorCode.ACCESS_DENIED)))
        .flatMap(member -> {
          if (!member.isAdmin()) {
            return Mono.error(new DomainException(WorkspaceErrorCode.ADMIN_REQUIRED));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> checkDuplicatePendingInvitation(
      String workspaceId,
      String email) {
    return invitationRepository
        .countByTargetAndEmailAndStatus(
            InvitationType.WORKSPACE.name(),
            workspaceId,
            email,
            InvitationStatus.PENDING.name())
        .flatMap(count -> {
          if (count > 0) {
            log.warn("Duplicate pending invitation: workspace={}, email={}",
                workspaceId, email);
            return Mono.error(new DomainException(ProjectErrorCode.INVITATION_ALREADY_EXISTS));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> checkNotAlreadyMember(String workspaceId, String userId) {
    return memberRepository
        .existsByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .flatMap(exists -> {
          if (exists) {
            return Mono.error(new DomainException(ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER));
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
                return Mono.error(new DomainException(ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER));
              }
              return Mono.empty();
            }))
        .then();
  }

  private Mono<WorkspaceMember> saveOrRestoreWorkspaceMember(
      String workspaceId,
      String userId,
      WorkspaceRole role) {
    return memberRepository.findLatestByWorkspaceIdAndUserId(workspaceId, userId)
        .flatMap(existing -> {
          if (!existing.isDeleted()) {
            return Mono.error(new DomainException(ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER));
          }
          existing.restore();
          existing.updateRole(role);
          return memberRepository.save(existing);
        })
        .switchIfEmpty(Mono.defer(() -> memberRepository.save(WorkspaceMember.create(workspaceId, userId, role))));
  }

  private Mono<WorkspaceMemberDetail> buildMemberDetail(
      WorkspaceMember member) {
    return userRepository.findById(member.getUserId())
        .map(user -> new WorkspaceMemberDetail(member, user));
  }

}
