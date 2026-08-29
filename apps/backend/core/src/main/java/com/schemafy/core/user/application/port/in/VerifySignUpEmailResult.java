package com.schemafy.core.user.application.port.in;

import java.time.Instant;

public record VerifySignUpEmailResult(
    String email,
    String signupVerificationToken,
    Instant expiresAt) {
}
