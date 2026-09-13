package com.schemafy.api.erd.controller.dto.response;

public record SchemaDdlExportResponse(
    String schemaId,
    long currentRevision,
    String targetDbVendor,
    String ddl) {
}
