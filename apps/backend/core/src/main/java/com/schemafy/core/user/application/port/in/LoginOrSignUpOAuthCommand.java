package com.schemafy.core.user.application.port.in;

import com.schemafy.core.user.domain.AuthProvider;

public record LoginOrSignUpOAuthCommand(
    String email,
    String name,
    AuthProvider provider,
    String providerUserId) {

  public LoginOrSignUpOAuthCommand withEmail(String email) {
    return new LoginOrSignUpOAuthCommand(email, name, provider, providerUserId);
  }

}
