package com.schemafy.domain.erd.column.domain.validator;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.ColumnLengthScale;
import com.schemafy.domain.erd.column.domain.exception.ColumnAutoIncrementNotAllowedException;
import com.schemafy.domain.erd.column.domain.exception.ColumnCharsetNotAllowedException;
import com.schemafy.domain.erd.column.domain.exception.ColumnDataTypeInvalidException;
import com.schemafy.domain.erd.column.domain.exception.ColumnLengthRequiredException;
import com.schemafy.domain.erd.column.domain.exception.ColumnNameDuplicateException;
import com.schemafy.domain.erd.column.domain.exception.ColumnNameInvalidException;
import com.schemafy.domain.erd.column.domain.exception.ColumnNameReservedException;
import com.schemafy.domain.erd.column.domain.exception.ColumnPositionInvalidException;
import com.schemafy.domain.erd.column.domain.exception.ColumnPrecisionRequiredException;
import com.schemafy.domain.erd.column.domain.exception.MultipleAutoIncrementColumnException;

public final class ColumnValidator {

  private static final int NAME_MIN_LENGTH = 1;
  private static final int NAME_MAX_LENGTH = 40;
  private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

  private static final Set<String> SUPPORTED_TYPES = Set.of(
      "TINYINT",
      "SMALLINT",
      "MEDIUMINT",
      "INT",
      "INTEGER",
      "BIGINT",
      "FLOAT",
      "DOUBLE",
      "REAL",
      "DECIMAL",
      "NUMERIC",
      "BIT",
      "BOOL",
      "BOOLEAN",
      "CHAR",
      "VARCHAR",
      "TINYTEXT",
      "TEXT",
      "MEDIUMTEXT",
      "LONGTEXT",
      "BINARY",
      "VARBINARY",
      "BLOB",
      "TINYBLOB",
      "MEDIUMBLOB",
      "LONGBLOB",
      "ENUM",
      "SET",
      "DATE",
      "TIME",
      "DATETIME",
      "TIMESTAMP",
      "YEAR",
      "GEOMETRY",
      "POINT",
      "LINESTRING",
      "POLYGON",
      "MULTIPOINT",
      "MULTILINESTRING",
      "MULTIPOLYGON",
      "GEOMETRYCOLLECTION",
      "JSON");

  private static final Set<String> LENGTH_REQUIRED_TYPES = Set.of("VARCHAR", "CHAR");
  private static final Set<String> PRECISION_REQUIRED_TYPES = Set.of("DECIMAL", "NUMERIC");
  private static final Set<String> INTEGER_TYPES = Set.of(
      "TINYINT",
      "SMALLINT",
      "MEDIUMINT",
      "INT",
      "INTEGER",
      "BIGINT");
  private static final Set<String> TEXT_TYPES = Set.of(
      "CHAR",
      "VARCHAR",
      "TINYTEXT",
      "TEXT",
      "MEDIUMTEXT",
      "LONGTEXT",
      "ENUM",
      "SET");

  private ColumnValidator() {}

  public static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new ColumnNameInvalidException("Column name must not be blank");
    }
    String trimmed = name.trim();
    if (trimmed.length() < NAME_MIN_LENGTH || trimmed.length() > NAME_MAX_LENGTH) {
      throw new ColumnNameInvalidException(
          "Column name must be between %d and %d characters".formatted(NAME_MIN_LENGTH, NAME_MAX_LENGTH));
    }
    if (!NAME_PATTERN.matcher(trimmed).matches()) {
      throw new ColumnNameInvalidException("Column name has an invalid format");
    }
  }

  public static void validateReservedKeyword(String dbVendorName, String name) {
    if (ReservedKeywordRegistry.isReserved(dbVendorName, name)) {
      throw new ColumnNameReservedException("Column name is a reserved keyword: " + name);
    }
  }

  public static void validateNameUniqueness(
      List<Column> columns,
      String name,
      String ignoreColumnId) {
    if (columns == null || name == null) {
      return;
    }
    String target = name.trim();
    boolean duplicated = columns.stream()
        .anyMatch(column -> !equalsIgnoreCase(column.id(), ignoreColumnId)
            && equalsIgnoreCase(column.name(), target));
    if (duplicated) {
      throw new ColumnNameDuplicateException("Column name already exists in table: " + name);
    }
  }

  public static String normalizeDataType(String dataType) {
    if (dataType == null || dataType.isBlank()) {
      throw new ColumnDataTypeInvalidException("Column data type must not be blank");
    }
    return dataType.trim().toUpperCase(Locale.ROOT);
  }

  public static void validateDataType(String dataType) {
    String normalized = normalizeDataType(dataType);
    if (!SUPPORTED_TYPES.contains(normalized)) {
      throw new ColumnDataTypeInvalidException("Unsupported column data type: " + dataType);
    }
  }

  public static void validateLengthScale(String dataType, ColumnLengthScale lengthScale) {
    String normalized = normalizeDataType(dataType);
    if (LENGTH_REQUIRED_TYPES.contains(normalized)) {
      if (lengthScale == null || lengthScale.length() == null) {
        throw new ColumnLengthRequiredException("Length is required for data type: " + normalized);
      }
    }
    if (PRECISION_REQUIRED_TYPES.contains(normalized)) {
      if (lengthScale == null || lengthScale.precision() == null || lengthScale.scale() == null) {
        throw new ColumnPrecisionRequiredException(
            "Precision and scale are required for data type: " + normalized);
      }
    }
  }

  public static void validateAutoIncrement(
      String dataType,
      boolean autoIncrement,
      List<Column> columns,
      String ignoreColumnId) {
    String normalized = normalizeDataType(dataType);
    if (autoIncrement && !INTEGER_TYPES.contains(normalized)) {
      throw new ColumnAutoIncrementNotAllowedException(
          "Auto increment is only allowed for integer types: " + normalized);
    }
    if (autoIncrement && columns != null) {
      boolean exists = columns.stream()
          .anyMatch(column -> !equalsIgnoreCase(column.id(), ignoreColumnId)
              && column.autoIncrement());
      if (exists) {
        throw new MultipleAutoIncrementColumnException("Only one auto-increment column is allowed");
      }
    }
  }

  public static void validateCharsetAndCollation(String dataType, String charset, String collation) {
    if (!hasText(charset) && !hasText(collation)) {
      return;
    }
    String normalized = normalizeDataType(dataType);
    if (!TEXT_TYPES.contains(normalized)) {
      throw new ColumnCharsetNotAllowedException(
          "Charset or collation is only allowed for text types: " + normalized);
    }
  }

  public static void validatePosition(int seqNo) {
    if (seqNo < 0) {
      throw new ColumnPositionInvalidException("Column position must be zero or positive");
    }
  }

  private static boolean equalsIgnoreCase(String left, String right) {
    if (left == null && right == null) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    return left.equalsIgnoreCase(right);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  public static boolean isTextType(String dataType) {
    if (dataType == null || dataType.isBlank()) {
      return false;
    }
    return TEXT_TYPES.contains(dataType.trim().toUpperCase(Locale.ROOT));
  }

}
