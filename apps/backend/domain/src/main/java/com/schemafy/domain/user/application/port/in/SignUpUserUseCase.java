package com.schemafy.domain.user.application.port.in;

import com.schemafy.domain.user.domain.User;

import reactor.core.publisher.Mono;

public interface SignUpUserUseCase {

  Mono<User> signUpUser(SignUpUserCommand command);

}
