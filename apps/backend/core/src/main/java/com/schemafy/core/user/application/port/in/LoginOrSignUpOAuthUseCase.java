package com.schemafy.core.user.application.port.in;

import reactor.core.publisher.Mono;

public interface LoginOrSignUpOAuthUseCase {

  Mono<LoginOrSignUpOAuthResult> loginOrSignUpOAuth(LoginOrSignUpOAuthCommand command);

}
