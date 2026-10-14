package com.schemafy.core.user.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.Email;
import com.schemafy.core.user.domain.exception.UserErrorCode;

public record VerifySignUpEmailCommand(String email, String code) {

  public VerifySignUpEmailCommand {
    email = Email.from(email).address();
    if (code == null || !code.matches("\\d{6}")) {
      throw new DomainException(UserErrorCode.INVALID_PARAMETER, "Invalid verification code");
    }
  }

}
