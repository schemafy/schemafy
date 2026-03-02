package com.schemafy.domain.user.application.port.in;

import com.schemafy.domain.user.domain.User;

import reactor.core.publisher.Mono;

public interface GetUserByIdUseCase {

  Mono<User> getUserById(GetUserByIdQuery query);

}

