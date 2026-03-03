package com.schemafy.domain.user.application.port.out;

import com.schemafy.domain.user.domain.User;

import reactor.core.publisher.Mono;

public interface CreateUserPort {

  Mono<User> createUser(User user);

}
