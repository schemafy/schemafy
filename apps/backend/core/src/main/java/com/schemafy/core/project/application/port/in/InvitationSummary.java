package com.schemafy.core.project.application.port.in;

import java.time.Instant;

public record InvitationSummary(
    String id,
    String targetType,
    String targetId,
    String targetName,
    String targetDescription,
    String invitedEmail,
    String invitedRole,
    String invitedBy,
    String status,
    Instant expiresAt,
    Instant createdAt) {
}
