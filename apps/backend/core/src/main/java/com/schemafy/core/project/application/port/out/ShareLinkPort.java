package com.schemafy.core.project.application.port.out;

import com.schemafy.core.project.domain.ShareLink;

import reactor.core.publisher.Mono;

public interface ShareLinkPort {

  Mono<ShareLink> save(ShareLink shareLink);

  Mono<ShareLink> findByIdAndNotDeleted(String id);

  Mono<ShareLink> findByProjectIdAndNotDeleted(String projectId);

  Mono<Void> deleteByProjectId(String projectId);

}
