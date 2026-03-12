package com.schemafy.api.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.api.project.orchestrator.dto.ProjectMemberView;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.user.domain.User;

public record ProjectMemberResponse(String projectId, String userId,
    String userName, String userEmail, String role, Instant joinedAt) {

  public static ProjectMemberResponse of(ProjectMember member, User user) {
    return new ProjectMemberResponse(member.getProjectId(),
        member.getUserId(), user.name(), user.email(), member.getRole(),
        member.getJoinedAt());
  }

  public static ProjectMemberResponse from(ProjectMemberView detail) {
    return of(detail.member(), detail.user());
  }

}
