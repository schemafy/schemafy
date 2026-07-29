package com.schemafy.core.erd.mermaid.application.port.in;

import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;

public record GenerateSchemaMermaidCommand(
    SchemaExportSnapshot snapshot) {
}
