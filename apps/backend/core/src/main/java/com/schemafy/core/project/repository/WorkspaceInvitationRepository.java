package com.schemafy.core.project.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.project.repository.entity.WorkspaceInvitation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkspaceInvitationRepository
    extends ReactiveCrudRepository<WorkspaceInvitation, String> {

  @Query("""
      SELECT * FROM workspace_invitations
      WHERE id = :invitationId
        AND deleted_at IS NULL
      """)
  Mono<WorkspaceInvitation> findByIdAndNotDeleted(String invitationId);

  @Query("""
      SELECT * FROM workspace_invitations
      WHERE workspace_id = :workspaceId
        AND deleted_at IS NULL
      ORDER BY created_at DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<WorkspaceInvitation> findByWorkspaceIdAndNotDeleted(
      String workspaceId, int limit, int offset);

  @Query("""
      SELECT COUNT(*) FROM workspace_invitations
      WHERE workspace_id = :workspaceId
        AND deleted_at IS NULL
      """)
  Mono<Long> countByWorkspaceIdAndNotDeleted(String workspaceId);

  @Query("""
      SELECT COUNT(*) FROM workspace_invitations
      WHERE workspace_id = :workspaceId
        AND invited_email = LOWER(:email)
        AND status = 'pending'
        AND deleted_at IS NULL
      """)
  Mono<Long> countPendingByWorkspaceAndEmail(String workspaceId, String email);

  @Query("""
      SELECT * FROM workspace_invitations
      WHERE invited_email = LOWER(:email)
        AND status = 'pending'
        AND deleted_at IS NULL
      ORDER BY created_at DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<WorkspaceInvitation> findPendingByEmail(String email, int limit, int offset);

  @Query("""
      SELECT COUNT(*) FROM workspace_invitations
      WHERE invited_email = LOWER(:email)
        AND status = 'pending'
        AND deleted_at IS NULL
      """)
  Mono<Long> countPendingByEmail(String email);

}
