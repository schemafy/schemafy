package com.schemafy.core.user.domain;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.exception.UserErrorCode;

public final class UserPolicy {

  public static final int MAX_NAME_LENGTH = 200;
  public static final int MIN_SIGN_UP_PASSWORD_LENGTH = 8;

  private UserPolicy() {}

  public static void validateName(final String name) {
    if (!isValidName(name)) {
      throw new DomainException(UserErrorCode.INVALID_PARAMETER, "Invalid name");
    }
  }

  public static boolean isValidName(final String name) {
    return name != null
        && !name.isBlank()
        && name.length() <= MAX_NAME_LENGTH;
  }

  public static String truncateName(final String name) {
    if (name == null || name.length() <= MAX_NAME_LENGTH) {
      return name;
    }
    return name.substring(0, MAX_NAME_LENGTH);
  }

}
