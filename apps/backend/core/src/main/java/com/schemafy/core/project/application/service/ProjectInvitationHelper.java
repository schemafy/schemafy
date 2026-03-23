package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.InvitationType;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.core.user.application.port.out.FindUserByEmailPort;
import com.schemafy.core.user.domain.Email;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ProjectInvitationHelper {

  private static final Logger log = LoggerFactory.getLogger(
      ProjectInvitationHelper.class);

  private final InvitationPort invitationPort;
  private final ProjectPort projectPort;
  private final ProjectMemberPort projectMemberPort;
  private final FindUserByEmailPort findUserByEmailPort;
  private final UlidGeneratorPort ulidGeneratorPort;

  Mono<Invitation> findInvitationOrThrow(String invitationId) {
    return invitationPort.findByIdAndNotDeleted(invitationId)
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.INVITATION_NOT_FOUND)));
  }

  Mono<Project> findProjectOrThrow(String projectId) {
    return projectPort.findByIdAndNotDeleted(projectId)
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.NOT_FOUND)));
  }

  Mono<Void> validateProjectAdmin(String projectId, String userId) {
    return projectMemberPort
        .findByProjectIdAndUserIdAndNotDeleted(projectId, userId)
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.ACCESS_DENIED)))
        .flatMap(member -> {
          if (!member.isAdmin()) {
            return Mono.error(
                new DomainException(ProjectErrorCode.ADMIN_REQUIRED));
          }
          return Mono.empty();
        });
  }

  Mono<Void> checkDuplicatePendingInvitation(String projectId, Email email) {
    return invitationPort.countByTargetAndEmailAndStatus(
        InvitationType.PROJECT.name(),
        projectId,
        email.address(),
        InvitationStatus.PENDING.name())
        .flatMap(count -> {
          if (count > 0) {
            log.warn("Duplicate pending invitation: project={}, email={}",
                projectId, email.address());
            return Mono.error(new DomainException(
                ProjectErrorCode.INVITATION_ALREADY_EXISTS));
          }
          return Mono.empty();
        });
  }

  Mono<Void> checkNotAlreadyProjectMember(String projectId, String userId) {
    return projectMemberPort
        .existsByProjectIdAndUserIdAndNotDeleted(projectId, userId)
        .flatMap(exists -> {
          if (exists) {
            return Mono.error(new DomainException(
                ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT));
          }
          return Mono.empty();
        });
  }

  Mono<Void> checkNotAlreadyProjectMemberByEmail(String projectId, Email email) {
    return findUserByEmailPort.findUserByEmail(email.address())
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> projectMemberPort
            .existsByProjectIdAndUserIdAndNotDeleted(projectId, user.id())
            .flatMap(exists -> {
              if (exists) {
                return Mono.error(new DomainException(
                    ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT));
              }
              return Mono.empty();
            }))
        .then();
  }

  Mono<ProjectMember> saveOrRestoreProjectMember(
      String projectId,
      String userId,
      ProjectRole role) {
    return projectMemberPort.findLatestByProjectIdAndUserId(projectId, userId)
        .flatMap(existing -> {
          if (!existing.isDeleted()) {
            return Mono.error(new DomainException(
                ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT));
          }
          existing.restore();
          existing.updateRole(role);
          return projectMemberPort.save(existing);
        })
        .switchIfEmpty(Mono.defer(() -> Mono
            .fromCallable(ulidGeneratorPort::generate)
            .flatMap(id -> projectMemberPort.save(
                ProjectMember.create(id, projectId, userId, role)))));
  }

}
