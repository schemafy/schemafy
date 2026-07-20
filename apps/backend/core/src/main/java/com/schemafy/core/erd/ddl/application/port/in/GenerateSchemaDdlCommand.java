package com.schemafy.core.erd.ddl.application.port.in;

import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.ddl.domain.DdlSchemaSnapshot;
import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;

public record GenerateSchemaDdlCommand(
    DdlSchemaSnapshot snapshot,
    DdlExportVendor targetDbVendor,
    IndexCapabilities indexCapabilities,
    IdentifierCapabilities identifierCapabilities) {
}
