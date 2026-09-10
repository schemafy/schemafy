package com.schemafy.core.user.domain;

import java.nio.charset.StandardCharsets;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.exception.UserErrorCode;

public final class UserPolicy {

  public static final int MAX_NAME_LENGTH = 200;
  public static final int MIN_PASSWORD_LENGTH = 8;
  public static final int MAX_PASSWORD_LENGTH = 72;
  public static final int MAX_PASSWORD_BYTES = 72;

  private UserPolicy() {}

  public static void validateName(final String name) {
    if (!isValidName(name)) {
      throw new DomainException(UserErrorCode.INVALID_PARAMETER, "Invalid name");
    }
  }

  public static void validatePassword(final String password) {
    if (!isValidPassword(password)) {
      throw new DomainException(UserErrorCode.INVALID_PARAMETER, "Invalid password");
    }
  }

  public static boolean isValidName(final String name) {
    return name != null
        && !name.isBlank()
        && name.length() <= MAX_NAME_LENGTH;
  }

  public static boolean isValidPassword(final String password) {
    return password != null
        && !password.isBlank()
        && password.length() >= MIN_PASSWORD_LENGTH
        && password.length() <= MAX_PASSWORD_LENGTH
        && password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES;
  }

  public static String truncateName(final String name) {
    if (name == null || name.length() <= MAX_NAME_LENGTH) {
      return name;
    }
    return name.substring(0, MAX_NAME_LENGTH);
  }

}
