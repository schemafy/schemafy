package com.schemafy.core.project.application.port.in;

public record CreateProjectCommand(
    String workspaceId,
    Integer dbVendorId,
    String name,
    String description,
    String requesterId) {
}
