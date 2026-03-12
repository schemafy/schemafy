package com.schemafy.domain.project.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteShareLinkUseCase {

  Mono<Void> deleteShareLink(DeleteShareLinkCommand command);

}
