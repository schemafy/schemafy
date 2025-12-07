package com.schemafy.core.project.controller.dto.response;

import java.time.LocalDateTime;

import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.user.repository.entity.User;

public record ProjectMemberResponse(String id, String userId, String userName,
        String userEmail, String role, LocalDateTime joinedAt) {

    public static ProjectMemberResponse of(ProjectMember member, User user) {
        return new ProjectMemberResponse(member.getId(), member.getUserId(),
                user.getName(), user.getEmail(), member.getRole(),
                member.getJoinedAt());
    }

}
