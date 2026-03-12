package com.schemafy.core.project.adapter.out.persistence;

import com.schemafy.core.common.PersistenceAdapter;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.ShareLink;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
public class ShareLinkPersistenceAdapter implements ShareLinkPort {

  private final DomainShareLinkRepository shareLinkRepository;

  @Override
  public Mono<ShareLink> save(ShareLink shareLink) {
    return shareLinkRepository.save(shareLink);
  }

  @Override
  public Mono<ShareLink> findByCodeAndNotDeleted(String code) {
    return shareLinkRepository.findByCodeAndNotDeleted(code);
  }

  @Override
  public Mono<Void> incrementAccessCount(String shareLinkId) {
    return shareLinkRepository.incrementAccessCount(shareLinkId);
  }

  @Override
  public Flux<ShareLink> findByProjectIdAndNotDeleted(String projectId,
      int limit, int offset) {
    return shareLinkRepository.findByProjectIdAndNotDeleted(projectId, limit,
        offset);
  }

  @Override
  public Mono<Long> countByProjectIdAndNotDeleted(String projectId) {
    return shareLinkRepository.countByProjectIdAndNotDeleted(projectId);
  }

  @Override
  public Mono<ShareLink> findByIdAndProjectIdAndNotDeleted(String shareLinkId,
      String projectId) {
    return shareLinkRepository.findByIdAndProjectIdAndNotDeleted(shareLinkId,
        projectId);
  }

  @Override
  public Mono<Long> softDeleteByProjectId(String projectId) {
    return shareLinkRepository.softDeleteByProjectId(projectId);
  }

}
