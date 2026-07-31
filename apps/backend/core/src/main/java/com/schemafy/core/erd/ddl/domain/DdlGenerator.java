package com.schemafy.core.erd.ddl.domain;

import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;

public interface DdlGenerator {

  DdlExportVendor exportVendor();

  String generate(SchemaExportSnapshot snapshot);

}
