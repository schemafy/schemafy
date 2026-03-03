package com.schemafy.core.user.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.user.repository.entity.User;

import reactor.core.publisher.Mono;

/** Transitional read repository.
 * <p>
 * User write paths are owned by domain user adapters in this phase. */
@Deprecated(forRemoval = false)
public interface UserRepository extends ReactiveCrudRepository<User, String> {

  Mono<Boolean> existsByEmail(String email);

  Mono<User> findByEmail(String email);

}
