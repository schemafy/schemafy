package com.schemafy.domain.project.application.port.in;

public record GetShareLinksQuery(
    String projectId,
    String requesterId,
    int page,
    int size) {
}
