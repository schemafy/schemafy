package com.schemafy.domain.project.application.port.out;

import com.schemafy.domain.project.domain.Invitation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface InvitationPort {

  Mono<Invitation> save(Invitation invitation);

  Mono<Invitation> findByIdAndNotDeleted(String invitationId);

  Flux<Invitation> findInvitationsByTargetAndId(
      String targetType,
      String targetId,
      int limit,
      int offset);

  Mono<Long> countByTarget(String targetType, String targetId);

  Mono<Long> countByTargetAndEmailAndStatus(
      String targetType,
      String targetId,
      String email,
      String status);

  Flux<Invitation> findByEmailAndTypeAndStatus(
      String email,
      String targetType,
      String status,
      int limit,
      int offset);

  Mono<Long> countByEmailAndTypeAndStatus(
      String email,
      String targetType,
      String status);

  Mono<Long> updateStatusByTargetAndEmail(
      String targetType,
      String targetId,
      String email,
      String resultStatus,
      String currentStatus,
      String excludeId);

  Mono<Long> softDeleteByTarget(String targetType, String targetId);

}
