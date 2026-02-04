package com.schemafy.core.erd.service.util.mysql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MySqlAlterTableGenerator {

  private final MySqlPrimaryKeyGenerator primaryKeyGenerator;
  private final MySqlUniqueKeyGenerator uniqueKeyGenerator;
  private final MySqlIndexGenerator indexGenerator;
  private final MySqlForeignKeyGenerator foreignKeyGenerator;

  public String generate(List<TableDetailResponse> tables) {
    if (tables == null || tables.isEmpty()) {
      return "";
    }

    Map<String, String> tableIdToName = buildTableMap(tables);
    Map<String, Map<String, String>> tableColumnMaps = buildAllColumnMaps(
        tables);

    List<String> alterStatements = new ArrayList<>();

    for (TableDetailResponse table : tables) {
      if (table == null) {
        continue;
      }
      Map<String, String> columnIdToName = tableColumnMaps.getOrDefault(
          table.getId(), Collections.emptyMap());

      primaryKeyGenerator.generate(table, columnIdToName)
          .ifPresent(alterStatements::add);

      alterStatements.addAll(
          uniqueKeyGenerator.generate(table, columnIdToName));

      alterStatements.addAll(
          indexGenerator.generate(table, columnIdToName));
    }

    for (TableDetailResponse table : tables) {
      if (table == null) {
        continue;
      }
      Map<String, String> columnIdToName = tableColumnMaps.getOrDefault(
          table.getId(), Collections.emptyMap());

      alterStatements.addAll(
          foreignKeyGenerator.generate(table, tableIdToName, columnIdToName,
              tableColumnMaps));
    }

    if (alterStatements.isEmpty()) {
      return "";
    }
    return String.join("\n", alterStatements) + "\n";
  }

  private Map<String, String> buildTableMap(
      List<TableDetailResponse> tables) {
    return tables.stream()
        .filter(t -> t != null && t.getId() != null && t.getName() != null)
        .collect(Collectors.toMap(
            TableDetailResponse::getId,
            TableDetailResponse::getName,
            (existing, replacement) -> existing));
  }

  private Map<String, Map<String, String>> buildAllColumnMaps(
      List<TableDetailResponse> tables) {
    return tables.stream()
        .filter(t -> t != null && t.getId() != null)
        .collect(Collectors.toMap(
            TableDetailResponse::getId,
            this::buildColumnMap,
            (existing, replacement) -> existing));
  }

  private Map<String, String> buildColumnMap(TableDetailResponse table) {
    List<ColumnResponse> columns = table.getColumns() != null
        ? table.getColumns()
        : Collections.emptyList();

    return columns.stream()
        .filter(c -> c != null && c.getId() != null && c.getName() != null)
        .collect(Collectors.toMap(
            ColumnResponse::getId,
            ColumnResponse::getName,
            (existing, replacement) -> existing));
  }

}
