package com.schemafy.domain.erd.domain.validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.schemafy.domain.erd.domain.Column;
import com.schemafy.domain.erd.domain.Index;
import com.schemafy.domain.erd.domain.IndexColumn;
import com.schemafy.domain.erd.domain.exception.IndexColumnDuplicateException;
import com.schemafy.domain.erd.domain.exception.IndexColumnNotExistException;
import com.schemafy.domain.erd.domain.exception.IndexColumnSortDirectionInvalidException;
import com.schemafy.domain.erd.domain.exception.IndexDefinitionDuplicateException;
import com.schemafy.domain.erd.domain.exception.IndexNameInvalidException;
import com.schemafy.domain.erd.domain.exception.IndexPositionInvalidException;
import com.schemafy.domain.erd.domain.exception.IndexTypeInvalidException;
import com.schemafy.domain.erd.domain.type.IndexType;
import com.schemafy.domain.erd.domain.type.SortDirection;

public final class IndexValidator {

  private static final int NAME_MIN_LENGTH = 1;
  private static final int NAME_MAX_LENGTH = 255;

  private IndexValidator() {}

  public static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new IndexNameInvalidException("Index name must not be blank");
    }
    String trimmed = name.trim();
    if (trimmed.length() < NAME_MIN_LENGTH || trimmed.length() > NAME_MAX_LENGTH) {
      throw new IndexNameInvalidException(
          "Index name must be between %d and %d characters".formatted(
              NAME_MIN_LENGTH,
              NAME_MAX_LENGTH));
    }
  }

  public static void validateType(IndexType type) {
    if (type == null) {
      throw new IndexTypeInvalidException("Index type is required");
    }
  }

  public static void validateSeqNoIntegrity(List<Integer> seqNos) {
    if (seqNos == null || seqNos.isEmpty()) {
      return;
    }
    Set<Integer> uniqueSeqNos = new HashSet<>();
    for (Integer seqNo : seqNos) {
      if (seqNo == null || seqNo < 0) {
        throw new IndexPositionInvalidException(
            "Index column position must be zero or positive");
      }
      if (!uniqueSeqNos.add(seqNo)) {
        throw new IndexPositionInvalidException("Index column positions must be unique");
      }
    }
    for (int expected = 0; expected < uniqueSeqNos.size(); expected++) {
      if (!uniqueSeqNos.contains(expected)) {
        throw new IndexPositionInvalidException(
            "Index column positions must be contiguous starting from 0");
      }
    }
  }

  public static void validateColumnExistence(
      List<Column> tableColumns,
      List<IndexColumn> indexColumns,
      String indexName) {
    if (indexColumns == null) {
      return;
    }
    for (IndexColumn column : indexColumns) {
      if (!containsColumn(tableColumns, column.columnId())) {
        throw new IndexColumnNotExistException(
            "Column '%s' specified in index '%s' does not exist in the table".formatted(
                column.columnId(),
                indexName));
      }
    }
  }

  public static void validateColumnUniqueness(
      List<IndexColumn> indexColumns,
      String indexName) {
    if (indexColumns == null) {
      return;
    }
    Set<String> columnIds = new HashSet<>();
    for (IndexColumn column : indexColumns) {
      String key = normalizeId(column.columnId());
      if (!columnIds.add(key)) {
        throw new IndexColumnDuplicateException(
            "Index '%s' has duplicate columns".formatted(indexName));
      }
    }
  }

  public static void validateSortDirections(
      List<IndexColumn> indexColumns,
      String indexName) {
    if (indexColumns == null) {
      return;
    }
    for (IndexColumn column : indexColumns) {
      SortDirection sortDirection = column.sortDirection();
      if (sortDirection == null) {
        throw new IndexColumnSortDirectionInvalidException(
            "Sort direction is invalid for index '%s'".formatted(indexName));
      }
    }
  }

  public static void validateDefinitionUniqueness(
      List<Index> indexes,
      Map<String, List<IndexColumn>> indexColumns,
      IndexType indexType,
      List<IndexColumn> candidateColumns,
      String indexName,
      String ignoreIndexId) {
    if (indexes == null) {
      return;
    }
    String candidateDefinition = definitionKey(indexType, candidateColumns);
    for (Index index : indexes) {
      if (equalsIgnoreCase(index.id(), ignoreIndexId)) {
        continue;
      }
      List<IndexColumn> columns = indexColumns == null
          ? List.of()
          : indexColumns.getOrDefault(index.id(), List.of());
      String existingDefinition = definitionKey(index.type(), columns);
      if (candidateDefinition.equals(existingDefinition)) {
        throw new IndexDefinitionDuplicateException(
            "Index '%s' has the same definition as existing index '%s'".formatted(
                indexName,
                index.name()));
      }
    }
  }

  private static String definitionKey(IndexType type, List<IndexColumn> columns) {
    List<String> columnKeys = new ArrayList<>();
    if (columns != null) {
      for (IndexColumn column : columns) {
        String columnId = normalizeId(column.columnId());
        String sortDir = column.sortDirection() == null
            ? ""
            : column.sortDirection().name();
        columnKeys.add(columnId + ":" + sortDir);
      }
    }
    columnKeys.sort(Comparator.nullsLast(String::compareToIgnoreCase));
    return type + "|" + String.join(",", columnKeys);
  }

  private static boolean containsColumn(List<Column> columns, String columnId) {
    if (columns == null) {
      return false;
    }
    for (Column column : columns) {
      if (equalsIgnoreCase(column.id(), columnId)) {
        return true;
      }
    }
    return false;
  }

  private static String normalizeId(String value) {
    return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
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
