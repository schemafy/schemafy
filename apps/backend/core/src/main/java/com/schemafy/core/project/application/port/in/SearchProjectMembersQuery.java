package com.schemafy.core.project.application.port.in;

public record SearchProjectMembersQuery(
    String projectId,
    String requesterId,
    String search,
    int page,
    int size) {
}
