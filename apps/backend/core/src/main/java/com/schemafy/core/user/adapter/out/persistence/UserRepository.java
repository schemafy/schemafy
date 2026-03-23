package com.schemafy.core.user.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;

interface UserRepository extends ReactiveCrudRepository<UserEntity, String> {

  Mono<Boolean> existsByEmail(String email);

  Mono<UserEntity> findByEmail(String email);

}
