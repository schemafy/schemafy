package com.schemafy.core.user.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.user.repository.entity.User;

import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, String> {
    Mono<Boolean> existsByEmail(String email);

    Mono<User> findByEmail(String email);
}
