package com.schemafy.core.erd.ddl.application.port.in;

import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;
import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;

public record GenerateSchemaDdlCommand(
    SchemaExportSnapshot snapshot,
    DdlExportVendor targetDbVendor,
    IndexCapabilities indexCapabilities) {
}
