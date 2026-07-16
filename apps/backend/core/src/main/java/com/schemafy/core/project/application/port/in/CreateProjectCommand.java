package com.schemafy.core.project.application.port.in;

public record CreateProjectCommand(
    String workspaceId,
    String dbVendorId,
    String name,
    String description,
    String requesterId) {
}
