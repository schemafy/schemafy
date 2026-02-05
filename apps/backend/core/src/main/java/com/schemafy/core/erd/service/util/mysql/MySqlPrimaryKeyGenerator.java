package com.schemafy.core.erd.service.util.mysql;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;

import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.escapeIdentifier;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.requireNonBlank;

@Component
public class MySqlPrimaryKeyGenerator {

  public Optional<String> generate(TableDetailResponse table,
      Map<String, String> columnIdToName) {
    requireNonBlank(table.getName(), "Table name");

    return getConstraints(table).stream()
        .filter(c -> "PRIMARY_KEY".equals(c.getKind()))
        .findFirst()
        .map(pk -> generateAlter(table.getName(), pk, columnIdToName));
  }

  private String generateAlter(String tableName, ConstraintResponse pk,
      Map<String, String> columnIdToName) {
    List<ConstraintColumnResponse> cols = getColumns(pk);
    if (cols.isEmpty()) {
      throw new IllegalArgumentException("Primary key must have at least one column");
    }

    String columns = cols.stream()
        .sorted(Comparator.comparing(ConstraintColumnResponse::getSeqNo,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(cc -> quoteColumn(columnIdToName, cc.getColumnId()))
        .collect(Collectors.joining(", "));

    return String.format("ALTER TABLE `%s` ADD PRIMARY KEY (%s);",
        escapeIdentifier(tableName), columns);
  }

  private String quoteColumn(Map<String, String> columnIdToName,
      String columnId) {
    String name = columnIdToName.get(columnId);
    if (name == null) {
      throw new IllegalStateException("Column not found: " + columnId);
    }
    return "`" + escapeIdentifier(name) + "`";
  }

  public static List<ConstraintResponse> getConstraints(TableDetailResponse table) {
    return table.getConstraints() != null
        ? table.getConstraints()
        : Collections.emptyList();
  }

  public static List<ConstraintColumnResponse> getColumns(ConstraintResponse c) {
    return c.getColumns() != null ? c.getColumns() : Collections.emptyList();
  }

}
