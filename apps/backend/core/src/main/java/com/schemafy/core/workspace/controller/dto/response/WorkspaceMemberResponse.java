package com.schemafy.core.workspace.controller.dto.response;

import java.time.LocalDateTime;

public record WorkspaceMemberResponse(String id, String userId, String userName,
        String userEmail, String role, LocalDateTime joinedAt) {
}
