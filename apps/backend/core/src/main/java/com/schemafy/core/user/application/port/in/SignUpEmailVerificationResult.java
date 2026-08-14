package com.schemafy.core.user.application.port.in;

import java.time.Instant;

public record SignUpEmailVerificationResult(String email, Instant expiresAt) {
}
