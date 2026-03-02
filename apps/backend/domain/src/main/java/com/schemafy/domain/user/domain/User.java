package com.schemafy.domain.user.domain;

import java.time.Instant;

public record User(String id, String email, String name, String password, UserStatus status, Instant createdAt,
    Instant updatedAt, Instant deletedAt) {

  private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

  public User {
    validateEmail(email);
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

  private static void validateEmail(String email) {
    if (email == null || !email.matches(EMAIL_REGEX)) {
      throw new IllegalArgumentException("Invalid email format");
    }
  }

}

