package com.schemafy.core.erd.service.util.mysql;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.CommonErrorCode;
import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintSnapshotResponse;
import com.schemafy.core.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;

import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.escapeIdentifier;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.quoteColumn;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.requireNonBlank;

@Component
public class MySqlUniqueKeyGenerator {

  public List<String> generate(TableSnapshotResponse table,
      Map<String, String> columnIdToName) {
    requireNonBlank(table.table().name(), "Table name");

    return getConstraints(table).stream()
        .filter(c -> ConstraintKind.UNIQUE == c.constraint().kind())
        .map(c -> generateAlter(table.table().name(), c, columnIdToName))
        .toList();
  }

  private String generateAlter(String tableName,
      ConstraintSnapshotResponse constraint,
      Map<String, String> columnIdToName) {
    requireNonBlank(constraint.constraint().name(), "Unique constraint name");

    List<ConstraintColumnResponse> cols = getColumns(constraint);
    if (cols.isEmpty()) {
      throw new DomainException(CommonErrorCode.INVALID_INPUT_VALUE);
    }

    String columns = cols.stream()
        .sorted(Comparator.comparing(ConstraintColumnResponse::seqNo,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(cc -> quoteColumn(columnIdToName, cc.columnId()))
        .collect(Collectors.joining(", "));

    return String.format("ALTER TABLE `%s` ADD UNIQUE KEY `%s` (%s);",
        escapeIdentifier(tableName),
        escapeIdentifier(constraint.constraint().name()),
        columns);
  }

  private List<ConstraintSnapshotResponse> getConstraints(
      TableSnapshotResponse table) {
    return table.constraints() != null
        ? table.constraints()
        : Collections.emptyList();
  }

  private List<ConstraintColumnResponse> getColumns(
      ConstraintSnapshotResponse c) {
    return c.columns() != null ? c.columns() : Collections.emptyList();
  }

}
