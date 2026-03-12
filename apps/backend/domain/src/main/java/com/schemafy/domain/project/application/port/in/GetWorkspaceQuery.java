package com.schemafy.domain.project.application.port.in;

public record GetWorkspaceQuery(
    String workspaceId,
    String requesterId) {
}
