package com.schemafy.core.project.application.port.in;

public record GetShareLinksQuery(
    String projectId,
    String requesterId,
    int page,
    int size) {
}
