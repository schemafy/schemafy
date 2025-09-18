package com.schemafy.core.member.domain.repository;

import com.schemafy.core.member.domain.entity.Member;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MemberRepository extends ReactiveCrudRepository<Member, String> {
    Mono<Boolean> existsByEmail(String email);
    Mono<Member> findByEmail(String email);
}