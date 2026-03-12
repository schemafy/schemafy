package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.ShareLink;

import reactor.core.publisher.Mono;

public interface CreateShareLinkUseCase {

  Mono<ShareLink> createShareLink(CreateShareLinkCommand command);

}
