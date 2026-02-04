package com.schemafy.core.erd.service.util.mysql;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.controller.dto.response.RelationshipColumnResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;

import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.escapeIdentifier;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.requireNonBlank;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.sanitizeReferentialAction;

@Component
public class MySqlForeignKeyGenerator {

  public List<String> generate(TableDetailResponse table,
      Map<String, String> tableIdToName,
      Map<String, String> columnIdToName,
      Map<String, Map<String, String>> tableColumnMaps) {
    requireNonBlank(table.getName(), "Table name");

    return getRelationships(table).stream()
        .filter(r -> table.getId().equals(r.getFkTableId()))
        .map(r -> generateAlter(table.getName(), r, tableIdToName,
            columnIdToName, tableColumnMaps))
        .toList();
  }

  private String generateAlter(String tableName,
      RelationshipResponse relationship,
      Map<String, String> tableIdToName,
      Map<String, String> columnIdToName,
      Map<String, Map<String, String>> tableColumnMaps) {
    requireNonBlank(relationship.getName(), "Foreign key name");

    String pkTableName = tableIdToName.get(relationship.getPkTableId());
    if (pkTableName == null) {
      throw new IllegalStateException(
          "Referenced table not found: " + relationship.getPkTableId());
    }

    StringBuilder sql = new StringBuilder("ALTER TABLE `");
    sql.append(escapeIdentifier(tableName)).append("` ADD CONSTRAINT `");
    sql.append(escapeIdentifier(relationship.getName()))
        .append("` FOREIGN KEY (");

    sql.append(buildFkColumnList(relationship, columnIdToName));
    sql.append(") REFERENCES `");
    sql.append(escapeIdentifier(pkTableName)).append("` (");

    Map<String, String> pkColumnIdToName = tableColumnMaps.getOrDefault(
        relationship.getPkTableId(), Collections.emptyMap());
    sql.append(buildPkColumnList(relationship, pkColumnIdToName));
    sql.append(")");

    appendReferentialActions(sql, relationship);
    sql.append(";");

    return sql.toString();
  }

  private String buildFkColumnList(RelationshipResponse relationship,
      Map<String, String> columnIdToName) {
    List<RelationshipColumnResponse> cols = getColumns(relationship);
    if (cols.isEmpty()) {
      throw new IllegalArgumentException(
          "Foreign key must have at least one column: " + relationship.getName());
    }

    return cols.stream()
        .sorted(Comparator.comparing(RelationshipColumnResponse::getSeqNo,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(rc -> quoteColumn(columnIdToName, rc.getFkColumnId()))
        .collect(Collectors.joining(", "));
  }

  private String buildPkColumnList(RelationshipResponse relationship,
      Map<String, String> pkColumnIdToName) {
    return getColumns(relationship).stream()
        .sorted(Comparator.comparing(RelationshipColumnResponse::getSeqNo,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(rc -> quoteColumn(pkColumnIdToName, rc.getPkColumnId()))
        .collect(Collectors.joining(", "));
  }

  private void appendReferentialActions(StringBuilder sql,
      RelationshipResponse relationship) {
    sanitizeReferentialAction(relationship.getOnDelete())
        .ifPresent(action -> sql.append(" ON DELETE ").append(action));

    sanitizeReferentialAction(relationship.getOnUpdate())
        .ifPresent(action -> sql.append(" ON UPDATE ").append(action));
  }

  private String quoteColumn(Map<String, String> columnIdToName,
      String columnId) {
    String name = columnIdToName.get(columnId);
    if (name == null) {
      throw new IllegalStateException("Column not found: " + columnId);
    }
    return "`" + escapeIdentifier(name) + "`";
  }

  private List<RelationshipResponse> getRelationships(
      TableDetailResponse table) {
    return table.getRelationships() != null
        ? table.getRelationships()
        : Collections.emptyList();
  }

  private List<RelationshipColumnResponse> getColumns(
      RelationshipResponse relationship) {
    return relationship.getColumns() != null
        ? relationship.getColumns()
        : Collections.emptyList();
  }

}
