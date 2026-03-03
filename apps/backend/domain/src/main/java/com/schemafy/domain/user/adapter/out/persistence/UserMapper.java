package com.schemafy.domain.user.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.schemafy.domain.user.domain.User;
import com.schemafy.domain.user.domain.UserStatus;

@Component
class UserMapper {

  User toDomain(UserEntity entity) {
    return new User(
        entity.getId(),
        entity.getEmail(),
        entity.getName(),
        entity.getPassword(),
        UserStatus.valueOf(entity.getStatus()),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt());
  }

  UserEntity toEntity(User user) {
    return new UserEntity(
        user.id(),
        user.email(),
        user.name(),
        user.password(),
        user.status().name(),
        user.createdAt(),
        user.updatedAt(),
        user.deletedAt());
  }

}
