package com.schemafy.core.erd.ddl.domain;

import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicy;

public interface DdlGenerator {

  DdlExportVendor exportVendor();

  String generate(SchemaExportSnapshot snapshot, DatatypePolicy datatypePolicy);

}
