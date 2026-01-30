package com.schemafy.core.erd.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.core.erd.controller.dto.response.ExportResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;
import com.schemafy.core.erd.controller.dto.response.TableResponse;
import com.schemafy.core.erd.service.util.mysql.MySqlDdlGenerator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ExportService {

  private final SchemaService schemaService;
  private final TableService tableService;
  private final MySqlDdlGenerator ddlGenerator;

  public Mono<ExportResponse> exportSchemaToMySqlDdl(String schemaId) {
    return schemaService.getSchema(schemaId)
        .flatMap(schema -> fetchAllTableDetails(schema.getTables())
            .collectList()
            .map(tableDetails -> {
              String ddl = ddlGenerator.generateSchemaDdl(schema, tableDetails);
              return ExportResponse.of(schema.getName(), ddl,
                  tableDetails.size());
            }));
  }

  private Flux<TableDetailResponse> fetchAllTableDetails(
      List<TableResponse> tables) {
    return Flux.fromIterable(tables)
        .flatMap(table -> tableService.getTable(table.getId()));
  }

}
