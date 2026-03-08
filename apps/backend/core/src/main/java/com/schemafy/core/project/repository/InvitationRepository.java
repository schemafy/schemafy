package com.schemafy.core.project.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.project.repository.entity.Invitation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
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
      SELECT COUNT(*) FROM invitations
      WHERE target_type = :targetType
        AND invited_email = :email
        AND status = :status
        AND deleted_at IS NULL
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

}
