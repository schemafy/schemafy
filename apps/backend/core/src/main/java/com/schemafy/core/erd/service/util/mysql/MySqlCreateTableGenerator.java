package com.schemafy.core.erd.service.util.mysql;

import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.escapeIdentifier;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.escapeString;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.requireNonBlank;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.sanitizeCharset;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.sanitizeCollation;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.sanitizeDataType;
import static com.schemafy.core.erd.service.util.mysql.MySqlDdlUtils.sanitizeLengthScale;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;

@Component
public class MySqlCreateTableGenerator {

  public String generate(TableDetailResponse table) {
    requireNonBlank(table.getName(), "Table name");

    List<ColumnResponse> columns = table.getColumns() != null
        ? table.getColumns().stream().filter(c -> c != null).toList()
        : Collections.emptyList();

    if (columns.isEmpty()) {
      throw new IllegalArgumentException(
          "Table must have at least one column: " + table.getName());
    }

    StringBuilder ddl = new StringBuilder();
    ddl.append("CREATE TABLE `").append(escapeIdentifier(table.getName()))
        .append("` (\n");

    List<String> columnDefs = columns.stream()
        .sorted(Comparator.comparing(ColumnResponse::getSeqNo,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(this::generateColumnDefinition)
        .toList();

    ddl.append(String.join(",\n", columnDefs));
    ddl.append("\n) ");
    ddl.append(generateTableOptions(table));
    ddl.append(";");

    return ddl.toString();
  }

  private String generateColumnDefinition(ColumnResponse column) {
    requireNonBlank(column.getName(), "Column name");

    StringBuilder col = new StringBuilder();
    col.append("  `").append(escapeIdentifier(column.getName())).append("` ");
    col.append(generateDataType(column));

    sanitizeCharset(column.getCharset())
        .ifPresent(cs -> col.append(" CHARACTER SET ").append(cs));

    sanitizeCollation(column.getCollation())
        .ifPresent(coll -> col.append(" COLLATE ").append(coll));

    if (Boolean.TRUE.equals(column.getIsAutoIncrement())) {
      col.append(" AUTO_INCREMENT");
    }

    if (column.getComment() != null && !column.getComment().isEmpty()) {
      col.append(" COMMENT '").append(escapeString(column.getComment()))
          .append("'");
    }

    return col.toString();
  }

  private String generateDataType(ColumnResponse column) {
    String type = sanitizeDataType(column.getDataType());
    return sanitizeLengthScale(column.getLengthScale())
        .map(ls -> type + "(" + ls + ")")
        .orElse(type);
  }

  private String generateTableOptions(TableDetailResponse table) {
    StringBuilder options = new StringBuilder();

    options.append("ENGINE=InnoDB");
    options.append(" DEFAULT CHARSET=utf8mb4");
    options.append(" COLLATE=utf8mb4_unicode_ci");

    if (table.getComment() != null && !table.getComment().isEmpty()) {
      options.append(" COMMENT='").append(escapeString(table.getComment()))
          .append("'");
    }

    return options.toString();
  }

}
