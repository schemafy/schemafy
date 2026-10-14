package com.schemafy.core.user.application.port.in;

import reactor.core.publisher.Mono;

public interface SendSignUpEmailCodeUseCase {

  Mono<SignUpEmailVerificationResult> sendSignUpEmailCode(SendSignUpEmailCodeCommand command);

}
