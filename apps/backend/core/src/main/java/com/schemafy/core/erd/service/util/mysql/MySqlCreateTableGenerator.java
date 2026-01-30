package com.schemafy.core.erd.service.util.mysql;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;

@Component
public class MySqlCreateTableGenerator {

  public String generate(TableDetailResponse table) {
    StringBuilder ddl = new StringBuilder();
    ddl.append("CREATE TABLE `").append(table.getName()).append("` (\n");

    List<String> columnDefs = table.getColumns().stream()
        .sorted(Comparator.comparing(ColumnResponse::getSeqNo))
        .map(this::generateColumnDefinition)
        .toList();

    ddl.append(String.join(",\n", columnDefs));
    ddl.append("\n) ");
    ddl.append(generateTableOptions(table));
    ddl.append(";");

    return ddl.toString();
  }

  private String generateColumnDefinition(ColumnResponse column) {
    StringBuilder col = new StringBuilder();

    col.append("  `").append(column.getName()).append("` ");
    col.append(generateDataType(column));

    if (column.getCharset() != null && !column.getCharset().isEmpty()) {
      col.append(" CHARACTER SET ").append(column.getCharset());
    }
    if (column.getCollation() != null && !column.getCollation().isEmpty()) {
      col.append(" COLLATE ").append(column.getCollation());
    }

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
    String type = column.getDataType();
    if (column.getLengthScale() != null && !column.getLengthScale().isEmpty()) {
      return type + "(" + column.getLengthScale() + ")";
    }
    return type;
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

  private String escapeString(String str) {
    if (str == null) {
      return "";
    }
    return str.replace("'", "''");
  }

}
