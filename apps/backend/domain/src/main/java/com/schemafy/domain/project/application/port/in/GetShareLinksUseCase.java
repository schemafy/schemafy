package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.common.PageResult;
import com.schemafy.domain.project.domain.ShareLink;

import reactor.core.publisher.Mono;

public interface GetShareLinksUseCase {

  Mono<PageResult<ShareLink>> getShareLinks(GetShareLinksQuery query);

}
