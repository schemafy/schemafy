package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.ShareLink;

import reactor.core.publisher.Mono;

public interface RevokeShareLinkUseCase {

  Mono<ShareLink> revokeShareLink(RevokeShareLinkCommand command);

}
