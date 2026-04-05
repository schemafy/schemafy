package com.schemafy.api.erd.service.util.mysql;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.api.erd.controller.dto.response.ColumnResponse;
import com.schemafy.api.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.api.erd.controller.dto.response.ConstraintSnapshotResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;

import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.escapeIdentifier;
import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.escapeString;
import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.quoteColumn;
import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.requireNonBlank;
import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.sanitizeCharset;
import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.sanitizeCollation;
import static com.schemafy.api.erd.service.util.mysql.MySqlDdlUtils.sanitizeDataType;

@Component
public class MySqlCreateTableGenerator {

  public String generate(TableSnapshotResponse table) {
    if (table == null || table.table() == null) {
      throw new DomainException(CommonErrorCode.INVALID_INPUT_VALUE);
    }

    requireNonBlank(table.table().name(), "Table name");

    List<ColumnResponse> columns = table.columns() != null
        ? table.columns().stream().filter(c -> c != null).toList()
        : Collections.emptyList();

    if (columns.isEmpty()) {
      throw new DomainException(CommonErrorCode.INVALID_INPUT_VALUE);
    }

    StringBuilder ddl = new StringBuilder();
    ddl.append("CREATE TABLE `").append(escapeIdentifier(table.table().name()))
        .append("` (\n");

    Map<String, String> columnIdToName = columns.stream()
        .filter(c -> c.id() != null && c.name() != null)
        .collect(Collectors.toMap(ColumnResponse::id, ColumnResponse::name,
            (existing, replacement) -> existing));

    Set<String> notNullColumnIds = collectNotNullColumnIds(table);

    List<String> columnDefs = columns.stream()
        .sorted(Comparator.comparingInt(ColumnResponse::seqNo))
        .map(col -> generateColumnDefinition(col, notNullColumnIds))
        .toList();

    ddl.append(String.join(",\n", columnDefs));

    generatePrimaryKeyClause(table, columnIdToName)
        .ifPresent(pk -> ddl.append(",\n").append(pk));

    ddl.append("\n)");
    ddl.append(generateTableOptions(table));
    ddl.append(";");

    return ddl.toString();
  }

  private String generateColumnDefinition(ColumnResponse column,
      Set<String> notNullColumnIds) {
    requireNonBlank(column.name(), "Column name");

    StringBuilder col = new StringBuilder();
    col.append("  `").append(escapeIdentifier(column.name())).append("` ");
    col.append(generateDataType(column));

    sanitizeCharset(column.charset())
        .ifPresent(cs -> col.append(" CHARACTER SET ").append(cs));

    sanitizeCollation(column.collation())
        .ifPresent(coll -> col.append(" COLLATE ").append(coll));

    if (notNullColumnIds.contains(column.id())) {
      col.append(" NOT NULL");
    }

    if (column.autoIncrement()) {
      col.append(" AUTO_INCREMENT");
    }

    if (column.comment() != null && !column.comment().isEmpty()) {
      col.append(" COMMENT '").append(escapeString(column.comment()))
          .append("'");
    }

    return col.toString();
  }

  private String generateDataType(ColumnResponse column) {
    String type = sanitizeDataType(column.dataType());
    ColumnTypeArguments args = column.typeArguments();

    if (args == null || args.isEmpty()) {
      return type;
    }

    if (args.hasValues()) {
      String values = args.values().stream()
          .map(v -> "'" + escapeString(v) + "'")
          .collect(Collectors.joining(", "));
      return type + "(" + values + ")";
    }

    if (args.hasLength()) {
      return type + "(" + args.length() + ")";
    }

    if (args.hasPrecisionScale()) {
      return type + "(" + args.precision() + "," + args.scale() + ")";
    }

    return type;
  }

  private Set<String> collectNotNullColumnIds(TableSnapshotResponse table) {
    List<ConstraintSnapshotResponse> constraints = table.constraints() != null
        ? table.constraints()
        : Collections.emptyList();

    return constraints.stream()
        .filter(c -> c.constraint() != null
            && ConstraintKind.NOT_NULL == c.constraint().kind())
        .flatMap(c -> (c.columns() != null ? c.columns() : Collections.<ConstraintColumnResponse>emptyList()).stream())
        .map(ConstraintColumnResponse::columnId)
        .collect(Collectors.toSet());
  }

  private Optional<String> generatePrimaryKeyClause(TableSnapshotResponse table,
      Map<String, String> columnIdToName) {
    List<ConstraintSnapshotResponse> constraints = table.constraints() != null
        ? table.constraints()
        : Collections.emptyList();

    return constraints.stream()
        .filter(c -> c.constraint() != null
            && ConstraintKind.PRIMARY_KEY == c.constraint().kind())
        .findFirst()
        .map(pk -> {
          List<ConstraintColumnResponse> cols = pk.columns() != null
              ? pk.columns()
              : Collections.emptyList();

          String columnList = cols.stream()
              .sorted(Comparator.comparing(ConstraintColumnResponse::seqNo,
                  Comparator.nullsLast(Comparator.naturalOrder())))
              .map(cc -> quoteColumn(columnIdToName, cc.columnId()))
              .collect(Collectors.joining(", "));

          return "  PRIMARY KEY (" + columnList + ")";
        });
  }

  private String generateTableOptions(TableSnapshotResponse table) {
    StringBuilder options = new StringBuilder(" ENGINE=InnoDB");

    sanitizeCharset(table.table().charset())
        .ifPresent(cs -> options.append(" DEFAULT CHARSET=").append(cs));

    sanitizeCollation(table.table().collation())
        .ifPresent(coll -> options.append(" COLLATE=").append(coll));

    return options.toString();
  }

}
