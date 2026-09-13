package com.schemafy.core.user.application.port.in;

import com.schemafy.core.user.domain.Email;
import com.schemafy.core.user.domain.UserPolicy;

public record SignUpUserCommand(String email, String name, String password,
    String signupVerificationToken) {

  public SignUpUserCommand {
    email = Email.from(email).address();
    UserPolicy.validateName(name);
    UserPolicy.validatePassword(password);
  }

}
