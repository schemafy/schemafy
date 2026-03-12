package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.ShareLink;

import reactor.core.publisher.Mono;

public interface RevokeShareLinkUseCase {

  Mono<ShareLink> revokeShareLink(RevokeShareLinkCommand command);

}
