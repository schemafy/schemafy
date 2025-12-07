package com.schemafy.core.project.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.CreateProjectRequest;
import com.schemafy.core.project.controller.dto.request.UpdateProjectRequest;
import com.schemafy.core.project.controller.dto.response.ProjectMemberResponse;
import com.schemafy.core.project.controller.dto.response.ProjectResponse;
import com.schemafy.core.project.controller.dto.response.ProjectSummaryResponse;
import com.schemafy.core.project.repository.ProjectMemberRepository;
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.project.repository.vo.ProjectSettings;
import com.schemafy.core.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public Mono<ProjectResponse> createProject(String workspaceId,
            CreateProjectRequest request, String userId) {
        return validateWorkspaceMemberAccess(workspaceId, userId).then(
                Mono.defer(() -> {
                    ProjectSettings settings = request.getSettingsOrDefault();
                    validateSettings(settings);

                    Project project = Project.create(workspaceId, userId,
                            request.name(), request.description(), settings);

                    ProjectMember ownerMember = ProjectMember
                            .create(project.getId(), userId, ProjectRole.OWNER);

                    return projectRepository.save(project)
                            .flatMap(savedProject -> projectMemberRepository
                                    .save(ownerMember)
                                    .thenReturn(savedProject))
                            .map(ProjectResponse::from);
                }));
    }

    @Transactional(readOnly = true)
    public Mono<PageResponse<ProjectSummaryResponse>> getProjects(
            String workspaceId, String userId, int page, int size) {
        return validateWorkspaceMemberAccess(workspaceId, userId)
                .then(projectMemberRepository.findByUserIdAndNotDeleted(userId)
                        .flatMap(member -> projectRepository
                                .findByIdAndNotDeleted(member.getProjectId())
                                .filter(project -> project
                                        .belongsToWorkspace(workspaceId))
                                .flatMap(project -> projectMemberRepository
                                        .countByProjectIdAndNotDeleted(
                                                project.getId())
                                        .map(count -> ProjectSummaryResponse.of(
                                                project,
                                                ProjectRole.fromString(
                                                        member.getRole()),
                                                count))))
                        .collectList().flatMap(allProjects -> {
                            int offset = page * size;
                            int totalElements = allProjects.size();
                            int start = Math.min(offset, totalElements);
                            int end = Math.min(offset + size, totalElements);
                            List<ProjectSummaryResponse> pagedContent = allProjects
                                    .subList(start, end);
                            return Mono.just(PageResponse.of(pagedContent, page,
                                    size, totalElements));
                        }));
    }

    @Transactional(readOnly = true)
    public Mono<ProjectResponse> getProject(String workspaceId,
            String projectId,
            String userId) {
        return validateWorkspaceMemberAccess(workspaceId, userId)
                .then(validateProjectMemberAccess(projectId, userId))
                .then(projectRepository.findByIdAndNotDeleted(projectId))
                .switchIfEmpty(
                        Mono.error(new BusinessException(
                                ErrorCode.PROJECT_NOT_FOUND)))
                .flatMap(project -> {
                    if (!project.belongsToWorkspace(workspaceId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.PROJECT_WORKSPACE_MISMATCH));
                    }
                    return Mono.just(ProjectResponse.from(project));
                });
    }

    @Transactional
    public Mono<ProjectResponse> updateProject(String workspaceId,
            String projectId, UpdateProjectRequest request, String userId) {
        return validateWorkspaceMemberAccess(workspaceId, userId)
                .then(validateProjectAdminAccess(projectId, userId))
                .then(projectRepository.findByIdAndNotDeleted(projectId))
                .switchIfEmpty(
                        Mono.error(new BusinessException(
                                ErrorCode.PROJECT_NOT_FOUND)))
                .flatMap(project -> {
                    if (!project.belongsToWorkspace(workspaceId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.PROJECT_WORKSPACE_MISMATCH));
                    }

                    ProjectSettings settings = request.getSettingsOrDefault();
                    validateSettings(settings);

                    project.update(request.name(), request.description(),
                            settings);
                    return projectRepository.save(project);
                }).map(ProjectResponse::from);
    }

    @Transactional
    public Mono<Void> deleteProject(String workspaceId, String projectId,
            String userId) {
        return validateWorkspaceMemberAccess(workspaceId, userId)
                .then(validateProjectOwnerAccess(projectId, userId))
                .then(projectRepository.findByIdAndNotDeleted(projectId))
                .switchIfEmpty(
                        Mono.error(new BusinessException(
                                ErrorCode.PROJECT_NOT_FOUND)))
                .flatMap(project -> {
                    if (!project.belongsToWorkspace(workspaceId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.PROJECT_WORKSPACE_MISMATCH));
                    }
                    if (project.isDeleted()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.PROJECT_ALREADY_DELETED));
                    }
                    project.delete();
                    return projectRepository.save(project)
                            .then(projectMemberRepository
                                    .softDeleteByProjectId(projectId));
                });
    }

    @Transactional(readOnly = true)
    public Mono<PageResponse<ProjectMemberResponse>> getMembers(
            String workspaceId, String projectId, String userId, int page,
            int size) {
        return validateWorkspaceMemberAccess(workspaceId, userId)
                .then(validateProjectMemberAccess(projectId, userId))
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

    private Mono<Void> validateWorkspaceMemberAccess(String workspaceId,
            String userId) {
        return workspaceMemberRepository
                .existsByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new BusinessException(
                                ErrorCode.WORKSPACE_ACCESS_DENIED));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> validateProjectMemberAccess(String projectId,
            String userId) {
        return projectMemberRepository
                .existsByProjectIdAndUserIdAndNotDeleted(projectId, userId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new BusinessException(
                                ErrorCode.PROJECT_ACCESS_DENIED));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> validateProjectOwnerAccess(String projectId,
            String userId) {
        return projectRepository.findByIdAndNotDeleted(projectId)
                .switchIfEmpty(
                        Mono.error(new BusinessException(
                                ErrorCode.PROJECT_NOT_FOUND)))
                .flatMap(project -> {
                    if (!project.isOwner(userId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.PROJECT_OWNER_ONLY));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> validateProjectAdminAccess(String projectId,
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

    private void validateSettings(ProjectSettings settings) {
        settings.validate();
        String json = settings.toJson();
        if (json.length() > 65536) {
            throw new BusinessException(ErrorCode.PROJECT_SETTINGS_TOO_LARGE);
        }
    }

}
