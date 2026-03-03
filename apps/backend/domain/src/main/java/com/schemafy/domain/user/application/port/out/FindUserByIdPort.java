package com.schemafy.domain.user.application.port.out;

import com.schemafy.domain.user.domain.User;

import reactor.core.publisher.Mono;

public interface FindUserByIdPort {

  Mono<User> findUserById(String userId);

}
