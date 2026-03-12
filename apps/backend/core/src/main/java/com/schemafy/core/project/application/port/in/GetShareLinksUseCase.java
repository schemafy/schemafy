package com.schemafy.core.project.application.port.in;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.domain.ShareLink;

import reactor.core.publisher.Mono;

public interface GetShareLinksUseCase {

  Mono<PageResult<ShareLink>> getShareLinks(GetShareLinksQuery query);

}
