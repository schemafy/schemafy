package com.schemafy.core.erd.ddl.domain;

public interface DdlGenerator {

  DdlExportVendor exportVendor();

  String generate(DdlSchemaSnapshot snapshot);

}
