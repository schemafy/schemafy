package com.schemafy.domain.user.application.port.out;

import com.schemafy.domain.user.domain.AuthProvider;
import com.schemafy.domain.user.domain.UserAuthProvider;

import reactor.core.publisher.Mono;

public interface FindUserAuthProviderPort {

  Mono<UserAuthProvider> findUserAuthProvider(AuthProvider provider, String providerUserId);

}

