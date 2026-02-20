package com.schemafy.core.erd.service.util;

import java.util.List;

import com.schemafy.core.erd.controller.dto.response.SchemaResponse;
import com.schemafy.core.erd.controller.dto.response.TableResponse;

public interface DdlGenerator {

  String generateSchemaDdl(SchemaResponse schema,
      List<TableResponse> tables);

}
