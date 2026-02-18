package com.schemafy.core.project.service;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.CreateProjectInvitationRequest;
import com.schemafy.core.project.controller.dto.response.ProjectInvitationResponse;
import com.schemafy.core.project.controller.dto.response.ProjectMemberResponse;
import com.schemafy.core.project.repository.InvitationRepository;
import com.schemafy.core.project.repository.ProjectMemberRepository;
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.entity.Invitation;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.project.repository.vo.InvitationType;
import com.schemafy.core.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
public class ProjectInvitationService {

  private static final Logger log = LoggerFactory.getLogger(ProjectInvitationService.class);

  private final TransactionalOperator transactionalOperator;
  private final InvitationRepository invitationRepository;
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;

  public Mono<Invitation> createInvitation(
      String workspaceId,
      String projectId,
      CreateProjectInvitationRequest request,
      String currentUserId) {
    return validateProjectAdmin(projectId, currentUserId)
        .then(findProjectOrThrow(projectId))
        .flatMap(project -> {
          project.belongsToWorkspace(workspaceId);
          return Mono.just(project);
        })
        .flatMap(project -> checkNotAlreadyProjectMemberByEmail(
            projectId, request.email())
            .then(checkDuplicatePendingInvitation(
                projectId, request.email()))
            .thenReturn(project))
        .flatMap(project -> {
          Invitation invitation = Invitation.createProjectInvitation(
              projectId,
              workspaceId,
              request.email(),
              request.role(),
              currentUserId);

          return invitationRepository.save(invitation);
        })
        .as(transactionalOperator::transactional);
  }

  public Mono<PageResponse<ProjectInvitationResponse>> getInvitations(
      String workspaceId,
      String projectId,
      String currentUserId,
      int page,
      int size) {
    return validateProjectAdmin(projectId, currentUserId)
        .then(invitationRepository.countProjectInvitations(projectId))
        .flatMap(totalElements -> invitationRepository
            .findProjectInvitations(projectId, size, page * size)
            .map(ProjectInvitationResponse::of)
            .collectList()
            .map(invitations -> PageResponse.of(
                invitations, page, size, totalElements)));
  }

  public Mono<PageResponse<ProjectInvitationResponse>> getMyInvitations(
      String currentUserId,
      int page,
      int size) {
    return userRepository.findById(currentUserId)
        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
        .flatMap(user -> {
          String email = user.getEmail();
          String targetType = InvitationType.PROJECT.getValue();

          return invitationRepository.countPendingByEmailAndType(email, targetType)
              .flatMap(totalElements -> invitationRepository
                  .findPendingByEmailAndType(email, targetType, size, page * size)
                  .map(ProjectInvitationResponse::of)
                  .collectList()
                  .map(invitations -> PageResponse.of(
                      invitations, page, size, totalElements)));
        });
  }

  public Mono<ProjectMemberResponse> acceptInvitation(
      String invitationId,
      String currentUserId) {
    return userRepository.findById(currentUserId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.USER_NOT_FOUND)))
        .flatMap(user -> findInvitationOrThrow(invitationId)
            .flatMap(invitation -> {
              if (!invitation.getTargetTypeAsEnum().isProject()) {
                return Mono.error(new BusinessException(
                    ErrorCode.INVITATION_TYPE_MISMATCH));
              }

              invitation.validateInvitedEmailMatches(user.getEmail());
              return checkNotAlreadyProjectMember(invitation.getProjectId(), currentUserId)
                  .then(Mono.defer(() -> {
                    log.info("Accepting invitation: id={}, status={}", invitation.getId(), invitation.getStatus());
                    invitation.accept();

                    ProjectMember member = ProjectMember.create(
                        invitation.getProjectId(),
                        currentUserId,
                        invitation.getProjectRole());

                    return invitationRepository.save(invitation)
                        .then(projectMemberRepository.save(member))
                        .flatMap(this::buildMemberResponse);
                  }));
            }))
        .as(transactionalOperator::transactional)
        .retryWhen(Retry.max(3)
            .filter(OptimisticLockingFailureException.class::isInstance)
            .doBeforeRetry(signal -> log.warn(
                "Retrying due to concurrent modification: invitationId={}", invitationId)))
        .onErrorMap(OptimisticLockingFailureException.class, e -> new BusinessException(
            ErrorCode.INVITATION_CONCURRENT_MODIFICATION));
  }

  public Mono<Void> rejectInvitation(
      String invitationId,
      String currentUserId) {

    return userRepository.findById(currentUserId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.USER_NOT_FOUND)))
        .flatMap(user -> findInvitationOrThrow(invitationId)
            .flatMap(invitation -> {
              if (!invitation.getTargetTypeAsEnum().isProject()) {
                return Mono.error(new BusinessException(
                    ErrorCode.INVITATION_TYPE_MISMATCH));
              }

              invitation.validateInvitedEmailMatches(user.getEmail());
              invitation.reject();
              return invitationRepository.save(invitation);
            }))
        .then()
        .as(transactionalOperator::transactional)
        .retryWhen(Retry.max(3)
            .filter(OptimisticLockingFailureException.class::isInstance)
            .doBeforeRetry(signal -> log.warn(
                "Retrying due to concurrent modification: invitationId={}", invitationId)))
        .onErrorMap(OptimisticLockingFailureException.class, error -> new BusinessException(
            ErrorCode.INVITATION_CONCURRENT_MODIFICATION));
  }

  private Mono<Invitation> findInvitationOrThrow(String invitationId) {
    return invitationRepository.findByIdAndNotDeleted(invitationId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.INVITATION_NOT_FOUND)));
  }

  private Mono<Project> findProjectOrThrow(String projectId) {
    return projectRepository.findByIdAndNotDeleted(projectId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.PROJECT_NOT_FOUND)));
  }

  private Mono<Void> validateProjectAdmin(
      String projectId,
      String userId) {
    return projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(projectId, userId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED)))
        .flatMap(member -> {
          if (!member.isAdmin()) {
            return Mono.error(new BusinessException(
                ErrorCode.PROJECT_ADMIN_REQUIRED));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> checkDuplicatePendingInvitation(
      String projectId,
      String email) {
    return invitationRepository
        .countPendingProjectInvitation(projectId, email)
        .flatMap(count -> {
          if (count > 0) {
            log.warn("Duplicate pending invitation: project={}, email={}",
                projectId, email);
            return Mono.error(new BusinessException(
                ErrorCode.INVITATION_ALREADY_EXISTS));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> checkNotAlreadyProjectMember(
      String projectId,
      String userId) {
    return projectMemberRepository
        .existsByProjectIdAndUserIdAndNotDeleted(projectId, userId)
        .flatMap(exists -> {
          if (exists) {
            return Mono.error(new BusinessException(
                ErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> checkNotAlreadyProjectMemberByEmail(
      String projectId,
      String email) {
    return userRepository.findByEmail(email.toLowerCase())
        .flatMap(user -> projectMemberRepository
            .existsByProjectIdAndUserIdAndNotDeleted(projectId, user.getId())
            .flatMap(exists -> {
              if (exists) {
                return Mono.error(new BusinessException(
                    ErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT));
              }
              return Mono.empty();
            }))
        .then();
  }

  private Mono<ProjectMemberResponse> buildMemberResponse(
      ProjectMember member) {
    return userRepository.findById(member.getUserId())
        .map(user -> ProjectMemberResponse.of(member, user));
  }

}
