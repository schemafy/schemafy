package com.schemafy.core.project.adapter.out.persistence;

import com.schemafy.core.common.PersistenceAdapter;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.ShareLink;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
public class ShareLinkPersistenceAdapter implements ShareLinkPort {

  private final ShareLinkRepository shareLinkRepository;

  @Override
  public Mono<ShareLink> save(ShareLink shareLink) {
    return shareLinkRepository.save(shareLink);
  }

  @Override
  public Mono<ShareLink> findByIdAndNotDeleted(String id) {
    return shareLinkRepository.findByIdAndNotDeleted(id);
  }

  @Override
  public Mono<ShareLink> findByProjectIdAndNotDeleted(String projectId) {
    return shareLinkRepository.findByProjectIdAndNotDeleted(projectId);
  }

  @Override
  public Mono<Void> deleteByProjectId(String projectId) {
    return shareLinkRepository.deleteByProjectId(projectId);
  }

}
