package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.ProjectRole;

public record UpdateProjectMemberRoleCommand(
    String projectId,
    String targetUserId,
    ProjectRole role,
    String requesterId) {
}
