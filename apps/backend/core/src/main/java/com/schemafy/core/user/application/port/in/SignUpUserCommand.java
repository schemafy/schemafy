package com.schemafy.core.user.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.exception.UserErrorCode;

public record SignUpUserCommand(String email, String name, String password) {

  private static final int MIN_PASSWORD_LENGTH = 8;

  public SignUpUserCommand {
    if (password == null || password.isBlank()
        || password.length() < MIN_PASSWORD_LENGTH) {
      throw new DomainException(UserErrorCode.INVALID_PARAMETER);
    }
  }

  public SignUpUserCommand withEmail(String email) {
    return new SignUpUserCommand(email, name, password);
  }

}
