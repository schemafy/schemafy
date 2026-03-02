package com.schemafy.domain.user.application.port.in;

import reactor.core.publisher.Mono;

public interface LoginOrSignUpOAuthUseCase {

  Mono<LoginOrSignUpOAuthResult> loginOrSignUpOAuth(LoginOrSignUpOAuthCommand command);

}

