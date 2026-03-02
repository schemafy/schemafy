package com.schemafy.domain.user.application.port.in;

import com.schemafy.domain.user.domain.AuthProvider;

public record LoginOrSignUpOAuthCommand(
    String email,
    String name,
    AuthProvider provider,
    String providerUserId) {
}

