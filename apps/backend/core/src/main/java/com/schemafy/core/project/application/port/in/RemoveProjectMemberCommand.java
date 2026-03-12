package com.schemafy.core.project.application.port.in;

public record RemoveProjectMemberCommand(
    String projectId,
    String targetUserId,
    String requesterId) {
}
