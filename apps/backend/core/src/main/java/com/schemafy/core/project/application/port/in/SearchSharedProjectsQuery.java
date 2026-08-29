package com.schemafy.core.project.application.port.in;

public record SearchSharedProjectsQuery(
    String requesterId,
    String search,
    int page,
    int size) {
}
