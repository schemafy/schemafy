package com.schemafy.core.user.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.user.repository.entity.UserAuthProvider;

import reactor.core.publisher.Mono;

/** Transitional read repository.
 * <p>
 * User auth-provider write paths are owned by domain user adapters in this phase. */
@Deprecated(forRemoval = false)
public interface UserAuthProviderRepository
    extends ReactiveCrudRepository<UserAuthProvider, String> {

  Mono<UserAuthProvider> findByProviderAndProviderUserId(String provider,
      String providerUserId);

}
