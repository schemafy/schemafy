package com.schemafy.core.project.service;

import java.util.function.Consumer;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.exception.WorkspaceErrorCode;
import com.schemafy.core.project.repository.InvitationRepository;
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.InvitationType;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.project.service.dto.WorkspaceDetail;
import com.schemafy.core.project.service.dto.WorkspaceMemberDetail;
import com.schemafy.domain.user.domain.exception.UserErrorCode;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.domain.common.exception.DomainException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

  private static final Logger log = LoggerFactory
      .getLogger(WorkspaceService.class);

  private final TransactionalOperator transactionalOperator;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final ProjectRepository projectRepository;
  private final InvitationRepository invitationRepository;
  private final ProjectService projectService;
  private final UserRepository userRepository;

  public Mono<WorkspaceDetail> createWorkspace(
      String name, String description, String userId) {
    return Mono.defer(() -> {
      Workspace workspace = Workspace.create(name,
          description);

      WorkspaceMember adminMember = WorkspaceMember.create(
          workspace.getId(), userId, WorkspaceRole.ADMIN);

      return workspaceRepository.save(workspace)
          .flatMap(savedWorkspace -> workspaceMemberRepository.save(
              adminMember).thenReturn(savedWorkspace))
          .flatMap(savedWorkspace -> buildWorkspaceDetail(
              savedWorkspace, userId));
    }).as(transactionalOperator::transactional);
  }

  public Mono<PageResponse<Workspace>> getWorkspaces(
      String userId, int page, int size) {
    return workspaceRepository.countByUserId(userId)
        .flatMap(sizeOfWorkspace -> workspaceRepository
            .findByUserIdWithPaging(userId, size, page * size)
            .collectList()
            .map(content -> PageResponse.of(content, page, size,
                sizeOfWorkspace)));
  }

  public Mono<WorkspaceDetail> getWorkspace(String workspaceId,
      String userId) {
    return validateMemberAccess(workspaceId, userId)
        .then(findWorkspaceOrThrow(workspaceId))
        .flatMap(workspace -> buildWorkspaceDetail(workspace, userId));
  }

  public Mono<WorkspaceDetail> updateWorkspace(String workspaceId,
      String name, String description, String userId) {
    return validateAdminAccess(workspaceId, userId)
        .then(findWorkspaceOrThrow(workspaceId))
        .flatMap(workspace -> {
          workspace.update(name, description);
          return workspaceRepository.save(workspace);
        })
        .flatMap(savedWorkspace -> buildWorkspaceDetail(
            savedWorkspace, userId))
        .as(transactionalOperator::transactional);
  }

  public Mono<Void> deleteWorkspace(String workspaceId, String userId) {
    return validateAdminAccess(workspaceId, userId)
        .then(doDeleteWorkspace(workspaceId))
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> doDeleteWorkspace(String workspaceId) {
    return findWorkspaceOrThrow(workspaceId)
        .flatMap(workspace -> {
          workspace.delete();
          return workspaceRepository.save(workspace)
              .then(softDeleteWorkspaceCascade(workspaceId));
        });
  }

  private Mono<Void> softDeleteWorkspaceCascade(String workspaceId) {
    return softDeleteWorkspaceProjects(workspaceId)
        .then(workspaceMemberRepository.softDeleteByWorkspaceId(workspaceId))
        .then(invitationRepository.softDeleteByTarget(
            InvitationType.WORKSPACE.name(),
            workspaceId))
        .then();
  }

  private Mono<Void> softDeleteWorkspaceProjects(String workspaceId) {
    return projectRepository.findByWorkspaceId(workspaceId)
        .concatMap(projectService::softDeleteProjectCascade)
        .then();
  }

  public Mono<PageResponse<WorkspaceMemberDetail>> getMembers(
      String workspaceId, String userId, int page, int size) {
    return validateMemberAccess(workspaceId, userId).then(
        workspaceMemberRepository.countByWorkspaceIdAndNotDeleted(
            workspaceId))
        .flatMap(totalElements -> {
          int offset = page * size;
          return workspaceMemberRepository
              .findByWorkspaceIdAndNotDeleted(
                  workspaceId, size, offset)
              .flatMap(this::buildMemberDetail)
              .collectList()
              .map(members -> PageResponse.of(members, page, size,
                  totalElements));
        });
  }

  // Soft delete된 멤버는 재활성화
  public Mono<WorkspaceMemberDetail> addMember(
      String workspaceId,
      String email,
      WorkspaceRole role,
      String currentUserId) {

    return validateAdminAccess(workspaceId, currentUserId)
        .then(findUserByEmailOrThrow(email))
        .flatMap(targetUser -> workspaceMemberRepository
            .findLatestByWorkspaceIdAndUserId(workspaceId,
                targetUser.getId())
            .flatMap(existing -> {
              if (!existing.isDeleted()) {
                log.warn(
                    "Member already exists and is active: memberId={}",
                    existing.getId());
                return Mono.error(new DomainException(
                    WorkspaceErrorCode.MEMBER_ALREADY_EXISTS));
              }

              // 삭제된 멤버 복원
              existing.restore();
              existing.updateRole(role);
              return workspaceMemberRepository.save(existing);
            })
            .switchIfEmpty(Mono.defer(() -> {
              // 신규 멤버 생성
              WorkspaceMember newMember = WorkspaceMember.create(
                  workspaceId,
                  targetUser.getId(),
                  role);
              return workspaceMemberRepository.save(newMember);
            })))
        .flatMap(savedMember -> projectService.propagateToExistingProjects(
            workspaceId,
            savedMember.getUserId(),
            savedMember.getRoleAsEnum())
            .then(Mono.just(savedMember)))
        .flatMap(this::buildMemberDetail)
        .onErrorResume(error -> {
          if (error instanceof DataIntegrityViolationException) {
            log.warn(
                "Duplicate key constraint: workspaceId={}, email={}",
                workspaceId, email);
            return Mono.error(new DomainException(
                WorkspaceErrorCode.MEMBER_ALREADY_EXISTS));
          }
          return Mono.error(error);
        })
        .as(transactionalOperator::transactional);
  }

  private Mono<User> findUserByEmailOrThrow(String email) {
    return userRepository.findByEmail(email)
        .switchIfEmpty(Mono.error(
            new DomainException(UserErrorCode.NOT_FOUND)));
  }

  private Mono<WorkspaceMemberDetail> buildMemberDetail(
      WorkspaceMember member) {
    return userRepository.findById(member.getUserId())
        .map(user -> new WorkspaceMemberDetail(member, user));
  }

  private Mono<WorkspaceDetail> buildWorkspaceDetail(
      Workspace workspace, String userId) {
    return Mono.zip(
        projectRepository.countByWorkspaceIdAndNotDeleted(
            workspace.getId()),
        workspaceMemberRepository
            .findByWorkspaceIdAndUserIdAndNotDeleted(
                workspace.getId(), userId)
            .switchIfEmpty(Mono.error(
                new DomainException(WorkspaceErrorCode.MEMBER_NOT_FOUND)))
            .map(WorkspaceMember::getRole))
        .map(tuple -> new WorkspaceDetail(
            workspace,
            tuple.getT1(),
            tuple.getT2()));
  }

  private Mono<Workspace> findWorkspaceOrThrow(String workspaceId) {
    return workspaceRepository.findByIdAndNotDeleted(workspaceId)
        .switchIfEmpty(Mono.error(
            new DomainException(WorkspaceErrorCode.NOT_FOUND)));
  }

  public Mono<Void> removeMember(
      String workspaceId,
      String targetUserId,
      String requesterId) {
    return validateAdminAccess(workspaceId, requesterId)
        .then(findWorkspaceMemberByUserIdAndWorkspaceId(
            targetUserId, workspaceId))
        .flatMap(targetMember -> modifyMemberWithAdminGuard(workspaceId,
            targetMember, WorkspaceMember::delete))
        .then(projectService.removeFromAllProjects(workspaceId, targetUserId))
        .as(transactionalOperator::transactional);
  }

  /** 셀프 워크스페이스 탈퇴 */
  public Mono<Void> leaveMember(
      String workspaceId,
      String targetUserId) {
    return findWorkspaceMemberByUserIdAndWorkspaceId(targetUserId,
        workspaceId)
        .flatMap(member -> workspaceMemberRepository
            .countByWorkspaceIdAndNotDeleted(workspaceId)
            .flatMap(totalMembers -> {
              if (totalMembers == 1) {
                return doDeleteWorkspace(workspaceId);
              }

              return modifyMemberWithAdminGuard(
                  workspaceId,
                  member,
                  WorkspaceMember::delete)
                  .then(projectService.removeFromAllProjects(workspaceId, targetUserId));
            }))
        .then()
        .as(transactionalOperator::transactional);
  }

  public Mono<WorkspaceMemberDetail> updateMemberRole(
      String workspaceId,
      String targetUserId,
      WorkspaceRole role,
      String currentUserId) {
    return validateAdminAccess(workspaceId, currentUserId)
        .then(findWorkspaceMemberByUserIdAndWorkspaceId(targetUserId,
            workspaceId))
        .flatMap(targetMember -> modifyMemberWithAdminGuard(workspaceId,
            targetMember, m -> m.updateRole(role)))
        .flatMap(savedMember -> projectService.updateRoleInAllProjects(workspaceId, targetUserId, role)
            .thenReturn(savedMember))
        .flatMap(this::buildMemberDetail)
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> validateMemberAccess(String workspaceId, String userId) {
    return workspaceMemberRepository
        .existsByWorkspaceIdAndUserIdAndNotDeleted(
            workspaceId, userId)
        .flatMap(exists -> {
          if (!exists) {
            return Mono.error(new DomainException(
                WorkspaceErrorCode.ACCESS_DENIED));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> validateAdminAccess(String workspaceId, String userId) {
    return findWorkspaceMemberByUserIdAndWorkspaceId(userId, workspaceId)
        .flatMap(member -> {
          if (!member.isAdmin()) {
            return Mono.error(new DomainException(
                WorkspaceErrorCode.ADMIN_REQUIRED));
          }
          return Mono.empty();
        });
  }

  private Mono<WorkspaceMember> modifyMemberWithAdminGuard(
      String workspaceId,
      WorkspaceMember member,
      Consumer<WorkspaceMember> action) {
    if (!member.isAdmin()) {
      action.accept(member);
      return workspaceMemberRepository.save(member);
    }

    return workspaceMemberRepository
        .countByWorkspaceIdAndRoleAndNotDeleted(workspaceId,
            WorkspaceRole.ADMIN.name())
        .flatMap(count -> {
          if (count <= 1) {
            return Mono.error(new DomainException(
                WorkspaceErrorCode.LAST_ADMIN_CANNOT_LEAVE));
          }
          action.accept(member);
          return workspaceMemberRepository.save(member);
        });
  }

  private Mono<WorkspaceMember> findWorkspaceMemberByUserIdAndWorkspaceId(
      String userId, String workspaceId) {
    return workspaceMemberRepository
        .findByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .switchIfEmpty(Mono.error(
            new DomainException(
                WorkspaceErrorCode.MEMBER_NOT_FOUND)));
  }

}
