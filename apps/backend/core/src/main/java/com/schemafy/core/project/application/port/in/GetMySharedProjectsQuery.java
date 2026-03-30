package com.schemafy.core.project.application.port.in;

public record GetMySharedProjectsQuery(
    String requesterId,
    int page,
    int size) {
}
