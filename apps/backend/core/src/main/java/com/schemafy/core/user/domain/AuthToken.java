package com.schemafy.core.user.domain;

import java.time.Instant;

public record AuthToken(
    AuthTokenType tokenType,
    String subject,
    String token,
    int attemptCount,
    int maxAttemptCount,
    Instant expiresAt) {
}
