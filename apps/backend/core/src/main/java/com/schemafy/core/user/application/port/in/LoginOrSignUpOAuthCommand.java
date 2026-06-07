package com.schemafy.core.user.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.AuthProvider;
import com.schemafy.core.user.domain.Email;
import com.schemafy.core.user.domain.UserPolicy;
import com.schemafy.core.user.domain.exception.UserErrorCode;

public record LoginOrSignUpOAuthCommand(
    String email,
    String name,
    AuthProvider provider,
    String providerUserId) {

  public LoginOrSignUpOAuthCommand {
    email = Email.from(email).address();
    if (name == null || name.isBlank()
        || name.length() > UserPolicy.MAX_NAME_LENGTH) {
      throw new DomainException(UserErrorCode.INVALID_PARAMETER);
    }
  }

}
