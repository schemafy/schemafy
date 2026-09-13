package com.schemafy.core.erd.column.domain.validator;

import java.util.List;
import java.util.regex.Pattern;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;

public final class ColumnValidator {

  private static final int NAME_MIN_LENGTH = 1;
  private static final int SCHEMAFY_NAME_MAX_LENGTH = 40;
  private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

  private ColumnValidator() {}

  public static int schemafyNameMaxLength() {
    return SCHEMAFY_NAME_MAX_LENGTH;
  }

  public static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new DomainException(ColumnErrorCode.NAME_INVALID, "Column name must not be blank");
    }
    String trimmed = name.trim();
    if (trimmed.length() < NAME_MIN_LENGTH || trimmed.length() > SCHEMAFY_NAME_MAX_LENGTH) {
      throw new DomainException(ColumnErrorCode.NAME_INVALID,
          "Column name must be between %d and %d characters"
              .formatted(NAME_MIN_LENGTH, SCHEMAFY_NAME_MAX_LENGTH));
    }
    if (!NAME_PATTERN.matcher(trimmed).matches()) {
      throw new DomainException(ColumnErrorCode.NAME_INVALID, "Column name has an invalid format");
    }
  }

  public static void validateReservedKeyword(String dbVendorName, String name) {
    if (ReservedKeywordRegistry.isReserved(dbVendorName, name)) {
      throw new DomainException(ColumnErrorCode.NAME_RESERVED, "Column name is a reserved keyword: " + name);
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
      throw new DomainException(ColumnErrorCode.NAME_DUPLICATE, "Column name already exists in table: " + name);
    }
  }

  public static void validateAutoIncrementUniqueness(
      boolean autoIncrement,
      List<Column> columns,
      String ignoreColumnId) {
    if (autoIncrement && columns != null) {
      boolean exists = columns.stream()
          .anyMatch(column -> !equalsIgnoreCase(column.id(), ignoreColumnId)
              && column.autoIncrement());
      if (exists) {
        throw new DomainException(ColumnErrorCode.MULTIPLE_AUTO_INCREMENT, "Only one auto-increment column is allowed");
      }
    }
  }

  public static void validatePosition(int seqNo) {
    if (seqNo < 0) {
      throw new DomainException(ColumnErrorCode.POSITION_INVALID, "Column position must be zero or positive");
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

}
