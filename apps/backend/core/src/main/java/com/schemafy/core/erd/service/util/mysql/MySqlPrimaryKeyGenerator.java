package com.schemafy.core.erd.service.util.mysql;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintSnapshotResponse;
import com.schemafy.core.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;

import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.escapeIdentifier;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.quoteColumn;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.requireNonBlank;

@Component
public class MySqlPrimaryKeyGenerator {

  public Optional<String> generate(TableSnapshotResponse table,
      Map<String, String> columnIdToName) {
    requireNonBlank(table.table().name(), "Table name");

    return getConstraints(table).stream()
        .filter(c -> ConstraintKind.PRIMARY_KEY == c.constraint().kind())
        .findFirst()
        .map(pk -> generateAlter(table.table().name(), pk, columnIdToName));
  }

  private String generateAlter(String tableName, ConstraintSnapshotResponse pk,
      Map<String, String> columnIdToName) {
    List<ConstraintColumnResponse> cols = getColumns(pk);
    if (cols.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    String columns = cols.stream()
        .sorted(Comparator.comparing(ConstraintColumnResponse::seqNo,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(cc -> quoteColumn(columnIdToName, cc.columnId()))
        .collect(Collectors.joining(", "));

    return String.format("ALTER TABLE `%s` ADD PRIMARY KEY (%s);",
        escapeIdentifier(tableName), columns);
  }

  public static List<ConstraintSnapshotResponse> getConstraints(TableSnapshotResponse table) {
    return table.constraints() != null
        ? table.constraints()
        : Collections.emptyList();
  }

  public static List<ConstraintColumnResponse> getColumns(ConstraintSnapshotResponse c) {
    return c.columns() != null ? c.columns() : Collections.emptyList();
  }

}
