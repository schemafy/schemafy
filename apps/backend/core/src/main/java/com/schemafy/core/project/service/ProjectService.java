package com.schemafy.core.project.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.CreateProjectRequest;
import com.schemafy.core.project.controller.dto.request.UpdateProjectMemberRoleRequest;
import com.schemafy.core.project.controller.dto.request.UpdateProjectRequest;
import com.schemafy.core.project.controller.dto.response.ProjectMemberResponse;
import com.schemafy.core.project.controller.dto.response.ProjectResponse;
import com.schemafy.core.project.controller.dto.response.ProjectSummaryResponse;
import com.schemafy.core.project.repository.ProjectMemberRepository;
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.project.repository.vo.ProjectSettings;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ProjectService {

  private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

  private final TransactionalOperator transactionalOperator;
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;

  public Mono<ProjectResponse> createProject(String workspaceId,
      CreateProjectRequest request, String userId) {
    return validateWorkspaceAdmin(workspaceId, userId).then(
        Mono.defer(() -> {
          ProjectSettings settings = request.getSettingsOrDefault();
          validateSettings(settings);

          Project project = Project.create(workspaceId,
              request.name(), request.description(), settings);

          ProjectMember adminMember = ProjectMember
              .create(project.getId(), userId, ProjectRole.ADMIN);

          return projectRepository.save(project)
              .flatMap(savedProject -> projectMemberRepository
                  .save(adminMember)
                  .thenReturn(savedProject))
              .flatMap(savedProject -> propagateWorkspaceMembersToProject(
                  savedProject.getId(), workspaceId, userId)
                  .thenReturn(savedProject))
              .flatMap(savedProject -> buildProjectResponse(
                  savedProject, userId));
        }))
        .as(transactionalOperator::transactional);
  }

  public Mono<PageResponse<ProjectSummaryResponse>> getProjects(
      String workspaceId, String userId, int page, int size) {
    return Mono.defer(() -> {
      int offset = page * size;
      return projectMemberRepository
          .countByWorkspaceIdAndUserId(workspaceId, userId)
          .flatMap(totalElements -> Mono.zip(
              projectRepository
                  .findByWorkspaceIdAndUserIdWithPaging(
                      workspaceId, userId, size,
                      offset)
                  .collectList(),
              projectMemberRepository
                  .findRolesByWorkspaceIdAndUserIdWithPaging(
                      workspaceId, userId, size,
                      offset)
                  .collectList())
              .flatMap(tuple -> {
                var projects = tuple.getT1();
                var roles = tuple.getT2();
                return Flux.range(0, projects.size())
                    .flatMap(i -> {
                      var project = projects
                          .get(i);
                      var role = ProjectRole
                          .fromString(
                              roles.get(
                                  i));
                      return projectMemberRepository
                          .countByProjectIdAndNotDeleted(
                              project.getId())
                          .map(count -> ProjectSummaryResponse
                              .of(project,
                                  role,
                                  count));
                    })
                    .collectList()
                    .map(content -> PageResponse.of(
                        content, page, size,
                        totalElements));
              }));
    });
  }

  public Mono<ProjectResponse> getProject(String workspaceId,
      String projectId,
      String userId) {
    return validateMemberAccess(projectId, userId)
        .then(findProjectById(projectId))
        .flatMap(project -> {
          project.belongsToWorkspace(workspaceId);
          return buildProjectResponse(project, userId);
        });
  }

  public Mono<ProjectResponse> updateProject(String workspaceId,
      String projectId, UpdateProjectRequest request, String userId) {
    return validateAdminAccess(projectId, userId)
        .then(findProjectById(projectId))
        .flatMap(project -> {
          project.belongsToWorkspace(workspaceId);
          ProjectSettings settings = request.getSettingsOrDefault();
          validateSettings(settings);

          project.update(request.name(), request.description(),
              settings);
          return projectRepository.save(project);
        })
        .flatMap(savedProject -> buildProjectResponse(savedProject, userId))
        .as(transactionalOperator::transactional);
  }

  public Mono<Void> deleteProject(String workspaceId, String projectId,
      String userId) {
    return validateAdminAccess(projectId, userId)
        .then(findProjectById(projectId))
        .flatMap(project -> {
          project.belongsToWorkspace(workspaceId);
          if (project.isDeleted()) {
            return Mono.error(new BusinessException(
                ErrorCode.PROJECT_ALREADY_DELETED));
          }
          project.delete();
          return projectRepository.save(project)
              .then(projectMemberRepository
                  .softDeleteByProjectId(projectId));
        })
        .as(transactionalOperator::transactional);
  }

  public Mono<PageResponse<ProjectMemberResponse>> getMembers(
      String projectId, String userId, int page,
      int size) {
    return validateMemberAccess(projectId, userId)
        .then(projectMemberRepository
            .countByProjectIdAndNotDeleted(projectId))
        .flatMap(totalElements -> {
          int offset = page * size;
          return projectMemberRepository
              .findByProjectIdAndNotDeleted(projectId, size,
                  offset)
              .flatMap(member -> userRepository
                  .findById(member.getUserId())
                  .map(user -> ProjectMemberResponse
                      .of(member, user)))
              .collectList()
              .map(members -> PageResponse.of(members, page, size,
                  totalElements));
        });
  }

  public Mono<ProjectMemberResponse> updateMemberRole(String projectId, String targetUserId,
      UpdateProjectMemberRoleRequest request, String requesterId) {
    return validateAdminAccess(projectId, requesterId)
        .then(Mono.zip(
            findProjectMemberByUserIdAndProjectId(requesterId,
                projectId),
            findProjectMemberByUserIdAndProjectId(targetUserId,
                projectId)))
        .flatMap(tuple -> {
          ProjectMember requester = tuple.getT1();
          ProjectMember target = tuple.getT2();

          validateRoleChangePermission(requester, target,
              request.role());

          // 마지막 ADMIN 강등 방지
          Mono<Void> adminGuard = Mono.empty();
          if (target.isAdmin() && request.role() != ProjectRole.ADMIN) {
            adminGuard = validateAdminProtection(projectId);
          }

          return adminGuard.then(Mono.defer(() -> {
            target.updateRole(request.role());
            return projectMemberRepository.save(target);
          }));
        })
        .flatMap(updatedMember -> userRepository
            .findById(updatedMember.getUserId())
            .map(user -> ProjectMemberResponse.of(updatedMember,
                user)))
        .as(transactionalOperator::transactional);
  }

  /** 요청자는 부여하려는 역할보다 높거나 같은 권한을 가져야 함
   * 요청자는 대상의 현재 역할보다 높거나 같은 권한을 가져야 함 */
  private void validateRoleChangePermission(ProjectMember requester,
      ProjectMember target, ProjectRole newRole) {
    ProjectRole requesterRole = requester.getRoleAsEnum();
    ProjectRole targetCurrentRole = target.getRoleAsEnum();

    // 요청자가 본인의 역할을 변경하려는지
    if (target.getUserId().equals(requester.getUserId())) {
      throw new BusinessException(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
    }

    // 요청자가 부여하려는 역할 이상의 권한을 가지고 있는지
    if (!requesterRole.isHigherOrEqualThan(newRole)) {
      throw new BusinessException(ErrorCode.CANNOT_ASSIGN_HIGHER_ROLE);
    }

    // 요청자가 대상의 현재 역할 이상의 권한을 가지고 있는지
    if (!requesterRole.isHigherOrEqualThan(targetCurrentRole)) {
      throw new BusinessException(
          ErrorCode.CANNOT_MODIFY_HIGHER_ROLE_MEMBER);
    }
  }

  /** 프로젝트 멤버 제거 (관리자 권한) */
  public Mono<Void> removeMember(String projectId,
      String targetUserId, String requesterId) {
    return validateAdminAccess(projectId, requesterId)
        .then(findProjectMemberByUserIdAndProjectId(targetUserId,
            projectId))
        .flatMap(targetMember -> protectAdmin(projectId, targetMember))
        .as(transactionalOperator::transactional);
  }

  /** 프로젝트 자발적 탈퇴 */
  public Mono<Void> leaveProject(String projectId, String userId) {
    return findProjectMemberByUserIdAndProjectId(userId, projectId)
        .flatMap(member -> protectAdmin(projectId, member)
            .then(projectMemberRepository
                .countByProjectIdAndNotDeleted(projectId)
                .flatMap(memberCount -> {
                  if (memberCount <= 1) {
                    return softDeleteMember(member)
                        .then(projectRepository
                            .findByIdAndNotDeleted(
                                projectId)
                            .flatMap(project -> {
                              project.delete();
                              return projectRepository
                                  .save(project)
                                  .then();
                            }));
                  }
                  return softDeleteMember(member);
                })))
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> validateAdminProtection(String projectId) {
    return projectMemberRepository
        .countByProjectIdAndRoleAndNotDeleted(projectId,
            ProjectRole.ADMIN.getValue())
        .flatMap(adminCount -> {
          if (adminCount <= 1) {
            return Mono.error(new BusinessException(
                ErrorCode.LAST_ADMIN_CANNOT_BE_REMOVED));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> validateMemberAccess(String projectId, String userId) {
    return validateProjectMember(projectId, userId);
  }

  private Mono<Void> validateAdminAccess(String projectId, String userId) {
    return validateProjectAdmin(projectId, userId);
  }

  private Mono<Project> findProjectById(String projectId) {
    return projectRepository.findByIdAndNotDeleted(projectId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.PROJECT_NOT_FOUND)));
  }

  private Mono<ProjectResponse> buildProjectResponse(Project project,
      String userId) {
    return Mono.zip(
        projectMemberRepository.countByProjectIdAndNotDeleted(
            project.getId()),
        projectMemberRepository
            .findByProjectIdAndUserIdAndNotDeleted(project.getId(), userId)
            .map(ProjectMember::getRole)
            .defaultIfEmpty(ProjectRole.VIEWER.getValue()))
        .map(tuple -> ProjectResponse.of(
            project,
            tuple.getT1(), // memberCount
            tuple.getT2()  // currentUserRole
        ));
  }

  private Mono<ProjectMember> findProjectMemberByUserIdAndProjectId(
      String userId, String projectId) {
    return projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(projectId, userId)
        .filter(member -> member.getProjectId().equals(projectId))
        .switchIfEmpty(Mono.error(new BusinessException(
            ErrorCode.PROJECT_MEMBER_NOT_FOUND)));
  }

  private Mono<Void> protectAdmin(String projectId,
      ProjectMember targetMember) {
    if (targetMember.isAdmin()) {
      return validateAdminProtection(projectId)
          .then(softDeleteMember(targetMember));
    }

    return softDeleteMember(targetMember);
  }

  private Mono<Void> softDeleteMember(ProjectMember member) {
    member.delete();
    return projectMemberRepository.save(member).then();
  }

  private Mono<Void> validateWorkspaceAdmin(String workspaceId,
      String userId) {
    return workspaceMemberRepository
        .findByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .switchIfEmpty(Mono.error(new BusinessException(
            ErrorCode.WORKSPACE_ACCESS_DENIED)))
        .filter(WorkspaceMember::isAdmin)
        .switchIfEmpty(Mono.error(new BusinessException(
            ErrorCode.WORKSPACE_ADMIN_REQUIRED)))
        .then();
  }

  private Mono<Void> validateProjectMember(String projectId,
      String userId) {
    return projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(projectId, userId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED)))
        .then();
  }

  private Mono<Void> validateProjectAdmin(String projectId, String userId) {
    return projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(projectId, userId)
        .switchIfEmpty(Mono.error(
            new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED)))
        .filter(ProjectMember::isAdmin)
        .switchIfEmpty(Mono.error(new BusinessException(
            ErrorCode.PROJECT_ADMIN_REQUIRED)))
        .then();
  }

  private void validateSettings(ProjectSettings settings) {
    settings.validate();
    String json = settings.toJson();
    if (json.length() > 65536) {
      throw new BusinessException(ErrorCode.PROJECT_SETTINGS_TOO_LARGE);
    }
  }

  private Mono<Void> propagateWorkspaceMembersToProject(
      String projectId, String workspaceId, String creatorUserId) {
    return workspaceMemberRepository.findAllByWorkspaceIdAndNotDeleted(workspaceId)
        .filter(wsMember -> !wsMember.getUserId().equals(creatorUserId))
        .flatMap(wsMember -> projectMemberRepository
            .existsByProjectIdAndUserIdAndNotDeleted(projectId, wsMember.getUserId())
            .flatMap(exists -> {
              if (exists) {
                return Mono.empty();
              }
              ProjectRole projectRole = wsMember.getRoleAsEnum().toProjectRole();
              ProjectMember newMember = ProjectMember.create(
                  projectId, wsMember.getUserId(), projectRole);
              return projectMemberRepository.save(newMember).then();
            })
            .onErrorResume(DataIntegrityViolationException.class, e -> {
              log.warn("Duplicate key on auto-add user {} to project {}: {}",
                  wsMember.getUserId(), projectId, e.getMessage());
              return Mono.empty();
            }))
        .then();
  }

  public Mono<Void> propagateToExistingProjects(
      String workspaceId, String userId, WorkspaceRole workspaceRole) {
    ProjectRole projectRole = workspaceRole.toProjectRole();

    return projectRepository.findByWorkspaceIdAndNotDeleted(workspaceId)
        .flatMap(project -> projectMemberRepository
            .existsByProjectIdAndUserIdAndNotDeleted(project.getId(), userId)
            .flatMap(exists -> {
              if (exists) {
                return Mono.empty();
              }
              ProjectMember newMember = ProjectMember.create(
                  project.getId(), userId, projectRole);
              return projectMemberRepository.save(newMember).then();
            })
            .onErrorResume(DataIntegrityViolationException.class, e -> {
              log.warn("Duplicate key on auto-add user {} to project {}: {}",
                  userId, project.getId(), e.getMessage());
              return Mono.empty();
            }))
        .then();
  }

}
