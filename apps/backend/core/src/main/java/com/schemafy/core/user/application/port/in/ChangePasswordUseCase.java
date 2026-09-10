package com.schemafy.core.user.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangePasswordUseCase {

  Mono<Void> changePassword(String authenticatedUserId, ChangePasswordCommand command);

}
