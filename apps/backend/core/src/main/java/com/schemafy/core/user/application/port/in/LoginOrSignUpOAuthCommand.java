package com.schemafy.core.user.application.port.in;

import java.util.Locale;

import com.schemafy.core.user.domain.AuthProvider;
import com.schemafy.core.user.domain.Email;
import com.schemafy.core.user.domain.UserPolicy;

public record LoginOrSignUpOAuthCommand(
    String email,
    String name,
    AuthProvider provider,
    String providerUserId) {

  public LoginOrSignUpOAuthCommand {
    email = Email.from(email).address();
    name = normalizeOAuthName(name, provider, providerUserId);
  }

  private static String normalizeOAuthName(
      String name,
      AuthProvider provider,
      String providerUserId) {
    String resolvedName = name;

    if ((resolvedName == null || resolvedName.isBlank())
        && provider != null
        && providerUserId != null
        && !providerUserId.isBlank()) {
      resolvedName = provider.name().toLowerCase(Locale.ROOT) + "-" + providerUserId;
    }
    resolvedName = UserPolicy.truncateName(resolvedName);
    UserPolicy.validateName(resolvedName);
    return resolvedName;
  }

}
