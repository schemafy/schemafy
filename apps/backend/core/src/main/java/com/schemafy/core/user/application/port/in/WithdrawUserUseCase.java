package com.schemafy.core.user.application.port.in;

import reactor.core.publisher.Mono;

public interface WithdrawUserUseCase {

  Mono<Void> withdrawUser(WithdrawUserCommand command);

}
