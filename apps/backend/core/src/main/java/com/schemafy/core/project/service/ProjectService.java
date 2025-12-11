package com.schemafy.core.project.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.schemafy.core.project.repository.ShareLinkRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.project.repository.entity.ShareLink;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.project.repository.vo.ProjectSettings;
import com.schemafy.core.project.repository.vo.ShareLinkRole;
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
    private final ShareLinkRepository shareLinkRepository;
    private final ShareLinkTokenService tokenService;

    private static final int MAX_PROJECT_MEMBERS = 30;

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

    /**
     * ShareLink 토큰을 통한 프로젝트 참여
     */
    @Transactional
    public Mono<ProjectMemberResponse> joinProjectByShareLink(String token,
            String userId) {
        byte[] tokenHash = tokenService.hashToken(token);

        return shareLinkRepository.findValidByTokenHash(tokenHash)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.SHARE_LINK_INVALID)))
                .flatMap(shareLink -> projectRepository
                        .findByIdAndNotDeleted(shareLink.getProjectId())
                        .switchIfEmpty(Mono.error(
                                new BusinessException(
                                        ErrorCode.PROJECT_NOT_FOUND)))
                        .flatMap(project -> workspaceMemberRepository
                                .existsByWorkspaceIdAndUserIdAndNotDeleted(
                                        project.getWorkspaceId(), userId)
                                .flatMap(isWorkspaceMember -> {
                                    if (!isWorkspaceMember) {
                                        return Mono.error(new BusinessException(
                                                ErrorCode.WORKSPACE_MEMBERSHIP_REQUIRED));
                                    }
                                    return createOrUpdateProjectMember(project,
                                            shareLink, userId);
                                })));
    }

    /**
     * 프로젝트 멤버 역할 변경
     */
    @Transactional
    public Mono<ProjectMemberResponse> updateMemberRole(String workspaceId,
            String projectId, String memberId,
            UpdateProjectMemberRoleRequest request, String userId) {
        return validateWorkspaceMemberAccess(workspaceId, userId)
                .then(validateProjectAdminAccess(projectId, userId))
                .then(projectMemberRepository.findByIdAndNotDeleted(memberId))
                .switchIfEmpty(Mono
                        .error(new BusinessException(
                                ErrorCode.MEMBER_NOT_FOUND)))
                .flatMap(targetMember -> {
                    if (!targetMember.getProjectId().equals(projectId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.MEMBER_NOT_FOUND));
                    }

                    if (targetMember.getUserId().equals(userId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.CANNOT_CHANGE_OWN_ROLE));
                    }

                    if ((targetMember.isOwner()
                            && request.role() != ProjectRole.OWNER) ||
                            (targetMember.isAdmin()
                                    && !request.role().isAdmin())) {
                        return validateOwnerOrAdminProtection(projectId)
                                .then(Mono.defer(() -> {
                                    targetMember.updateRole(request.role());
                                    return projectMemberRepository
                                            .save(targetMember);
                                }));
                    }

                    targetMember.updateRole(request.role());
                    return projectMemberRepository.save(targetMember);
                })
                .flatMap(updatedMember -> userRepository
                        .findById(updatedMember.getUserId())
                        .map(user -> ProjectMemberResponse.of(updatedMember,
                                user)));
    }

    /**
     * 프로젝트 멤버 제거 (관리자 권한)
     */
    @Transactional
    public Mono<Void> removeMember(String workspaceId, String projectId,
            String memberId, String userId) {
        return validateWorkspaceMemberAccess(workspaceId, userId)
                .then(validateProjectAdminAccess(projectId, userId))
                .then(projectMemberRepository.findByIdAndNotDeleted(memberId))
                .switchIfEmpty(Mono
                        .error(new BusinessException(
                                ErrorCode.MEMBER_NOT_FOUND)))
                .flatMap(targetMember -> {
                    if (!targetMember.getProjectId().equals(projectId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.MEMBER_NOT_FOUND));
                    }

                    if (targetMember.isOwner() || targetMember.isAdmin()) {
                        return validateOwnerOrAdminProtection(projectId)
                                .then(softDeleteMember(targetMember));
                    }

                    return softDeleteMember(targetMember);
                });
    }

    /**
     * 프로젝트 자발적 탈퇴
     */
    @Transactional
    public Mono<Void> leaveProject(String workspaceId, String projectId,
            String userId) {
        return validateWorkspaceMemberAccess(workspaceId, userId)
                .then(projectMemberRepository
                        .findByProjectIdAndUserIdAndNotDeleted(projectId,
                                userId))
                .switchIfEmpty(Mono
                        .error(new BusinessException(
                                ErrorCode.MEMBER_NOT_FOUND)))
                .flatMap(member -> {
                    if (member.isOwner() || member.isAdmin()) {
                        return validateOwnerOrAdminProtection(projectId)
                                .then(Mono
                                        .defer(() -> softDeleteMember(member)));
                    }

                    return projectMemberRepository
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
                            });
                });
    }

    private Mono<ProjectMemberResponse> createOrUpdateProjectMember(
            Project project, ShareLink shareLink, String userId) {
        return projectMemberRepository
                .findByProjectIdAndUserIdAndNotDeleted(project.getId(), userId)
                .flatMap(existingMember -> {
                    ShareLinkRole shareLinkRole = shareLink.getRoleAsEnum();
                    ProjectRole newRole = shareLinkRole.toProjectRole();
                    ProjectRole currentRole = existingMember.getRoleAsEnum();

                    // 새로운 역할이 현재 역할보다 높은 권한을 가지면 업그레이드
                    if (newRole.getLevel() > currentRole.getLevel()) {
                        existingMember.updateRole(newRole);
                        return projectMemberRepository.save(existingMember);
                    }

                    return Mono.just(existingMember);
                })
                .switchIfEmpty(Mono.defer(() -> projectMemberRepository
                        .countByProjectIdAndNotDeleted(project.getId())
                        .flatMap(memberCount -> {
                            if (memberCount >= MAX_PROJECT_MEMBERS) {
                                return Mono.error(new BusinessException(
                                        ErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED));
                            }

                            ShareLinkRole shareLinkRole = shareLink
                                    .getRoleAsEnum();
                            ProjectRole projectRole = shareLinkRole
                                    .toProjectRole();
                            ProjectMember newMember = ProjectMember
                                    .create(project.getId(), userId,
                                            projectRole);

                            return projectMemberRepository.save(newMember);
                        })))
                .flatMap(member -> userRepository.findById(member.getUserId())
                        .map(user -> ProjectMemberResponse.of(member, user)));
    }

    private Mono<Void> validateOwnerOrAdminProtection(String projectId) {
        return Mono.zip(
                projectMemberRepository.countByProjectIdAndRoleAndNotDeleted(
                        projectId,
                        ProjectRole.OWNER.getValue()),
                projectMemberRepository.countByProjectIdAndRoleAndNotDeleted(
                        projectId,
                        ProjectRole.ADMIN.getValue()))
                .flatMap(tuple -> {
                    long ownerCount = tuple.getT1();
                    long adminCount = tuple.getT2();
                    long totalAdminCount = ownerCount + adminCount;

                    if (totalAdminCount <= 1) {
                        return Mono.error(new BusinessException(
                                ErrorCode.LAST_OWNER_CANNOT_BE_REMOVED));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> softDeleteMember(ProjectMember member) {
        member.delete();
        return projectMemberRepository.save(member).then();
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
