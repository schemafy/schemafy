package com.schemafy.core.user.application.port.out;

import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Mono;

public interface UpdateUserPort {

  Mono<User> updateUser(User user);

}
