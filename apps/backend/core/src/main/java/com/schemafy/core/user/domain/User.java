package com.schemafy.core.user.domain;

import java.time.Instant;

public record User(String id, String email, String name, String password, UserStatus status, Instant createdAt,
    Instant updatedAt, Instant deletedAt) {

  public User {
    email = Email.from(email).address();
  }

  public static User signUp(
      String id,
      String email,
      String name,
      String encodedPassword) {
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
