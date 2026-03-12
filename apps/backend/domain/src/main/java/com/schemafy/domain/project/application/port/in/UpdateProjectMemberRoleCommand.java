package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.ProjectRole;

public record UpdateProjectMemberRoleCommand(
    String projectId,
    String targetUserId,
    ProjectRole role,
    String requesterId) {
}
