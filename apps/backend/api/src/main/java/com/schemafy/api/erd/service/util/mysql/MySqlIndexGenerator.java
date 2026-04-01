package com.schemafy.api.erd.service.util.mysql;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.api.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.api.erd.controller.dto.response.IndexSnapshotResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.domain.type.IndexType;

import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.comparingNullableStrings;
import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.escapeIdentifier;
import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.quoteColumn;
import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.requireNonBlank;

@Component
public class MySqlIndexGenerator {

  public List<String> generate(TableSnapshotResponse table,
      Map<String, String> columnIdToName) {
    requireNonBlank(table.table().name(), "Table name");

    return getIndexes(table).stream()
        .sorted(comparingNullableStrings(
            idx -> idx.index() != null ? idx.index().name() : null,
            idx -> idx.index() != null ? idx.index().id() : null))
        .map(idx -> generateAlter(table.table().name(), idx, columnIdToName))
        .toList();
  }

  private String generateAlter(String tableName, IndexSnapshotResponse snapshot,
      Map<String, String> columnIdToName) {
    requireNonBlank(snapshot.index().name(), "Index name");

    StringBuilder sql = new StringBuilder("ALTER TABLE `");
    sql.append(escapeIdentifier(tableName)).append("` ADD ");

    IndexType type = snapshot.index().type();
    String typeName = type != null ? type.name() : "BTREE";

    appendIndexPrefix(sql, typeName);
    sql.append("INDEX `").append(escapeIdentifier(snapshot.index().name()))
        .append("` (");
    sql.append(buildColumnList(snapshot, columnIdToName));
    sql.append(")");
    appendUsing(sql, typeName);
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

  private String buildColumnList(IndexSnapshotResponse snapshot,
      Map<String, String> columnIdToName) {
    List<IndexColumnResponse> cols = getColumns(snapshot);
    if (cols.isEmpty()) {
      throw new DomainException(CommonErrorCode.INVALID_INPUT_VALUE);
    }

    return cols.stream()
        .sorted(Comparator.comparingInt(IndexColumnResponse::seqNo))
        .map(ic -> formatColumn(ic, columnIdToName))
        .collect(Collectors.joining(", "));
  }

  private String formatColumn(IndexColumnResponse ic,
      Map<String, String> columnIdToName) {
    String col = quoteColumn(columnIdToName, ic.columnId());
    if (ic.sortDirection() != null) {
      return col + " " + ic.sortDirection().name();
    }
    return col;
  }

  private List<IndexSnapshotResponse> getIndexes(TableSnapshotResponse table) {
    return table.indexes() != null
        ? table.indexes()
        : Collections.emptyList();
  }

  private List<IndexColumnResponse> getColumns(IndexSnapshotResponse snapshot) {
    return snapshot.columns() != null
        ? snapshot.columns()
        : Collections.emptyList();
  }

}
