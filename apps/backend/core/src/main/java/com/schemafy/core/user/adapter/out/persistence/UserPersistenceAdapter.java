package com.schemafy.core.user.adapter.out.persistence;

import java.util.Set;

import com.schemafy.core.common.PersistenceAdapter;
import com.schemafy.core.user.application.port.out.CreateUserPort;
import com.schemafy.core.user.application.port.out.ExistsUserByEmailPort;
import com.schemafy.core.user.application.port.out.FindUserByEmailPort;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.application.port.out.FindUsersByIdsPort;
import com.schemafy.core.user.domain.User;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class UserPersistenceAdapter implements
    ExistsUserByEmailPort,
    CreateUserPort,
    FindUserByEmailPort,
    FindUserByIdPort,
    FindUsersByIdsPort {

  private final DomainUserRepository domainUserRepository;
  private final UserMapper userMapper;

  @Override
  public Mono<Boolean> existsUserByEmail(String email) {
    return domainUserRepository.existsByEmail(email);
  }

  @Override
  public Mono<User> createUser(User user) {
    return domainUserRepository.save(userMapper.toEntity(user))
        .map(userMapper::toDomain);
  }

  @Override
  public Mono<User> findUserByEmail(String email) {
    return domainUserRepository.findByEmail(email)
        .map(userMapper::toDomain);
  }

  @Override
  public Mono<User> findUserById(String userId) {
    return domainUserRepository.findById(userId)
        .map(userMapper::toDomain);
  }

  @Override
  public Flux<User> findUsersByIds(Set<String> userIds) {
    return domainUserRepository.findAllById(userIds)
        .map(userMapper::toDomain);
  }

}
