package com.schemafy.core.user.application.port.in;

import reactor.core.publisher.Mono;

public interface VerifySignUpEmailUseCase {

  Mono<VerifySignUpEmailResult> verifySignUpEmail(VerifySignUpEmailCommand command);

}
