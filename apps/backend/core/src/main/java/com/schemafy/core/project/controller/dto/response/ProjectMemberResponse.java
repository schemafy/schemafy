package com.schemafy.core.project.controller.dto.response;

import java.time.LocalDateTime;

public record ProjectMemberResponse(String id, String userId, String userName,
        String userEmail, String role, LocalDateTime joinedAt) {
}
