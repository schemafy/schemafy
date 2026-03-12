package com.schemafy.api.erd.service.util;

import java.util.List;

import com.schemafy.api.erd.controller.dto.response.SchemaResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;

public interface DdlGenerator {

  String generateSchemaDdl(SchemaResponse schema,
      List<TableSnapshotResponse> tables);

}
