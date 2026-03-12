package com.schemafy.core.user.application.port.out;

import com.schemafy.core.user.domain.UserAuthProvider;

import reactor.core.publisher.Mono;

public interface CreateUserAuthProviderPort {

  Mono<UserAuthProvider> createUserAuthProvider(UserAuthProvider userAuthProvider);

}
