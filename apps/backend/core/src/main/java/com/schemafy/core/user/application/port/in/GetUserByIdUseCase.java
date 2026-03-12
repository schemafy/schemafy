package com.schemafy.core.user.application.port.in;

import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Mono;

public interface GetUserByIdUseCase {

  Mono<User> getUserById(GetUserByIdQuery query);

}
