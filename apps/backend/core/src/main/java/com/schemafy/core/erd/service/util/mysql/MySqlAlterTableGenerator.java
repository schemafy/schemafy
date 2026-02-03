package com.schemafy.core.erd.service.util.mysql;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
              .map(cc -> "`" + columnIdToName.get(cc.getColumnId()) + "`")
              .collect(Collectors.joining(", "));

          return String.format("ALTER TABLE `%s` ADD PRIMARY KEY (%s);",
              table.getName(), columns);
        });
  }

  private String generateUniqueAlter(String tableName,
      ConstraintResponse constraint,
      Map<String, String> columnIdToName) {
    String columns = constraint.getColumns().stream()
        .sorted(Comparator.comparing(ConstraintColumnResponse::getSeqNo))
        .map(cc -> "`" + columnIdToName.get(cc.getColumnId()) + "`")
        .collect(Collectors.joining(", "));

    return String.format("ALTER TABLE `%s` ADD UNIQUE KEY `%s` (%s);",
        tableName, constraint.getName(), columns);
  }

  private String generateIndexAlter(String tableName, IndexResponse index,
      Map<String, String> columnIdToName) {
    StringBuilder idx = new StringBuilder("ALTER TABLE `");
    idx.append(tableName).append("` ADD ");

    String type = index.getType();
    if ("FULLTEXT".equals(type)) {
      idx.append("FULLTEXT ");
    } else if ("SPATIAL".equals(type)) {
      idx.append("SPATIAL ");
    }

    idx.append("INDEX `").append(index.getName()).append("` (");

    String columns = index.getColumns().stream()
        .sorted(Comparator.comparing(IndexColumnResponse::getSeqNo))
        .map(ic -> {
          String col = "`" + columnIdToName.get(ic.getColumnId()) + "`";
          if (ic.getSortDir() != null && !ic.getSortDir().isEmpty()) {
            col += " " + ic.getSortDir();
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
    fk.append(tableName).append("` ADD CONSTRAINT `");
    fk.append(relationship.getName()).append("` FOREIGN KEY (");

    String fkColumns = relationship.getColumns().stream()
        .sorted(Comparator.comparing(RelationshipColumnResponse::getSeqNo))
        .map(rc -> "`" + columnIdToName.get(rc.getFkColumnId()) + "`")
        .collect(Collectors.joining(", "));

    fk.append(fkColumns).append(") REFERENCES `");

    String pkTableName = tableIdToName.get(relationship.getPkTableId());
    fk.append(pkTableName).append("` (");

    Map<String, String> pkColumnIdToName = tableColumnMaps
        .get(relationship.getPkTableId());

    String pkColumns = relationship.getColumns().stream()
        .sorted(Comparator.comparing(RelationshipColumnResponse::getSeqNo))
        .map(rc -> "`" + pkColumnIdToName.get(rc.getPkColumnId()) + "`")
        .collect(Collectors.joining(", "));

    fk.append(pkColumns).append(")");

    if (relationship.getOnDelete() != null
        && !relationship.getOnDelete().isEmpty()) {
      fk.append(" ON DELETE ").append(relationship.getOnDelete());
    }
    if (relationship.getOnUpdate() != null
        && !relationship.getOnUpdate().isEmpty()) {
      fk.append(" ON UPDATE ").append(relationship.getOnUpdate());
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

}
