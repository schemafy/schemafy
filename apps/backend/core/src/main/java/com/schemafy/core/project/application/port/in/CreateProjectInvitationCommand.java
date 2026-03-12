package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.ProjectRole;

public record CreateProjectInvitationCommand(
    String projectId,
    String email,
    ProjectRole role,
    String requesterId) {
}
