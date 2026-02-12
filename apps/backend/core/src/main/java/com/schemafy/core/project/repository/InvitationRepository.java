package com.schemafy.core.project.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.project.repository.entity.Invitation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface InvitationRepository
    extends ReactiveCrudRepository<Invitation, String> {

  @Query("""
      SELECT * FROM invitations
      WHERE id = :invitationId
        AND deleted_at IS NULL
      """)
  Mono<Invitation> findByIdAndNotDeleted(String invitationId);

  @Query("""
      SELECT * FROM invitations
      WHERE target_type = 'WORKSPACE'
        AND target_id = :workspaceId
        AND deleted_at IS NULL
      ORDER BY created_at DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<Invitation> findWorkspaceInvitations(String workspaceId, int limit, int offset);

  @Query("""
      SELECT COUNT(*) FROM invitations
      WHERE target_type = 'WORKSPACE'
        AND target_id = :workspaceId
        AND deleted_at IS NULL
      """)
  Mono<Long> countWorkspaceInvitations(String workspaceId);

  @Query("""
      SELECT COUNT(*) FROM invitations
      WHERE target_type = 'WORKSPACE'
        AND target_id = :workspaceId
        AND invited_email = :email
        AND status = 'pending'
        AND deleted_at IS NULL
      """)
  Mono<Long> countPendingWorkspaceInvitation(String workspaceId, String email);

  @Query("""
      SELECT * FROM invitations
      WHERE target_type = 'PROJECT'
        AND target_id = :projectId
        AND deleted_at IS NULL
      ORDER BY created_at DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<Invitation> findProjectInvitations(String projectId, int limit, int offset);

  @Query("""
      SELECT COUNT(*) FROM invitations
      WHERE target_type = 'PROJECT'
        AND target_id = :projectId
        AND deleted_at IS NULL
      """)
  Mono<Long> countProjectInvitations(String projectId);

  @Query("""
      SELECT COUNT(*) FROM invitations
      WHERE target_type = 'PROJECT'
        AND target_id = :projectId
        AND invited_email = :email
        AND status = 'pending'
        AND deleted_at IS NULL
      """)
  Mono<Long> countPendingProjectInvitation(String projectId, String email);


  @Query("""
      SELECT * FROM invitations
      WHERE target_type = :targetType
        AND invited_email = :email
        AND status = 'pending'
        AND deleted_at IS NULL
      ORDER BY created_at DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<Invitation> findPendingByEmailAndType(String email, String targetType, int limit, int offset);

  @Query("""
      SELECT COUNT(*) FROM invitations
      WHERE target_type = :targetType
        AND invited_email = :email
        AND status = 'pending'
        AND deleted_at IS NULL
      """)
  Mono<Long> countPendingByEmailAndType(String email, String targetType);

}
