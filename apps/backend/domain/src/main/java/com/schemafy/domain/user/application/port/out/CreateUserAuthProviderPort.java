package com.schemafy.domain.user.application.port.out;

import com.schemafy.domain.user.domain.UserAuthProvider;

import reactor.core.publisher.Mono;

public interface CreateUserAuthProviderPort {

  Mono<UserAuthProvider> createUserAuthProvider(UserAuthProvider userAuthProvider);

}

