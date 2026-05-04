package com.schemafy.core.project.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.project.application.port.in.InvitationSummary;
import com.schemafy.core.project.domain.Invitation;

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
      WHERE target_type = :targetType
        AND target_id = :targetId
        AND deleted_at IS NULL
      ORDER BY created_at DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<Invitation> findInvitationsByTargetAndId(
      String targetType,
      String targetId,
      int limit,
      int offset);

  @Query("""
      SELECT COUNT(*) FROM invitations
      WHERE target_type = :targetType
        AND target_id = :targetId
        AND deleted_at IS NULL
      """)
  Mono<Long> countByTarget(
      String targetType,
      String targetId);

  @Query("""
      SELECT COUNT(*) FROM invitations
      WHERE target_type = :targetType
        AND target_id = :targetId
        AND invited_email = :email
        AND status = :status
        AND deleted_at IS NULL
        AND expires_at > NOW()
      """)
  Mono<Long> countByTargetAndEmailAndStatus(
      String targetType,
      String targetId,
      String email,
      String status);

  @Query("""
      SELECT * FROM invitations
      WHERE target_type = :targetType
        AND invited_email = :email
        AND status = :status
        AND deleted_at IS NULL
        AND expires_at > NOW()
      ORDER BY created_at DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<Invitation> findByEmailAndTypeAndStatus(
      String email,
      String targetType,
      String status,
      int limit,
      int offset);

  @Query("""
      SELECT *
      FROM (
        SELECT * FROM (
          SELECT
            i.id AS id,
            i.target_type AS target_type,
            i.target_id AS target_id,
            w.name AS target_name,
            w.description AS target_description,
            i.invited_email AS invited_email,
            i.invited_role AS invited_role,
            i.invited_by AS invited_by,
            i.status AS status,
            i.expires_at AS expires_at,
            i.created_at AS created_at
          FROM invitations i
          JOIN workspaces w
            ON i.target_type = 'WORKSPACE'
           AND i.target_id = w.id
           AND w.deleted_at IS NULL
          WHERE i.invited_email = :email
            AND i.status = :status
            AND i.deleted_at IS NULL
            AND i.expires_at > NOW()
          ORDER BY i.id DESC
          LIMIT :limit
        ) workspace_invitations
        UNION ALL
        SELECT * FROM (
          SELECT
            i.id AS id,
            i.target_type AS target_type,
            i.target_id AS target_id,
            p.name AS target_name,
            p.description AS target_description,
            i.invited_email AS invited_email,
            i.invited_role AS invited_role,
            i.invited_by AS invited_by,
            i.status AS status,
            i.expires_at AS expires_at,
            i.created_at AS created_at
          FROM invitations i
          JOIN projects p
            ON i.target_type = 'PROJECT'
           AND i.target_id = p.id
           AND p.deleted_at IS NULL
          WHERE i.invited_email = :email
            AND i.status = :status
            AND i.deleted_at IS NULL
            AND i.expires_at > NOW()
          ORDER BY i.id DESC
          LIMIT :limit
        ) project_invitations
      ) my_invitations
      ORDER BY id DESC
      LIMIT :limit
      """)
  Flux<InvitationSummary> findMyInvitationSummariesFirstPage(
      String email,
      String status,
      int limit);

  @Query("""
      SELECT *
      FROM (
        SELECT * FROM (
          SELECT
            i.id AS id,
            i.target_type AS target_type,
            i.target_id AS target_id,
            w.name AS target_name,
            w.description AS target_description,
            i.invited_email AS invited_email,
            i.invited_role AS invited_role,
            i.invited_by AS invited_by,
            i.status AS status,
            i.expires_at AS expires_at,
            i.created_at AS created_at
          FROM invitations i
          JOIN workspaces w
            ON i.target_type = 'WORKSPACE'
           AND i.target_id = w.id
           AND w.deleted_at IS NULL
          WHERE i.invited_email = :email
            AND i.status = :status
            AND i.deleted_at IS NULL
            AND i.expires_at > NOW()
            AND i.id < :cursorId
          ORDER BY i.id DESC
          LIMIT :limit
        ) workspace_invitations
        UNION ALL
        SELECT * FROM (
          SELECT
            i.id AS id,
            i.target_type AS target_type,
            i.target_id AS target_id,
            p.name AS target_name,
            p.description AS target_description,
            i.invited_email AS invited_email,
            i.invited_role AS invited_role,
            i.invited_by AS invited_by,
            i.status AS status,
            i.expires_at AS expires_at,
            i.created_at AS created_at
          FROM invitations i
          JOIN projects p
            ON i.target_type = 'PROJECT'
           AND i.target_id = p.id
           AND p.deleted_at IS NULL
          WHERE i.invited_email = :email
            AND i.status = :status
            AND i.deleted_at IS NULL
            AND i.expires_at > NOW()
            AND i.id < :cursorId
          ORDER BY i.id DESC
          LIMIT :limit
        ) project_invitations
      ) my_invitations
      ORDER BY id DESC
      LIMIT :limit
      """)
  Flux<InvitationSummary> findMyInvitationSummariesNextPage(
      String email,
      String status,
      String cursorId,
      int limit);

  @Query("""
      SELECT COUNT(*) FROM invitations
      WHERE target_type = :targetType
        AND invited_email = :email
        AND status = :status
        AND deleted_at IS NULL
        AND expires_at > NOW()
      """)
  Mono<Long> countByEmailAndTypeAndStatus(
      String email,
      String targetType,
      String status);

  @Modifying
  @Query("""
      UPDATE invitations
      SET status = :resultStatus, resolved_at = NOW(), updated_at = NOW(), version = version + 1
      WHERE target_type = :targetType
        AND target_id = :targetId
        AND invited_email = :email
        AND status = :currentStatus
        AND expires_at > NOW()
        AND deleted_at IS NULL
        AND id != :excludeId
      """)
  Mono<Long> updateStatusByTargetAndEmail(
      String targetType,
      String targetId,
      String email,
      String resultStatus,
      String currentStatus,
      String excludeId);

  @Modifying
  @Query("""
      UPDATE invitations
      SET deleted_at = NOW(), updated_at = NOW(), version = version + 1
      WHERE target_type = :targetType
        AND target_id = :targetId
        AND deleted_at IS NULL
      """)
  Mono<Long> softDeleteByTarget(String targetType, String targetId);

}
