package com.schemafy.core.project.adapter.out.persistence;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.r2dbc.core.DatabaseClient;

import com.schemafy.core.common.PersistenceAdapter;
import com.schemafy.core.erd.schema.application.port.out.ActiveProjectExistsPort;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
public class ProjectPersistenceAdapter
    implements ProjectPort, ProjectMemberPort, ActiveProjectExistsPort {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final DatabaseClient databaseClient;

  private static final int PROJECT_MEMBER_UPSERT_BATCH_SIZE = 100;

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
  public Mono<Project> findByIdAndNotDeletedForUpdate(String projectId) {
    return projectRepository.findByIdAndNotDeletedForUpdate(projectId);
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
  public Mono<Void> upsertAllForProject(String projectId,
      Collection<ProjectMember> projectMembers) {
    if (projectMembers.isEmpty()) {
      return Mono.empty();
    }

    List<ProjectMemberRow> rows = projectMembers.stream()
        .map(ProjectMemberRow::from)
        .map(row -> row.requireProjectId(projectId))
        .toList();

    return upsertProjectMembers(rows,
        (role, batch) -> upsertProjectMembersForProject(projectId, role,
            batch));
  }

  @Override
  public Mono<Void> upsertAllForUser(String userId,
      Collection<ProjectMember> projectMembers) {
    if (projectMembers.isEmpty()) {
      return Mono.empty();
    }

    List<ProjectMemberRow> rows = projectMembers.stream()
        .map(ProjectMemberRow::from)
        .map(row -> row.requireUserId(userId))
        .toList();

    return upsertProjectMembers(rows,
        (role, batch) -> upsertProjectMembersForUser(userId, role,
            batch));
  }

  private Mono<Void> upsertProjectMembers(
      List<ProjectMemberRow> rows,
      ProjectMemberBatchUpserter upserter) {
    Map<String, List<ProjectMemberRow>> membersByRole = rows.stream()
        .collect(Collectors.groupingBy(
            ProjectMemberRow::role,
            LinkedHashMap::new,
            Collectors.toList()));

    return Flux.fromIterable(membersByRole.entrySet())
        .concatMap(entry -> Flux.fromIterable(entry.getValue())
            .buffer(PROJECT_MEMBER_UPSERT_BATCH_SIZE)
            .concatMap(batch -> upserter.upsert(entry.getKey(), batch)))
        .then();
  }

  private Mono<Void> upsertProjectMembersForProject(
      String projectId,
      String role,
      List<ProjectMemberRow> projectMembers) {
    List<String> userIds = userIds(projectMembers);
    Mono<Long> setAdminForActiveMembers = ProjectRole.ADMIN.name().equals(role)
        ? projectMemberRepository.setAdminForActiveMembersByProjectIdAndUserIds(
            projectId,
            userIds)
        : Mono.empty();

    return insertMissingProjectMembers(projectMembers)
        .then(projectMemberRepository.restoreDeletedByProjectIdAndUserIds(
            role,
            projectId,
            userIds))
        .then(setAdminForActiveMembers)
        .then();
  }

  private Mono<Void> upsertProjectMembersForUser(
      String userId,
      String role,
      List<ProjectMemberRow> projectMembers) {
    List<String> projectIds = projectIds(projectMembers);
    Mono<Long> setAdminForActiveMembers = ProjectRole.ADMIN.name().equals(role)
        ? projectMemberRepository.setAdminForActiveMembersByProjectIdsAndUserId(
            projectIds,
            userId)
        : Mono.empty();

    return insertMissingProjectMembers(projectMembers)
        .then(projectMemberRepository.restoreDeletedByProjectIdsAndUserId(
            role,
            projectIds,
            userId))
        .then(setAdminForActiveMembers)
        .then();
  }

  private Mono<Long> insertMissingProjectMembers(
      List<ProjectMemberRow> projectMembers) {
    StringBuilder sql = new StringBuilder("""
        INSERT INTO project_members (
          id, project_id, user_id, role, joined_at, created_at, updated_at,
          deleted_at
        )
        VALUES
        """);

    for (int i = 0; i < projectMembers.size(); i++) {
      if (i > 0) {
        sql.append(",\n");
      }
      sql.append("(:id").append(i)
          .append(", :projectId").append(i)
          .append(", :userId").append(i)
          .append(", :role").append(i)
          .append(", CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ")
          .append("NULL)");
    }

    sql.append("""

        ON DUPLICATE KEY UPDATE
          id = id
        """);

    DatabaseClient.GenericExecuteSpec spec = databaseClient
        .sql(sql.toString());

    for (int i = 0; i < projectMembers.size(); i++) {
      ProjectMemberRow member = projectMembers.get(i);

      spec = spec.bind("id" + i, member.id())
          .bind("projectId" + i, member.projectId())
          .bind("userId" + i, member.userId())
          .bind("role" + i, member.role());
    }
    return spec.fetch().rowsUpdated();
  }

  private List<String> projectIds(List<ProjectMemberRow> projectMembers) {
    return projectMembers.stream()
        .map(ProjectMemberRow::projectId)
        .toList();
  }

  private List<String> userIds(List<ProjectMemberRow> projectMembers) {
    return projectMembers.stream()
        .map(ProjectMemberRow::userId)
        .toList();
  }

  private record ProjectMemberRow(
      String id,
      String projectId,
      String userId,
      String role) {

    ProjectMemberRow requireProjectId(String expectedProjectId) {
      if (!projectId.equals(expectedProjectId)) {
        throw new IllegalArgumentException(
            "projectMember.projectId must match projectId");
      }
      return this;
    }

    ProjectMemberRow requireUserId(String expectedUserId) {
      if (!userId.equals(expectedUserId)) {
        throw new IllegalArgumentException(
            "projectMember.userId must match userId");
      }
      return this;
    }

    static ProjectMemberRow from(ProjectMember member) {
      if (member == null) {
        throw new IllegalArgumentException("projectMember must not be null");
      }

      String id = member.getId();
      if (id == null) {
        throw new IllegalArgumentException("projectMember.id must not be null");
      }
      String projectId = member.getProjectId();
      if (projectId == null) {
        throw new IllegalArgumentException(
            "projectMember.projectId must not be null");
      }
      String userId = member.getUserId();
      if (userId == null) {
        throw new IllegalArgumentException(
            "projectMember.userId must not be null");
      }
      String role = member.getRole();
      if (role == null) {
        throw new IllegalArgumentException("projectMember.role must not be null");
      }

      return new ProjectMemberRow(id, projectId, userId, role);
    }

  }

  @FunctionalInterface
  private interface ProjectMemberBatchUpserter {

    Mono<Void> upsert(String role, List<ProjectMemberRow> projectMembers);

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
