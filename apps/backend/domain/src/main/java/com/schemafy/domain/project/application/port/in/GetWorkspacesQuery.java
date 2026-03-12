package com.schemafy.domain.project.application.port.in;

public record GetWorkspacesQuery(
    String requesterId,
    int page,
    int size) {
}
