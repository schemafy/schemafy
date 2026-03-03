package com.schemafy.domain.user.adapter.out.persistence;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.user.application.port.out.CreateUserAuthProviderPort;
import com.schemafy.domain.user.application.port.out.FindUserAuthProviderPort;
import com.schemafy.domain.user.domain.AuthProvider;
import com.schemafy.domain.user.domain.UserAuthProvider;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class UserAuthProviderPersistenceAdapter implements
    FindUserAuthProviderPort,
    CreateUserAuthProviderPort {

  private final DomainUserAuthProviderRepository domainUserAuthProviderRepository;
  private final UserAuthProviderMapper userAuthProviderMapper;

  @Override
  public Mono<UserAuthProvider> findUserAuthProvider(
      AuthProvider provider,
      String providerUserId) {
    return domainUserAuthProviderRepository
        .findByProviderAndProviderUserId(provider.name(), providerUserId)
        .map(userAuthProviderMapper::toDomain);
  }

  @Override
  public Mono<UserAuthProvider> createUserAuthProvider(
      UserAuthProvider userAuthProvider) {
    return domainUserAuthProviderRepository
        .save(userAuthProviderMapper.toEntity(userAuthProvider))
        .map(userAuthProviderMapper::toDomain);
  }

}
