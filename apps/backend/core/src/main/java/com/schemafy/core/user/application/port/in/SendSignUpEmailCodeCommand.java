package com.schemafy.core.user.application.port.in;

import com.schemafy.core.user.domain.Email;

public record SendSignUpEmailCodeCommand(String email) {

  public SendSignUpEmailCodeCommand {
    email = Email.from(email).address();
  }

}
