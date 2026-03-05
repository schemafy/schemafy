package com.schemafy.domain.user.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;

// TODO: core 모듈 정리 시에 해당 클래스 이름 변경
interface DomainUserRepository extends ReactiveCrudRepository<UserEntity, String> {

  Mono<Boolean> existsByEmail(String email);

  Mono<UserEntity> findByEmail(String email);

}
