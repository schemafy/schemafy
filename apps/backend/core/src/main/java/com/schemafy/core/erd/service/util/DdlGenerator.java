package com.schemafy.core.erd.service.util;

import java.util.List;

import com.schemafy.core.erd.controller.dto.response.SchemaDetailResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;

public interface DdlGenerator {

  String generateSchemaDdl(SchemaDetailResponse schema,
      List<TableDetailResponse> tables);

}
