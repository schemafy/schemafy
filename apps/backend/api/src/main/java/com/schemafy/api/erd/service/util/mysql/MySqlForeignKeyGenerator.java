package com.schemafy.api.erd.service.util.mysql;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.api.erd.controller.dto.response.RelationshipColumnResponse;
import com.schemafy.api.erd.controller.dto.response.RelationshipSnapshotResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;

import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.escapeIdentifier;
import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.quoteColumn;
import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.requireNonBlank;

@Component
public class MySqlForeignKeyGenerator {

  public List<String> generate(TableSnapshotResponse table,
      Map<String, String> tableIdToName,
      Map<String, String> columnIdToName,
      Map<String, Map<String, String>> tableColumnMaps) {
    requireNonBlank(table.table().name(), "Table name");

    return getRelationships(table).stream()
        .filter(r -> table.table().id().equals(r.relationship().fkTableId()))
        .map(r -> generateAlter(table.table().name(), r, tableIdToName,
            columnIdToName, tableColumnMaps))
        .toList();
  }

  private String generateAlter(String tableName,
      RelationshipSnapshotResponse snapshot,
      Map<String, String> tableIdToName,
      Map<String, String> columnIdToName,
      Map<String, Map<String, String>> tableColumnMaps) {
    requireNonBlank(snapshot.relationship().name(), "Foreign key name");

    String pkTableName = tableIdToName.get(snapshot.relationship().pkTableId());
    if (pkTableName == null) {
      throw new DomainException(RelationshipErrorCode.TARGET_TABLE_NOT_FOUND);
    }

    StringBuilder sql = new StringBuilder("ALTER TABLE `");
    sql.append(escapeIdentifier(tableName)).append("` ADD CONSTRAINT `");
    sql.append(escapeIdentifier(snapshot.relationship().name()))
        .append("` FOREIGN KEY (");

    sql.append(buildFkColumnList(snapshot, columnIdToName));
    sql.append(") REFERENCES `");
    sql.append(escapeIdentifier(pkTableName)).append("` (");

    Map<String, String> pkColumnIdToName = tableColumnMaps.getOrDefault(
        snapshot.relationship().pkTableId(), Collections.emptyMap());
    sql.append(buildPkColumnList(snapshot, pkColumnIdToName));
    sql.append(")");

    sql.append(";");

    return sql.toString();
  }

  private String buildFkColumnList(RelationshipSnapshotResponse snapshot,
      Map<String, String> columnIdToName) {
    List<RelationshipColumnResponse> cols = getColumns(snapshot);
    if (cols.isEmpty()) {
      throw new DomainException(CommonErrorCode.INVALID_INPUT_VALUE);
    }

    return cols.stream()
        .sorted(Comparator.comparing(RelationshipColumnResponse::seqNo,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(rc -> quoteColumn(columnIdToName, rc.fkColumnId()))
        .collect(Collectors.joining(", "));
  }

  private String buildPkColumnList(RelationshipSnapshotResponse snapshot,
      Map<String, String> pkColumnIdToName) {
    List<RelationshipColumnResponse> cols = getColumns(snapshot);
    if (cols.isEmpty()) {
      throw new DomainException(CommonErrorCode.INVALID_INPUT_VALUE);
    }

    return cols.stream()
        .sorted(Comparator.comparing(RelationshipColumnResponse::seqNo,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(rc -> quoteColumn(pkColumnIdToName, rc.pkColumnId()))
        .collect(Collectors.joining(", "));
  }

  private List<RelationshipSnapshotResponse> getRelationships(
      TableSnapshotResponse table) {
    return table.relationships() != null
        ? table.relationships()
        : Collections.emptyList();
  }

  private List<RelationshipColumnResponse> getColumns(
      RelationshipSnapshotResponse snapshot) {
    return snapshot.columns() != null
        ? snapshot.columns()
        : Collections.emptyList();
  }

}
