package com.schemafy.core.user.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;

// TODO: core 모듈 정리 시에 해당 클래스 이름 변경
interface DomainUserAuthProviderRepository
    extends ReactiveCrudRepository<UserAuthProviderEntity, String> {

  Mono<UserAuthProviderEntity> findByProviderAndProviderUserId(
      String provider,
      String providerUserId);

}
