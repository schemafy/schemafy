package com.schemafy.domain.project.application.port.in;

public record RemoveProjectMemberCommand(
    String projectId,
    String targetUserId,
    String requesterId) {
}
