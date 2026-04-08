package com.schemafy.core.project.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

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
      SELECT * FROM invitations
      WHERE invited_email = :email
        AND status = :status
        AND deleted_at IS NULL
        AND expires_at > NOW()
      ORDER BY id DESC
      LIMIT :limit
      """)
  Flux<Invitation> findMyInvitationsByEmailAndStatus(
      String email,
      String status,
      int limit);

  @Query("""
      SELECT * FROM invitations
      WHERE invited_email = :email
        AND status = :status
        AND deleted_at IS NULL
        AND expires_at > NOW()
        AND id < :cursorId
      ORDER BY id DESC
      LIMIT :limit
      """)
  Flux<Invitation> findMyInvitationsByEmailAndStatusBeforeId(
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
