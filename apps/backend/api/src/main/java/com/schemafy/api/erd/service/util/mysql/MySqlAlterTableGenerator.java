package com.schemafy.api.erd.service.util.mysql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.api.erd.controller.dto.response.ColumnResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;

import lombok.RequiredArgsConstructor;

import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.normalizeTables;

@Component
@RequiredArgsConstructor
public class MySqlAlterTableGenerator {

  private final MySqlPrimaryKeyGenerator primaryKeyGenerator;
  private final MySqlUniqueKeyGenerator uniqueKeyGenerator;
  private final MySqlIndexGenerator indexGenerator;
  private final MySqlForeignKeyGenerator foreignKeyGenerator;

  public String generate(List<TableSnapshotResponse> tables) {
    if (tables == null || tables.isEmpty()) {
      return "";
    }

    List<TableSnapshotResponse> orderedTables = normalizeTables(tables);

    if (orderedTables.isEmpty()) {
      return "";
    }

    Map<String, String> tableIdToName = buildTableMap(orderedTables);
    Map<String, Map<String, String>> tableColumnMaps = buildAllColumnMaps(
        orderedTables);

    List<String> alterStatements = new ArrayList<>();

    for (TableSnapshotResponse table : orderedTables) {
      Map<String, String> columnIdToName = tableColumnMaps.getOrDefault(
          table.table().id(), Collections.emptyMap());

      primaryKeyGenerator.generate(table, columnIdToName)
          .ifPresent(alterStatements::add);

      alterStatements.addAll(
          uniqueKeyGenerator.generate(table, columnIdToName));

      alterStatements.addAll(
          indexGenerator.generate(table, columnIdToName));
    }

    for (TableSnapshotResponse table : orderedTables) {
      Map<String, String> columnIdToName = tableColumnMaps.getOrDefault(
          table.table().id(), Collections.emptyMap());

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
      List<TableSnapshotResponse> tables) {
    return tables.stream()
        .filter(t -> t != null && t.table() != null
            && t.table().id() != null && t.table().name() != null)
        .collect(Collectors.toMap(
            t -> t.table().id(),
            t -> t.table().name(),
            (existing, replacement) -> existing));
  }

  private Map<String, Map<String, String>> buildAllColumnMaps(
      List<TableSnapshotResponse> tables) {
    return tables.stream()
        .filter(t -> t != null && t.table() != null && t.table().id() != null)
        .collect(Collectors.toMap(
            t -> t.table().id(),
            this::buildColumnMap,
            (existing, replacement) -> existing));
  }

  private Map<String, String> buildColumnMap(TableSnapshotResponse table) {
    List<ColumnResponse> columns = table.columns() != null
        ? table.columns()
        : Collections.emptyList();

    return columns.stream()
        .filter(c -> c != null && c.id() != null && c.name() != null)
        .collect(Collectors.toMap(
            ColumnResponse::id,
            ColumnResponse::name,
            (existing, replacement) -> existing));
  }

}
