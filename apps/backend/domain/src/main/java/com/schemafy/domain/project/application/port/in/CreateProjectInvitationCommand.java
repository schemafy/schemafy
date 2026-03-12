package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.ProjectRole;

public record CreateProjectInvitationCommand(
    String projectId,
    String email,
    ProjectRole role,
    String requesterId) {
}
