package com.schemafy.core.project.application.port.in;

public record GetWorkspaceQuery(
    String workspaceId,
    String requesterId) {
}
