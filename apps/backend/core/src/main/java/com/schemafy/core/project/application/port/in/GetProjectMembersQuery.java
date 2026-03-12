package com.schemafy.core.project.application.port.in;

public record GetProjectMembersQuery(
    String projectId,
    String requesterId,
    int page,
    int size) {
}
