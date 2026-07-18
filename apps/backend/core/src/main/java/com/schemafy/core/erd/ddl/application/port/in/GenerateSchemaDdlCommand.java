package com.schemafy.core.erd.ddl.application.port.in;

import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;

public record GenerateSchemaDdlCommand(
    SchemaExportSnapshot snapshot,
    DdlExportVendor targetDbVendor) {
}
