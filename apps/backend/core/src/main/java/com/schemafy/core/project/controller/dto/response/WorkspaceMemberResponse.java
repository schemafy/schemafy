package com.schemafy.core.project.controller.dto.response;

import java.time.LocalDateTime;

import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.user.repository.entity.User;

public record WorkspaceMemberResponse(String id, String userId, String userName,
        String userEmail, String role, LocalDateTime joinedAt) {

    public static WorkspaceMemberResponse of(WorkspaceMember member,
            User user) {
        return new WorkspaceMemberResponse(
                member.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                member.getRole(),
                member.getJoinedAt());
    }

}
