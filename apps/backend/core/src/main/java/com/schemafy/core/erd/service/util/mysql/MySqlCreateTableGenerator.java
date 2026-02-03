package com.schemafy.core.erd.service.util.mysql;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
import com.schemafy.core.erd.controller.dto.response.TableDetailResponse;

@Component
public class MySqlCreateTableGenerator {

  private static final Pattern VALID_DATA_TYPE_PATTERN = Pattern
      .compile("^[A-Z][A-Z0-9_ ]*$");

  private static final Pattern VALID_LENGTH_SCALE_PATTERN = Pattern
      .compile("^[0-9]+(,[0-9]+)?$");

  private static final Pattern VALID_CHARSET_PATTERN = Pattern
      .compile("^[a-zA-Z][a-zA-Z0-9_]*$");

  public String generate(TableDetailResponse table) {
    StringBuilder ddl = new StringBuilder();
    ddl.append("CREATE TABLE `").append(escapeIdentifier(table.getName()))
        .append("` (\n");

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

    col.append("  `").append(escapeIdentifier(column.getName())).append("` ");
    col.append(generateDataType(column));

    String charset = sanitizeCharset(column.getCharset());
    if (charset != null) {
      col.append(" CHARACTER SET ").append(charset);
    }
    String collation = sanitizeCollation(column.getCollation());
    if (collation != null) {
      col.append(" COLLATE ").append(collation);
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
    String type = sanitizeDataType(column.getDataType());
    String lengthScale = sanitizeLengthScale(column.getLengthScale());
    if (lengthScale != null) {
      return type + "(" + lengthScale + ")";
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
    return str.replace("\\", "\\\\").replace("'", "''");
  }

  private String escapeIdentifier(String identifier) {
    if (identifier == null) {
      return "";
    }
    return identifier.replace("`", "``");
  }

  private String sanitizeDataType(String dataType) {
    if (dataType == null || dataType.isEmpty()) {
      throw new IllegalArgumentException("Data type cannot be null or empty");
    }
    String normalized = dataType.toUpperCase().trim();
    if (!VALID_DATA_TYPE_PATTERN.matcher(normalized).matches()) {
      throw new IllegalArgumentException(
          "Invalid data type format: " + dataType);
    }
    return normalized;
  }

  private String sanitizeLengthScale(String lengthScale) {
    if (lengthScale == null || lengthScale.isEmpty()) {
      return null;
    }
    String trimmed = lengthScale.trim();
    if (!VALID_LENGTH_SCALE_PATTERN.matcher(trimmed).matches()) {
      throw new IllegalArgumentException(
          "Invalid length/scale format: " + lengthScale);
    }
    return trimmed;
  }

  private String sanitizeCharset(String charset) {
    if (charset == null || charset.isEmpty()) {
      return null;
    }
    String trimmed = charset.trim();
    if (!VALID_CHARSET_PATTERN.matcher(trimmed).matches()) {
      throw new IllegalArgumentException("Invalid charset format: " + charset);
    }
    return trimmed;
  }

  private String sanitizeCollation(String collation) {
    if (collation == null || collation.isEmpty()) {
      return null;
    }
    String trimmed = collation.trim();
    if (!VALID_CHARSET_PATTERN.matcher(trimmed).matches()) {
      throw new IllegalArgumentException(
          "Invalid collation format: " + collation);
    }
    return trimmed;
  }

}
