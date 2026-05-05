package com.schemafy.core.project.adapter.out.persistence;

import com.schemafy.core.common.PersistenceAdapter;
import com.schemafy.core.project.application.port.in.InvitationSummary;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
public class InvitationPersistenceAdapter implements InvitationPort {

  private final InvitationRepository invitationRepository;

  @Override
  public Mono<Invitation> save(Invitation invitation) {
    return invitationRepository.save(invitation);
  }

  @Override
  public Mono<Invitation> findByIdAndNotDeleted(String invitationId) {
    return invitationRepository.findByIdAndNotDeleted(invitationId);
  }

  @Override
  public Flux<Invitation> findInvitationsByTargetAndId(String targetType,
      String targetId, int limit, int offset) {
    return invitationRepository.findInvitationsByTargetAndId(targetType,
        targetId, limit, offset);
  }

  @Override
  public Mono<Long> countByTarget(String targetType, String targetId) {
    return invitationRepository.countByTarget(targetType, targetId);
  }

  @Override
  public Mono<Long> countByTargetAndEmailAndStatus(String targetType,
      String targetId, String email, String status) {
    return invitationRepository.countByTargetAndEmailAndStatus(targetType,
        targetId, email, status);
  }

  @Override
  public Flux<Invitation> findByEmailAndTypeAndStatus(String email,
      String targetType, String status, int limit, int offset) {
    return invitationRepository.findByEmailAndTypeAndStatus(email, targetType,
        status, limit, offset);
  }

  @Override
  public Flux<InvitationSummary> findMyPendingInvitationSummaries(
      String email, String cursorId, int limit) {
    String status = InvitationStatus.PENDING.name();
    if (cursorId == null) {
      return invitationRepository.findMyInvitationSummariesFirstPage(email,
          status, limit);
    }
    return invitationRepository.findMyInvitationSummariesNextPage(email,
        status, cursorId, limit);
  }

  @Override
  public Mono<Long> countByEmailAndTypeAndStatus(String email,
      String targetType, String status) {
    return invitationRepository.countByEmailAndTypeAndStatus(email, targetType,
        status);
  }

  @Override
  public Mono<Long> updateStatusByTargetAndEmail(String targetType,
      String targetId, String email, String resultStatus,
      String currentStatus, String excludeId) {
    return invitationRepository.updateStatusByTargetAndEmail(targetType,
        targetId, email, resultStatus, currentStatus, excludeId);
  }

  @Override
  public Mono<Long> softDeleteByTarget(String targetType, String targetId) {
    return invitationRepository.softDeleteByTarget(targetType, targetId);
  }

}
