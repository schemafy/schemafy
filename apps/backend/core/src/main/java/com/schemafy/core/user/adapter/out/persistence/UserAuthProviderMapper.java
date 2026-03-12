package com.schemafy.core.user.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.core.user.domain.AuthProvider;
import com.schemafy.core.user.domain.UserAuthProvider;

@Component
class UserAuthProviderMapper {

  UserAuthProvider toDomain(UserAuthProviderEntity entity) {
    return new UserAuthProvider(
        entity.getId(),
        entity.getUserId(),
        AuthProvider.valueOf(entity.getProvider()),
        entity.getProviderUserId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt());
  }

  UserAuthProviderEntity toEntity(UserAuthProvider userAuthProvider) {
    return new UserAuthProviderEntity(
        userAuthProvider.id(),
        userAuthProvider.userId(),
        userAuthProvider.provider().name(),
        userAuthProvider.providerUserId(),
        userAuthProvider.createdAt(),
        userAuthProvider.updatedAt(),
        userAuthProvider.deletedAt());
  }

}
