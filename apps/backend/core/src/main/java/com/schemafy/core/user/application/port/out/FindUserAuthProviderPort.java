package com.schemafy.core.user.application.port.out;

import com.schemafy.core.user.domain.AuthProvider;
import com.schemafy.core.user.domain.UserAuthProvider;

import reactor.core.publisher.Mono;

public interface FindUserAuthProviderPort {

  Mono<UserAuthProvider> findUserAuthProvider(AuthProvider provider, String providerUserId);

}
