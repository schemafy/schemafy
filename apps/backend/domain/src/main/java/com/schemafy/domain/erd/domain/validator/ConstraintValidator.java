package com.schemafy.domain.erd.domain.validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.schemafy.domain.erd.domain.Column;
import com.schemafy.domain.erd.domain.Constraint;
import com.schemafy.domain.erd.domain.ConstraintColumn;
import com.schemafy.domain.erd.domain.exception.ConstraintColumnDuplicateException;
import com.schemafy.domain.erd.domain.exception.ConstraintColumnNotExistException;
import com.schemafy.domain.erd.domain.exception.ConstraintDefinitionDuplicateException;
import com.schemafy.domain.erd.domain.exception.ConstraintNameInvalidException;
import com.schemafy.domain.erd.domain.exception.ConstraintPositionInvalidException;
import com.schemafy.domain.erd.domain.exception.UniqueSameAsPrimaryKeyException;
import com.schemafy.domain.erd.domain.type.ConstraintKind;

public final class ConstraintValidator {

  private static final int NAME_MIN_LENGTH = 1;
  private static final int NAME_MAX_LENGTH = 255;

  private ConstraintValidator() {}

  public static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new ConstraintNameInvalidException("Constraint name must not be blank");
    }
    String trimmed = name.trim();
    if (trimmed.length() < NAME_MIN_LENGTH || trimmed.length() > NAME_MAX_LENGTH) {
      throw new ConstraintNameInvalidException(
          "Constraint name must be between %d and %d characters".formatted(
              NAME_MIN_LENGTH,
              NAME_MAX_LENGTH));
    }
  }

  public static void validatePosition(int seqNo) {
    if (seqNo < 0) {
      throw new ConstraintPositionInvalidException(
          "Constraint column position must be zero or positive");
    }
  }

  public static void validateColumnExistence(
      List<Column> tableColumns,
      List<String> columnIds,
      String constraintName) {
    if (tableColumns == null || columnIds == null) {
      return;
    }
    for (String columnId : columnIds) {
      boolean exists = tableColumns.stream()
          .anyMatch(column -> equalsIgnoreCase(column.id(), columnId));
      if (!exists) {
        throw new ConstraintColumnNotExistException(
            "Column '%s' specified in constraint '%s' does not exist in the table".formatted(
                columnId,
                constraintName));
      }
    }
  }

  public static void validateColumnUniqueness(
      List<String> columnIds,
      String constraintName) {
    if (columnIds == null) {
      return;
    }
    Set<String> uniqueIds = new HashSet<>();
    for (String columnId : columnIds) {
      String key = columnId == null ? null : columnId.toUpperCase(Locale.ROOT);
      if (!uniqueIds.add(key)) {
        throw new ConstraintColumnDuplicateException(
            "Column '%s' is already included in constraint '%s'".formatted(
                columnId,
                constraintName));
      }
    }
  }

  public static void validateDefinitionUniqueness(
      List<Constraint> constraints,
      Map<String, List<ConstraintColumn>> constraintColumns,
      ConstraintKind kind,
      String checkExpr,
      String defaultExpr,
      List<String> columnIds,
      String constraintName,
      String ignoreConstraintId) {
    if (constraints == null) {
      return;
    }
    String candidateDefinition = definitionKey(kind, checkExpr, defaultExpr, columnIds);
    for (Constraint constraint : constraints) {
      if (equalsIgnoreCase(constraint.id(), ignoreConstraintId)) {
        continue;
      }
      List<ConstraintColumn> columns = constraintColumns == null
          ? List.of()
          : constraintColumns.getOrDefault(constraint.id(), List.of());
      String existingDefinition = definitionKey(
          constraint.kind(),
          constraint.checkExpr(),
          constraint.defaultExpr(),
          toColumnIds(columns));
      if (candidateDefinition.equals(existingDefinition)) {
        throw new ConstraintDefinitionDuplicateException(
            "Constraint '%s' has the same definition as existing constraint '%s'".formatted(
                constraintName,
                constraint.name()));
      }
    }
  }

  public static void validateUniqueSameAsPrimaryKey(
      List<Constraint> constraints,
      Map<String, List<ConstraintColumn>> constraintColumns,
      ConstraintKind kind,
      List<String> columnIds,
      String constraintName,
      String ignoreConstraintId) {
    if (kind != ConstraintKind.UNIQUE || constraints == null) {
      return;
    }
    List<String> candidateColumns = normalizeColumnIds(columnIds);
    for (Constraint constraint : constraints) {
      if (constraint.kind() != ConstraintKind.PRIMARY_KEY) {
        continue;
      }
      if (equalsIgnoreCase(constraint.id(), ignoreConstraintId)) {
        continue;
      }
      List<ConstraintColumn> columns = constraintColumns == null
          ? List.of()
          : constraintColumns.getOrDefault(constraint.id(), List.of());
      if (candidateColumns.equals(normalizeColumnIds(toColumnIds(columns)))) {
        throw new UniqueSameAsPrimaryKeyException(
            "Unique constraint '%s' duplicates the primary key columns of '%s'".formatted(
                constraintName,
                constraint.name()));
      }
    }
  }

  private static String definitionKey(
      ConstraintKind kind,
      String checkExpr,
      String defaultExpr,
      List<String> columnIds) {
    String normalizedCheck = normalizeOptional(checkExpr);
    String normalizedDefault = normalizeOptional(defaultExpr);
    return kind + "|" + normalizedCheck + "|" + normalizedDefault + "|"
        + String.join(",", normalizeColumnIds(columnIds));
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.trim();
  }

  private static List<String> toColumnIds(List<ConstraintColumn> columns) {
    if (columns == null) {
      return List.of();
    }
    List<String> columnIds = new ArrayList<>(columns.size());
    for (ConstraintColumn column : columns) {
      columnIds.add(column.columnId());
    }
    return columnIds;
  }

  private static List<String> normalizeColumnIds(List<String> columnIds) {
    if (columnIds == null) {
      return List.of();
    }
    List<String> normalized = new ArrayList<>(columnIds.size());
    for (String columnId : columnIds) {
      normalized.add(columnId == null ? null : columnId.trim());
    }
    normalized.sort(Comparator.nullsLast(String::compareToIgnoreCase));
    return normalized;
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
