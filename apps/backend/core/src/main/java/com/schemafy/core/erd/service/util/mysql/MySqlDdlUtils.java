package com.schemafy.core.erd.service.util.mysql;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class MySqlDdlUtils {

  private static final Pattern VALID_DATA_TYPE_PATTERN = Pattern
      .compile("^[A-Z][A-Z0-9_ ]*$");

  private static final Pattern VALID_LENGTH_SCALE_PATTERN = Pattern
      .compile("^[0-9]+(,[0-9]+)?$");

  private static final Pattern VALID_CHARSET_PATTERN = Pattern
      .compile("^[a-zA-Z][a-zA-Z0-9_]*$");

  private static final Set<String> VALID_REFERENTIAL_ACTIONS = Set.of(
      "CASCADE", "SET NULL", "SET DEFAULT", "RESTRICT", "NO ACTION");

  private static final Set<String> VALID_SORT_DIRECTIONS = Set.of("ASC", "DESC");

  private static final Set<String> VALID_INDEX_TYPES = Set.of(
      "BTREE", "HASH", "FULLTEXT", "SPATIAL");

  public static String escapeIdentifier(String identifier) {
    if (identifier == null) return "";
    return identifier.replace("`", "``");
  }

  public static void requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
  }

  public static String escapeString(String str) {
    if (str == null) return "";
    return str.replace("\\", "\\\\").replace("'", "''");
  }

  public static String sanitizeDataType(String dataType) {
    if (dataType == null || dataType.isEmpty())
      throw new IllegalArgumentException("Data type cannot be null or empty");

    String normalized = dataType.toUpperCase().trim();
    if (!VALID_DATA_TYPE_PATTERN.matcher(normalized).matches()) {
      throw new IllegalArgumentException(
          "Invalid data type format: " + dataType);
    }
    return normalized;
  }

  public static Optional<String> sanitizeLengthScale(String lengthScale) {
    if (lengthScale == null || lengthScale.isEmpty()) return Optional.empty();

    String trimmed = lengthScale.trim();
    if (!VALID_LENGTH_SCALE_PATTERN.matcher(trimmed).matches()) {
      throw new IllegalArgumentException(
          "Invalid length/scale format: " + lengthScale);
    }
    return Optional.of(trimmed);
  }

  public static Optional<String> sanitizeCharset(String charset) {
    if (charset == null || charset.isEmpty()) return Optional.empty();

    String trimmed = charset.trim();
    if (!VALID_CHARSET_PATTERN.matcher(trimmed).matches()) {
      throw new IllegalArgumentException("Invalid charset format: " + charset);
    }
    return Optional.of(trimmed);
  }

  public static Optional<String> sanitizeCollation(String collation) {
    if (collation == null || collation.isEmpty()) return Optional.empty();

    String trimmed = collation.trim();
    if (!VALID_CHARSET_PATTERN.matcher(trimmed).matches()) {
      throw new IllegalArgumentException(
          "Invalid collation format: " + collation);
    }
    return Optional.of(trimmed);
  }

  public static Optional<String> sanitizeIndexType(String indexType) {
    if (indexType == null || indexType.isEmpty()) return Optional.empty();

    String normalized = indexType.toUpperCase().trim();
    if (!VALID_INDEX_TYPES.contains(normalized)) {
      throw new IllegalArgumentException("Invalid index type: " + indexType);
    }
    return Optional.of(normalized);
  }

  public static Optional<String> sanitizeSortDirection(String sortDir) {
    if (sortDir == null || sortDir.isEmpty()) return Optional.empty();

    String normalized = sortDir.toUpperCase().trim();
    if (!VALID_SORT_DIRECTIONS.contains(normalized)) {
      throw new IllegalArgumentException("Invalid sort direction: " + sortDir);
    }
    return Optional.of(normalized);
  }

  public static Optional<String> sanitizeReferentialAction(String action) {
    if (action == null || action.isEmpty()) return Optional.empty();

    String normalized = action.toUpperCase().trim();
    if (!VALID_REFERENTIAL_ACTIONS.contains(normalized)) {
      throw new IllegalArgumentException(
          "Invalid referential action: " + action);
    }
    return Optional.of(normalized);
  }

}
