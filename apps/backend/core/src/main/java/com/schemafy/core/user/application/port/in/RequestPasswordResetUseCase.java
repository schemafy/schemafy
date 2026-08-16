package com.schemafy.core.user.application.port.in;

import reactor.core.publisher.Mono;

public interface RequestPasswordResetUseCase {

  Mono<Void> requestPasswordReset(RequestPasswordResetCommand command);

}
