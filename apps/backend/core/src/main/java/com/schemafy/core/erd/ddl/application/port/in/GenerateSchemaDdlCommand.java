package com.schemafy.core.erd.ddl.application.port.in;

import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot;
import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;

public record GenerateSchemaDdlCommand(
    DdlSchemaSnapshot snapshot,
    DdlExportVendor targetDbVendor,
    IndexCapabilities indexCapabilities) {
}
