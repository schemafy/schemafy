package com.schemafy.domain.project.application.port.out;

import com.schemafy.domain.project.domain.ShareLink;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ShareLinkPort {

  Mono<ShareLink> save(ShareLink shareLink);

  Mono<ShareLink> findByCodeAndNotDeleted(String code);

  Mono<Void> incrementAccessCount(String shareLinkId);

  Flux<ShareLink> findByProjectIdAndNotDeleted(
      String projectId,
      int limit,
      int offset);

  Mono<Long> countByProjectIdAndNotDeleted(String projectId);

  Mono<ShareLink> findByIdAndProjectIdAndNotDeleted(
      String shareLinkId,
      String projectId);

  Mono<Long> softDeleteByProjectId(String projectId);

}
