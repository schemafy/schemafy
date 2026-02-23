package com.schemafy.core.user.service.dto;

import com.schemafy.core.user.repository.vo.AuthProvider;

public record OAuthLoginCommand(
    String email,
    String name,
    AuthProvider provider,
    String providerUserId) {

}
