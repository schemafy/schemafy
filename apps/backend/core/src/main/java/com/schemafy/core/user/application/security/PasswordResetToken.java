package com.schemafy.core.user.application.security;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.exception.UserErrorCode;

public record PasswordResetToken(String userId, String rawToken) {

  public static PasswordResetToken of(String userId, String rawToken) {
    if (userId == null || userId.isBlank() || rawToken == null || rawToken.isBlank()) {
      throw invalid();
    }
    return new PasswordResetToken(userId, rawToken);
  }

  public static PasswordResetToken parse(String value) {
    if (value == null) {
      throw invalid();
    }
    String[] parts = value.split("\\.", 2);
    return parts.length == 2 ? of(parts[0], parts[1]) : invalidToken();
  }

  public String value() {
    return userId + "." + rawToken;
  }

  private static PasswordResetToken invalidToken() {
    throw invalid();
  }

  private static DomainException invalid() {
    return new DomainException(UserErrorCode.PASSWORD_RESET_TOKEN_INVALID);
  }

}
