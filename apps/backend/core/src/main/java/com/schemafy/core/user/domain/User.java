package com.schemafy.core.user.domain;

import java.time.Instant;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.exception.UserErrorCode;

public record User(String id, String email, String name, String password, UserStatus status, Instant createdAt,
    Instant updatedAt, Instant deletedAt) {

  private static final int MAX_NAME_LENGTH = 200;

  public User {
    email = Email.from(email).address();
    if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
      throw new DomainException(UserErrorCode.INVALID_PARAMETER);
    }
  }

  public static User signUp(
      String id,
      String email,
      String name,
      String encodedPassword) {
    if (encodedPassword == null || encodedPassword.isBlank()) {
      throw new DomainException(UserErrorCode.INVALID_PARAMETER);
    }
    return new User(
        id,
        email,
        name,
        encodedPassword,
        UserStatus.ACTIVE,
        null,
        null,
        null);
  }

  public static User signUpOAuth(
      String id,
      String email,
      String name) {
    return new User(
        id,
        email,
        name,
        null,
        UserStatus.ACTIVE,
        null,
        null,
        null);
  }

}
