package com.schemafy.core.user.service.dto;

import com.schemafy.domain.user.domain.AuthProvider;

public record OAuthLoginCommand(
    String email,
    String name,
    AuthProvider provider,
    String providerUserId) {

}
