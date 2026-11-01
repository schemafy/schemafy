package com.schemafy.api.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.api.common.constant.UserDisplayConstants;
import com.schemafy.api.project.orchestrator.dto.ProjectMemberView;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.UserStatus;

public record ProjectMemberResponse(String projectId, String userId,
    String userName, String userEmail, String role, Instant joinedAt) {

  public static ProjectMemberResponse of(ProjectMember member, User user) {
    boolean active = user.status() == UserStatus.ACTIVE;
    return new ProjectMemberResponse(member.getProjectId(),
        member.getUserId(),
        active ? user.name() : UserDisplayConstants.WITHDRAWN_USER_LABEL,
        active ? user.email() : "",
        member.getRole(), member.getJoinedAt());
  }

  public static ProjectMemberResponse from(ProjectMemberView detail) {
    return of(detail.member(), detail.user());
  }

}
