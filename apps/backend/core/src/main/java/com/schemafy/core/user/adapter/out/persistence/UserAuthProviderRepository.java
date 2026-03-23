package com.schemafy.core.user.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;

interface UserAuthProviderRepository
    extends ReactiveCrudRepository<UserAuthProviderEntity, String> {

  Mono<UserAuthProviderEntity> findByProviderAndProviderUserId(
      String provider,
      String providerUserId);

}
