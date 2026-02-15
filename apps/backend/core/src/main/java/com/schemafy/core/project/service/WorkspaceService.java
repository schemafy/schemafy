package com.schemafy.core.project.service;

import java.util.function.Consumer;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.AddWorkspaceMemberRequest;
import com.schemafy.core.project.controller.dto.request.CreateWorkspaceRequest;
import com.schemafy.core.project.controller.dto.request.UpdateMemberRoleRequest;
import com.schemafy.core.project.controller.dto.request.UpdateWorkspaceRequest;
import com.schemafy.core.project.controller.dto.response.WorkspaceMemberResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceSummaryResponse;
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;

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
  private final ProjectService projectService;
  private final UserRepository userRepository;

  public Mono<WorkspaceResponse> createWorkspace(
      CreateWorkspaceRequest request, String userId) {
    return Mono.defer(() -> {
      Workspace workspace = Workspace.create(request.name(),
          request.description());

      WorkspaceMember adminMember = WorkspaceMember.create(
          workspace.getId(), userId, WorkspaceRole.ADMIN);

      return workspaceRepository.save(workspace)
          .flatMap(savedWorkspace -> workspaceMemberRepository.save(
              adminMember).thenReturn(savedWorkspace))
          .flatMap(savedWorkspace -> buildWorkspaceResponse(
              savedWorkspace, userId));
    }).as(transactionalOperator::transactional);
  }

  public Mono<PageResponse<WorkspaceSummaryResponse>> getWorkspaces(
      String userId, int page, int size) {
    return workspaceRepository.countByUserId(userId)
        .flatMap(sizeOfWorkspace -> workspaceRepository
            .findByUserIdWithPaging(userId, size, page * size)
            .flatMap(workspace -> workspaceMemberRepository
                .countByWorkspaceIdAndNotDeleted(
                    workspace.getId())
                .map(memberCount -> WorkspaceSummaryResponse
                    .of(workspace, memberCount)))
            .collectList()
            .map(content -> PageResponse.of(content, page, size,
                sizeOfWorkspace)));
  }

  public Mono<WorkspaceResponse> getWorkspace(String workspaceId,
      String userId) {
    return validateMemberAccess(workspaceId, userId)
        .then(findWorkspaceOrThrow(workspaceId))
        .flatMap(workspace -> buildWorkspaceResponse(workspace, userId));
  }

  public Mono<WorkspaceResponse> updateWorkspace(String workspaceId,
      UpdateWorkspaceRequest request, String userId) {
    return validateAdminAccess(workspaceId, userId)
        .then(findWorkspaceOrThrow(workspaceId))
        .flatMap(workspace -> {
          workspace.update(request.name(), request.description());
          return workspaceRepository.save(workspace);
        })
        .flatMap(savedWorkspace -> buildWorkspaceResponse(
            savedWorkspace, userId))
        .as(transactionalOperator::transactional);
  }

  public Mono<Void> deleteWorkspace(String workspaceId, String userId) {
    return validateAdminAccess(workspaceId, userId)
        .then(findWorkspaceOrThrow(workspaceId))
        .flatMap(workspace -> {
          if (workspace.isDeleted()) {
            return Mono.error(new BusinessException(
                ErrorCode.WORKSPACE_ALREADY_DELETED));
          }
          workspace.delete();
          return workspaceRepository.save(workspace)
              .then(workspaceMemberRepository
                  .softDeleteByWorkspaceId(
                      workspaceId));
        }).as(transactionalOperator::transactional);
  }

  public Mono<PageResponse<WorkspaceMemberResponse>> getMembers(
      String workspaceId, String userId, int page, int size) {
    return validateMemberAccess(workspaceId, userId).then(
        workspaceMemberRepository.countByWorkspaceIdAndNotDeleted(
            workspaceId))
        .flatMap(totalElements -> {
          int offset = page * size;
          return workspaceMemberRepository
              .findByWorkspaceIdAndNotDeleted(
                  workspaceId, size, offset)
              .flatMap(this::buildMemberResponse)
              .collectList()
              .map(members -> PageResponse.of(members, page, size,
                  totalElements));
        });
  }

  // Soft delete된 멤버는 재활성화
  public Mono<WorkspaceMemberResponse> addMember(
      String workspaceId,
      AddWorkspaceMemberRequest request,
      String currentUserId) {

    return validateAdminAccess(workspaceId, currentUserId)
        .then(findUserByEmailOrThrow(request.email()))
        .flatMap(targetUser -> workspaceMemberRepository
            .findLatestByWorkspaceIdAndUserId(workspaceId,
                targetUser.getId())
            .flatMap(existing -> {
              if (!existing.isDeleted()) {
                log.warn(
                    "Member already exists and is active: memberId={}",
                    existing.getId());
                return Mono.error(new BusinessException(
                    ErrorCode.WORKSPACE_MEMBER_ALREADY_EXISTS));
              }

              // 삭제된 멤버 복원
              return workspaceMemberRepository
                  .restoreDeletedMember(existing.getId(),
                      request.role().getValue())
                  .flatMap(updatedCount -> {
                    if (updatedCount == 0) {
                      return Mono.error(new BusinessException(
                          ErrorCode.WORKSPACE_MEMBER_ALREADY_EXISTS));
                    }
                    return workspaceMemberRepository
                        .findById(existing.getId());
                  });
            })
            .switchIfEmpty(Mono.defer(() -> {
              // 신규 멤버 생성
              WorkspaceMember newMember = WorkspaceMember.create(
                  workspaceId,
                  targetUser.getId(),
                  request.role());
              return workspaceMemberRepository.save(newMember);
            })))
        .flatMap(savedMember -> projectService.propagateToExistingProjects(
            workspaceId,
            savedMember.getUserId(),
            savedMember.getRoleAsEnum())
            .then(Mono.just(savedMember)))
        .flatMap(this::buildMemberResponse)
        .onErrorResume(error -> {
          if (error instanceof DataIntegrityViolationException) {
            log.warn(
                "Duplicate key constraint: workspaceId={}, email={}",
                workspaceId, request.email());
            return Mono.error(new BusinessException(
                ErrorCode.WORKSPACE_MEMBER_ALREADY_EXISTS));
          }
          return Mono.error(error);
        })
        .as(transactionalOperator::transactional);
  }

  private Mono<User> findUserByEmailOrThrow(String email) {
    return userRepository.findByEmail(email)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.USER_NOT_FOUND)));
  }

  private Mono<WorkspaceMemberResponse> buildMemberResponse(
      WorkspaceMember member) {
    return userRepository.findById(member.getUserId())
        .map(user -> WorkspaceMemberResponse.of(member, user));
  }

  private Mono<WorkspaceResponse> buildWorkspaceResponse(
      Workspace workspace, String userId) {
    return Mono.zip(
        workspaceMemberRepository.countByWorkspaceIdAndNotDeleted(
            workspace.getId()),
        projectRepository.countByWorkspaceIdAndNotDeleted(
            workspace.getId()),
        workspaceMemberRepository
            .findByWorkspaceIdAndUserIdAndNotDeleted(
                workspace.getId(), userId)
            .map(WorkspaceMember::getRole)
            .defaultIfEmpty(WorkspaceRole.MEMBER.getValue()))
        .map(tuple -> WorkspaceResponse.of(
            workspace,
            tuple.getT1(), // memberCount
            tuple.getT2(), // projectCount
            tuple.getT3()  // currentUserRole
        ));
  }

  private Mono<Workspace> findWorkspaceOrThrow(String workspaceId) {
    return workspaceRepository.findByIdAndNotDeleted(workspaceId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND)));
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
        .then()
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
                return this.deleteWorkspace(workspaceId,
                    targetUserId);
              }

              return modifyMemberWithAdminGuard(
                  workspaceId,
                  member,
                  WorkspaceMember::delete);
            }))
        .then()
        .as(transactionalOperator::transactional);
  }

  public Mono<WorkspaceMemberResponse> updateMemberRole(
      String workspaceId,
      String targetUserId,
      UpdateMemberRoleRequest request,
      String currentUserId) {
    return validateAdminAccess(workspaceId, currentUserId)
        .then(findWorkspaceMemberByUserIdAndWorkspaceId(targetUserId,
            workspaceId))
        .flatMap(targetMember -> modifyMemberWithAdminGuard(workspaceId,
            targetMember, m -> m.updateRole(request.role())))
        .flatMap(this::buildMemberResponse)
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> validateMemberAccess(String workspaceId, String userId) {
    return workspaceMemberRepository
        .existsByWorkspaceIdAndUserIdAndNotDeleted(
            workspaceId, userId)
        .flatMap(exists -> {
          if (!exists) {
            return Mono.error(new BusinessException(
                ErrorCode.WORKSPACE_ACCESS_DENIED));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> validateAdminAccess(String workspaceId, String userId) {
    return findWorkspaceMemberByUserIdAndWorkspaceId(userId, workspaceId)
        .flatMap(member -> {
          if (!member.isAdmin()) {
            return Mono.error(new BusinessException(
                ErrorCode.WORKSPACE_ADMIN_REQUIRED));
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
            WorkspaceRole.ADMIN.getValue())
        .flatMap(count -> {
          if (count <= 1) {
            return Mono.error(new BusinessException(
                ErrorCode.LAST_ADMIN_CANNOT_LEAVE));
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
            new BusinessException(
                ErrorCode.WORKSPACE_MEMBER_NOT_FOUND)));
  }

}
