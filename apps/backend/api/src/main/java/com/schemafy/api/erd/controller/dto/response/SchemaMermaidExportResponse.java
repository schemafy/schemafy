package com.schemafy.api.erd.controller.dto.response;

public record SchemaMermaidExportResponse(
    String schemaId,
    long currentRevision,
    String mermaid) {
}
