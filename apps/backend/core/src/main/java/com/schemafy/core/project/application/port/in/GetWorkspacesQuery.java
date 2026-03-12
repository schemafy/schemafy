package com.schemafy.core.project.application.port.in;

public record GetWorkspacesQuery(
    String requesterId,
    int page,
    int size) {
}
