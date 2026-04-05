package com.schemafy.core.project.adapter.out.persistence;

import com.schemafy.core.common.PersistenceAdapter;
import com.schemafy.core.erd.schema.application.port.out.ActiveProjectExistsPort;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
public class ProjectPersistenceAdapter
    implements ProjectPort, ProjectMemberPort, ActiveProjectExistsPort {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;

  @Override
  public Mono<Project> save(Project project) {
    return projectRepository.save(project);
  }

  @Override
  public Mono<Project> findById(String projectId) {
    return projectRepository.findById(projectId);
  }

  @Override
  public Mono<Project> findByIdAndNotDeleted(String projectId) {
    return projectRepository.findByIdAndNotDeleted(projectId);
  }

  @Override
  public Mono<Boolean> existsActiveProjectById(String projectId) {
    return projectRepository.findByIdAndNotDeleted(projectId)
        .hasElement();
  }

  @Override
  public Flux<Project> findByWorkspaceIdAndNotDeleted(String workspaceId) {
    return projectRepository.findByWorkspaceIdAndNotDeleted(workspaceId);
  }

  @Override
  public Flux<Project> findByWorkspaceId(String workspaceId) {
    return projectRepository.findByWorkspaceId(workspaceId);
  }

  @Override
  public Mono<Long> countByWorkspaceIdAndNotDeleted(String workspaceId) {
    return projectRepository.countByWorkspaceIdAndNotDeleted(workspaceId);
  }

  @Override
  public Flux<Project> findByWorkspaceIdAndUserIdWithPaging(String workspaceId,
      String userId, int limit, int offset) {
    return projectRepository.findByWorkspaceIdAndUserIdWithPaging(workspaceId,
        userId, limit, offset);
  }

  @Override
  public Flux<Project> findSharedByUserIdWithPaging(String userId,
      int limit, int offset) {
    return projectRepository.findSharedByUserIdWithPaging(userId, limit,
        offset);
  }

  @Override
  public Mono<ProjectMember> save(ProjectMember projectMember) {
    return projectMemberRepository.save(projectMember);
  }

  @Override
  public Mono<ProjectMember> findByProjectIdAndUserIdAndNotDeleted(
      String projectId,
      String userId) {
    return projectMemberRepository.findByProjectIdAndUserIdAndNotDeleted(
        projectId, userId);
  }

  @Override
  public Flux<ProjectMember> findByProjectIdAndNotDeleted(String projectId,
      int limit, int offset) {
    return projectMemberRepository.findByProjectIdAndNotDeleted(projectId,
        limit, offset);
  }

  @Override
  public Mono<Long> countByProjectIdAndNotDeleted(String projectId) {
    return projectMemberRepository.countByProjectIdAndNotDeleted(projectId);
  }

  @Override
  public Mono<Boolean> existsByProjectIdAndUserIdAndNotDeleted(
      String projectId,
      String userId) {
    return projectMemberRepository.existsByProjectIdAndUserIdAndNotDeleted(
        projectId, userId);
  }

  @Override
  public Mono<Void> softDeleteByProjectId(String projectId) {
    return projectMemberRepository.softDeleteByProjectId(projectId);
  }

  @Override
  public Mono<Long> countByProjectIdAndRoleAndNotDeleted(String projectId,
      String role) {
    return projectMemberRepository.countByProjectIdAndRoleAndNotDeleted(
        projectId, role);
  }

  @Override
  public Mono<ProjectMember> findLatestByProjectIdAndUserId(String projectId,
      String userId) {
    return projectMemberRepository.findLatestByProjectIdAndUserId(projectId,
        userId);
  }

  @Override
  public Flux<String> findRolesByWorkspaceIdAndUserIdWithPaging(
      String workspaceId,
      String userId,
      int limit,
      int offset) {
    return projectMemberRepository.findRolesByWorkspaceIdAndUserIdWithPaging(
        workspaceId, userId, limit, offset);
  }

  @Override
  public Flux<String> findSharedRolesByUserIdWithPaging(String userId,
      int limit, int offset) {
    return projectMemberRepository.findSharedRolesByUserIdWithPaging(userId,
        limit, offset);
  }

  @Override
  public Mono<Long> countByWorkspaceIdAndUserId(String workspaceId,
      String userId) {
    return projectMemberRepository.countByWorkspaceIdAndUserId(workspaceId,
        userId);
  }

  @Override
  public Mono<Long> softDeleteByWorkspaceIdAndUserId(String workspaceId,
      String userId) {
    return projectMemberRepository.softDeleteByWorkspaceIdAndUserId(workspaceId,
        userId);
  }

  @Override
  public Flux<ProjectMember> findByWorkspaceIdAndUserId(String workspaceId,
      String userId) {
    return projectMemberRepository.findByWorkspaceIdAndUserId(workspaceId,
        userId);
  }

  @Override
  public Mono<Long> countSharedByUserId(String userId) {
    return projectMemberRepository.countSharedByUserId(userId);
  }

}
