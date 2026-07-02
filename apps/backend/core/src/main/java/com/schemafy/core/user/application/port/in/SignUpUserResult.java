package com.schemafy.core.user.application.port.in;

import java.time.Instant;

public record SignUpUserResult(String email, Instant expiresAt) {
}
