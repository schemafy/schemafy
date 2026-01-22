package com.schemafy.core.project.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.project.repository.entity.ProjectInvitation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProjectInvitationRepository
    extends ReactiveCrudRepository<ProjectInvitation, String> {

  @Query("""
      SELECT * FROM project_invitations
      WHERE id = :invitationId
        AND deleted_at IS NULL
      """)
  Mono<ProjectInvitation> findByIdAndNotDeleted(String invitationId);

  @Query("""
      SELECT * FROM project_invitations
      WHERE project_id = :projectId
        AND deleted_at IS NULL
      ORDER BY created_at DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<ProjectInvitation> findByProjectIdAndNotDeleted(
      String projectId, int limit, int offset);

  @Query("""
      SELECT COUNT(*) FROM project_invitations
      WHERE project_id = :projectId
        AND deleted_at IS NULL
      """)
  Mono<Long> countByProjectIdAndNotDeleted(String projectId);

  @Query("""
      SELECT COUNT(*) FROM project_invitations
      WHERE project_id = :projectId
        AND invited_email = LOWER(:email)
        AND status = 'pending'
        AND deleted_at IS NULL
      """)
  Mono<Long> countPendingByProjectAndEmail(String projectId, String email);

  @Query("""
      UPDATE project_invitations
      SET deleted_at = CURRENT_TIMESTAMP
      WHERE project_id = :projectId
      """)
  Mono<Void> softDeleteByProjectId(String projectId);

  @Query("""
      SELECT * FROM project_invitations
      WHERE invited_email = LOWER(:email)
        AND status = 'pending'
        AND deleted_at IS NULL
      ORDER BY created_at DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<ProjectInvitation> findPendingByEmail(String email, int limit, int offset);

  @Query("""
      SELECT COUNT(*) FROM project_invitations
      WHERE invited_email = LOWER(:email)
        AND status = 'pending'
        AND deleted_at IS NULL
      """)
  Mono<Long> countPendingByEmail(String email);

}
