package com.schemafy.core.erd.service.util.mysql;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.core.erd.controller.dto.response.IndexResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipColumnResponse;
import com.schemafy.core.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;

@Component
public class MySqlAlterTableGenerator {

  private static final Set<String> VALID_REFERENTIAL_ACTIONS = Set.of(
      "CASCADE", "SET NULL", "SET DEFAULT", "RESTRICT", "NO ACTION");

  private static final Set<String> VALID_SORT_DIRECTIONS = Set.of("ASC", "DESC");

  private static final Set<String> VALID_INDEX_TYPES = Set.of(
      "BTREE", "HASH", "FULLTEXT", "SPATIAL");

  public String generate(List<TableDetailResponse> tables) {
    if (tables == null || tables.isEmpty()) {
      return "";
    }
    Map<String, String> tableIdToName = buildTableMap(tables);
    Map<String, Map<String, String>> tableColumnMaps = buildAllColumnMaps(
        tables);

    List<String> alterStatements = new ArrayList<>();

    for (TableDetailResponse table : tables) {
      Map<String, String> columnIdToName = tableColumnMaps.get(table.getId());

      generatePrimaryKeyAlter(table, columnIdToName)
          .ifPresent(alterStatements::add);

      table.getConstraints().stream()
          .filter(c -> "UNIQUE".equals(c.getKind()))
          .map(c -> generateUniqueAlter(table.getName(), c, columnIdToName))
          .forEach(alterStatements::add);

      table.getIndexes().stream()
          .map(idx -> generateIndexAlter(table.getName(), idx, columnIdToName))
          .forEach(alterStatements::add);
    }

    for (TableDetailResponse table : tables) {
      Map<String, String> columnIdToName = tableColumnMaps.get(table.getId());

      table.getRelationships().stream()
          .filter(r -> table.getId().equals(r.getFkTableId()))
          .map(r -> generateForeignKeyAlter(table.getName(), r, tableIdToName,
              columnIdToName, tableColumnMaps))
          .forEach(alterStatements::add);
    }

    if (alterStatements.isEmpty()) {
      return "";
    }
    return String.join("\n", alterStatements) + "\n";
  }

  private Optional<String> generatePrimaryKeyAlter(
      TableDetailResponse table,
      Map<String, String> columnIdToName) {
    return table.getConstraints().stream()
        .filter(c -> "PRIMARY_KEY".equals(c.getKind()))
        .findFirst()
        .map(pk -> {
          String columns = pk.getColumns().stream()
              .sorted(Comparator.comparing(ConstraintColumnResponse::getSeqNo))
              .map(cc -> "`" + escapeIdentifier(
                  columnIdToName.get(cc.getColumnId())) + "`")
              .collect(Collectors.joining(", "));

          return String.format("ALTER TABLE `%s` ADD PRIMARY KEY (%s);",
              escapeIdentifier(table.getName()), columns);
        });
  }

  private String generateUniqueAlter(String tableName,
      ConstraintResponse constraint,
      Map<String, String> columnIdToName) {
    String columns = constraint.getColumns().stream()
        .sorted(Comparator.comparing(ConstraintColumnResponse::getSeqNo))
        .map(cc -> "`" + escapeIdentifier(
            columnIdToName.get(cc.getColumnId())) + "`")
        .collect(Collectors.joining(", "));

    return String.format("ALTER TABLE `%s` ADD UNIQUE KEY `%s` (%s);",
        escapeIdentifier(tableName), escapeIdentifier(constraint.getName()),
        columns);
  }

  private String generateIndexAlter(String tableName, IndexResponse index,
      Map<String, String> columnIdToName) {
    StringBuilder idx = new StringBuilder("ALTER TABLE `");
    idx.append(escapeIdentifier(tableName)).append("` ADD ");

    String type = sanitizeIndexType(index.getType());
    if ("FULLTEXT".equals(type)) {
      idx.append("FULLTEXT ");
    } else if ("SPATIAL".equals(type)) {
      idx.append("SPATIAL ");
    }

    idx.append("INDEX `").append(escapeIdentifier(index.getName()))
        .append("` (");

    String columns = index.getColumns().stream()
        .sorted(Comparator.comparing(IndexColumnResponse::getSeqNo))
        .map(ic -> {
          String col = "`" + escapeIdentifier(
              columnIdToName.get(ic.getColumnId())) + "`";
          String sortDir = sanitizeSortDirection(ic.getSortDir());
          if (sortDir != null) {
            col += " " + sortDir;
          }
          return col;
        })
        .collect(Collectors.joining(", "));

    idx.append(columns).append(")");

    if ("BTREE".equals(type) || "HASH".equals(type)) {
      idx.append(" USING ").append(type);
    }

    idx.append(";");

    return idx.toString();
  }

  private String generateForeignKeyAlter(String tableName,
      RelationshipResponse relationship,
      Map<String, String> tableIdToName,
      Map<String, String> columnIdToName,
      Map<String, Map<String, String>> tableColumnMaps) {
    StringBuilder fk = new StringBuilder("ALTER TABLE `");
    fk.append(escapeIdentifier(tableName)).append("` ADD CONSTRAINT `");
    fk.append(escapeIdentifier(relationship.getName()))
        .append("` FOREIGN KEY (");

    String fkColumns = relationship.getColumns().stream()
        .sorted(Comparator.comparing(RelationshipColumnResponse::getSeqNo))
        .map(rc -> "`" + escapeIdentifier(
            columnIdToName.get(rc.getFkColumnId())) + "`")
        .collect(Collectors.joining(", "));

    fk.append(fkColumns).append(") REFERENCES `");

    String pkTableName = tableIdToName.get(relationship.getPkTableId());
    fk.append(escapeIdentifier(pkTableName)).append("` (");

    Map<String, String> pkColumnIdToName = tableColumnMaps
        .get(relationship.getPkTableId());

    String pkColumns = relationship.getColumns().stream()
        .sorted(Comparator.comparing(RelationshipColumnResponse::getSeqNo))
        .map(rc -> "`" + escapeIdentifier(
            pkColumnIdToName.get(rc.getPkColumnId())) + "`")
        .collect(Collectors.joining(", "));

    fk.append(pkColumns).append(")");

    String onDelete = sanitizeReferentialAction(relationship.getOnDelete());
    if (onDelete != null) {
      fk.append(" ON DELETE ").append(onDelete);
    }
    String onUpdate = sanitizeReferentialAction(relationship.getOnUpdate());
    if (onUpdate != null) {
      fk.append(" ON UPDATE ").append(onUpdate);
    }

    fk.append(";");

    return fk.toString();
  }

  private Map<String, String> buildTableMap(
      List<TableDetailResponse> tables) {
    return tables.stream()
        .collect(Collectors.toMap(
            TableDetailResponse::getId,
            TableDetailResponse::getName));
  }

  private Map<String, Map<String, String>> buildAllColumnMaps(
      List<TableDetailResponse> tables) {
    return tables.stream()
        .collect(Collectors.toMap(
            TableDetailResponse::getId,
            table -> table.getColumns().stream()
                .collect(Collectors.toMap(
                    ColumnResponse::getId,
                    ColumnResponse::getName))));
  }

  private String escapeIdentifier(String identifier) {
    if (identifier == null) {
      return "";
    }
    return identifier.replace("`", "``");
  }

  private String sanitizeIndexType(String indexType) {
    if (indexType == null || indexType.isEmpty()) {
      return null;
    }
    String normalized = indexType.toUpperCase().trim();
    if (!VALID_INDEX_TYPES.contains(normalized)) {
      throw new IllegalArgumentException("Invalid index type: " + indexType);
    }
    return normalized;
  }

  private String sanitizeSortDirection(String sortDir) {
    if (sortDir == null || sortDir.isEmpty()) {
      return null;
    }
    String normalized = sortDir.toUpperCase().trim();
    if (!VALID_SORT_DIRECTIONS.contains(normalized)) {
      throw new IllegalArgumentException("Invalid sort direction: " + sortDir);
    }
    return normalized;
  }

  private String sanitizeReferentialAction(String action) {
    if (action == null || action.isEmpty()) {
      return null;
    }
    String normalized = action.toUpperCase().trim();
    if (!VALID_REFERENTIAL_ACTIONS.contains(normalized)) {
      throw new IllegalArgumentException(
          "Invalid referential action: " + action);
    }
    return normalized;
  }

}
