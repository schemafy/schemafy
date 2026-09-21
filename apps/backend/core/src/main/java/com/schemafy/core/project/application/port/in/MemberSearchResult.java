package com.schemafy.core.project.application.port.in;

import java.time.Instant;

public record MemberSearchResult(
    String userId,
    String userName,
    String userEmail,
    String role,
    Instant joinedAt) {
}
