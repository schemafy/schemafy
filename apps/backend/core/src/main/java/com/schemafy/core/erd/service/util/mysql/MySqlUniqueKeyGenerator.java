package com.schemafy.core.erd.service.util.mysql;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;

import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.escapeIdentifier;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.quoteColumn;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.requireNonBlank;

@Component
public class MySqlUniqueKeyGenerator {

  public List<String> generate(TableDetailResponse table,
      Map<String, String> columnIdToName) {
    requireNonBlank(table.getName(), "Table name");

    return getConstraints(table).stream()
        .filter(c -> "UNIQUE".equals(c.getKind()))
        .map(c -> generateAlter(table.getName(), c, columnIdToName))
        .toList();
  }

  private String generateAlter(String tableName, ConstraintResponse constraint,
      Map<String, String> columnIdToName) {
    requireNonBlank(constraint.getName(), "Unique constraint name");

    List<ConstraintColumnResponse> cols = getColumns(constraint);
    if (cols.isEmpty()) {
      throw new IllegalArgumentException(
          "Unique constraint must have at least one column: " + constraint.getName());
    }

    String columns = cols.stream()
        .sorted(Comparator.comparing(ConstraintColumnResponse::getSeqNo,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(cc -> quoteColumn(columnIdToName, cc.getColumnId()))
        .collect(Collectors.joining(", "));

    return String.format("ALTER TABLE `%s` ADD UNIQUE KEY `%s` (%s);",
        escapeIdentifier(tableName),
        escapeIdentifier(constraint.getName()),
        columns);
  }

  private List<ConstraintResponse> getConstraints(TableDetailResponse table) {
    return table.getConstraints() != null
        ? table.getConstraints()
        : Collections.emptyList();
  }

  private List<ConstraintColumnResponse> getColumns(ConstraintResponse c) {
    return c.getColumns() != null ? c.getColumns() : Collections.emptyList();
  }

}
