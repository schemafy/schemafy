package com.schemafy.core.project.adapter.out.persistence;

import com.schemafy.core.common.PersistenceAdapter;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
public class WorkspacePersistenceAdapter
    implements WorkspacePort, WorkspaceMemberPort {

  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;

  @Override
  public Mono<Workspace> save(Workspace workspace) {
    return workspaceRepository.save(workspace);
  }

  @Override
  public Mono<Workspace> findByIdAndNotDeleted(String workspaceId) {
    return workspaceRepository.findByIdAndNotDeleted(workspaceId);
  }

  @Override
  public Flux<Workspace> findByUserIdWithPaging(String userId, int limit,
      int offset) {
    return workspaceRepository.findByUserIdWithPaging(userId, limit, offset);
  }

  @Override
  public Mono<Long> countByUserId(String userId) {
    return workspaceRepository.countByUserId(userId);
  }

  @Override
  public Mono<WorkspaceMember> save(WorkspaceMember workspaceMember) {
    return workspaceMemberRepository.save(workspaceMember);
  }

  @Override
  public Mono<WorkspaceMember> findByWorkspaceIdAndUserIdAndNotDeleted(
      String workspaceId,
      String userId) {
    return workspaceMemberRepository
        .findByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId);
  }

  @Override
  public Flux<WorkspaceMember> findByWorkspaceIdAndNotDeleted(
      String workspaceId,
      int limit,
      int offset) {
    return workspaceMemberRepository.findByWorkspaceIdAndNotDeleted(
        workspaceId, limit, offset);
  }

  @Override
  public Flux<WorkspaceMember> findAllByWorkspaceIdAndNotDeleted(
      String workspaceId) {
    return workspaceMemberRepository.findAllByWorkspaceIdAndNotDeleted(
        workspaceId);
  }

  @Override
  public Mono<Long> countByWorkspaceIdAndNotDeleted(String workspaceId) {
    return workspaceMemberRepository.countByWorkspaceIdAndNotDeleted(
        workspaceId);
  }

  @Override
  public Mono<Void> softDeleteByWorkspaceId(String workspaceId) {
    return workspaceMemberRepository.softDeleteByWorkspaceId(workspaceId);
  }

  @Override
  public Mono<Boolean> existsByWorkspaceIdAndUserIdAndNotDeleted(
      String workspaceId,
      String userId) {
    return workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndNotDeleted(
        workspaceId, userId);
  }

  @Override
  public Mono<Long> countByWorkspaceIdAndRoleAndNotDeleted(String workspaceId,
      String role) {
    return workspaceMemberRepository.countByWorkspaceIdAndRoleAndNotDeleted(
        workspaceId, role);
  }

  @Override
  public Mono<WorkspaceMember> findLatestByWorkspaceIdAndUserId(
      String workspaceId,
      String userId) {
    return workspaceMemberRepository.findLatestByWorkspaceIdAndUserId(
        workspaceId, userId);
  }

}
