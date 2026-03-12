package com.schemafy.core.user.application.port.out;

import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Mono;

public interface FindUserByIdPort {

  Mono<User> findUserById(String userId);

}
