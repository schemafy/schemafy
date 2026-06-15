package com.schemafy.core.user.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.Email;
import com.schemafy.core.user.domain.UserPolicy;
import com.schemafy.core.user.domain.exception.UserErrorCode;

public record SignUpUserCommand(String email, String name, String password) {

  public SignUpUserCommand {
    email = Email.from(email).address();
    UserPolicy.validateName(name);

    if (password == null || password.isBlank()
        || password.length() < UserPolicy.MIN_SIGN_UP_PASSWORD_LENGTH) {
      throw new DomainException(UserErrorCode.INVALID_PARAMETER, "Invalid password");
    }
  }

}
