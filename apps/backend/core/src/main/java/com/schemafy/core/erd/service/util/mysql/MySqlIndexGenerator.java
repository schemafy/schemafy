package com.schemafy.core.erd.service.util.mysql;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.core.erd.controller.dto.response.IndexResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;

import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.escapeIdentifier;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.requireNonBlank;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.sanitizeIndexType;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.sanitizeSortDirection;

@Component
public class MySqlIndexGenerator {

  public List<String> generate(TableDetailResponse table,
      Map<String, String> columnIdToName) {
    requireNonBlank(table.getName(), "Table name");

    return getIndexes(table).stream()
        .map(idx -> generateAlter(table.getName(), idx, columnIdToName))
        .toList();
  }

  private String generateAlter(String tableName, IndexResponse index,
      Map<String, String> columnIdToName) {
    requireNonBlank(index.getName(), "Index name");

    StringBuilder sql = new StringBuilder("ALTER TABLE `");
    sql.append(escapeIdentifier(tableName)).append("` ADD ");

    String type = sanitizeIndexType(index.getType()).orElse("BTREE");

    appendIndexPrefix(sql, type);
    sql.append("INDEX `").append(escapeIdentifier(index.getName()))
        .append("` (");
    sql.append(buildColumnList(index, columnIdToName));
    sql.append(")");
    appendUsing(sql, type);
    sql.append(";");

    return sql.toString();
  }

  private void appendIndexPrefix(StringBuilder sql, String type) {
    if ("FULLTEXT".equals(type)) {
      sql.append("FULLTEXT ");
    } else if ("SPATIAL".equals(type)) {
      sql.append("SPATIAL ");
    }
  }

  private void appendUsing(StringBuilder sql, String type) {
    if ("BTREE".equals(type) || "HASH".equals(type)) {
      sql.append(" USING ").append(type);
    }
  }

  private String buildColumnList(IndexResponse index,
      Map<String, String> columnIdToName) {
    List<IndexColumnResponse> cols = getColumns(index);
    if (cols.isEmpty()) {
      throw new IllegalArgumentException(
          "Index must have at least one column: " + index.getName());
    }

    return cols.stream()
        .sorted(Comparator.comparing(IndexColumnResponse::getSeqNo,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(ic -> formatColumn(ic, columnIdToName))
        .collect(Collectors.joining(", "));
  }

  private String formatColumn(IndexColumnResponse ic,
      Map<String, String> columnIdToName) {
    String col = quoteColumn(columnIdToName, ic.getColumnId());
    return sanitizeSortDirection(ic.getSortDir())
        .map(dir -> col + " " + dir)
        .orElse(col);
  }

  private String quoteColumn(Map<String, String> columnIdToName,
      String columnId) {
    String name = columnIdToName.get(columnId);
    if (name == null) {
      throw new IllegalStateException("Column not found: " + columnId);
    }
    return "`" + escapeIdentifier(name) + "`";
  }

  private List<IndexResponse> getIndexes(TableDetailResponse table) {
    return table.getIndexes() != null
        ? table.getIndexes()
        : Collections.emptyList();
  }

  private List<IndexColumnResponse> getColumns(IndexResponse index) {
    return index.getColumns() != null
        ? index.getColumns()
        : Collections.emptyList();
  }

}
