package com.schemafy.core.erd.ddl.application.port.in;

import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot;

public record GenerateSchemaDdlCommand(
    DdlSchemaSnapshot snapshot,
    DdlExportVendor targetDbVendor) {
}
