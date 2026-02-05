package com.schemafy.core.erd.service.util.mysql;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MySqlDdlUtils {

  private static final Set<String> VALID_REFERENTIAL_ACTIONS = Set.of(
      "CASCADE", "SET NULL", "SET DEFAULT", "RESTRICT", "NO ACTION");

  private static final Set<String> VALID_SORT_DIRECTIONS = Set.of("ASC", "DESC");

  private static final Set<String> VALID_INDEX_TYPES = Set.of(
      "BTREE", "HASH", "FULLTEXT", "SPATIAL");

  public static String escapeIdentifier(String identifier) {
    if (identifier == null)
      return "";
    return identifier.replace("`", "``");
  }

  public static void requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
  }

  public static String escapeString(String str) {
    if (str == null)
      return "";
    return str.replace("\\", "\\\\").replace("'", "''");
  }

  public static String sanitizeDataType(String dataType) {
    if (dataType == null || dataType.isEmpty()) {
      throw new IllegalArgumentException("Data type cannot be null or empty");
    }

    String normalized = dataType.toUpperCase().trim();
    if (!isValidDataType(normalized)) {
      throw new IllegalArgumentException(
          "Invalid data type format: " + dataType);
    }
    return normalized;
  }

  public static Optional<String> sanitizeLengthScale(String lengthScale) {
    if (lengthScale == null || lengthScale.isEmpty())
      return Optional.empty();

    String trimmed = lengthScale.trim();
    if (!isValidLengthScale(trimmed)) {
      throw new IllegalArgumentException(
          "Invalid length/scale format: " + lengthScale);
    }
    return Optional.of(trimmed);
  }

  public static Optional<String> sanitizeCharset(String charset) {
    if (charset == null || charset.isEmpty())
      return Optional.empty();

    String trimmed = charset.trim();
    if (!isValidIdentifierFormat(trimmed)) {
      throw new IllegalArgumentException("Invalid charset format: " + charset);
    }
    return Optional.of(trimmed);
  }

  public static Optional<String> sanitizeCollation(String collation) {
    if (collation == null || collation.isEmpty())
      return Optional.empty();

    String trimmed = collation.trim();
    if (!isValidIdentifierFormat(trimmed)) {
      throw new IllegalArgumentException(
          "Invalid collation format: " + collation);
    }
    return Optional.of(trimmed);
  }

  public static Optional<String> sanitizeIndexType(String indexType) {
    if (indexType == null || indexType.isEmpty())
      return Optional.empty();

    String normalized = indexType.toUpperCase().trim();
    if (!VALID_INDEX_TYPES.contains(normalized)) {
      throw new IllegalArgumentException("Invalid index type: " + indexType);
    }
    return Optional.of(normalized);
  }

  public static Optional<String> sanitizeSortDirection(String sortDir) {
    if (sortDir == null || sortDir.isEmpty())
      return Optional.empty();

    String normalized = sortDir.toUpperCase().trim();
    if (!VALID_SORT_DIRECTIONS.contains(normalized)) {
      throw new IllegalArgumentException("Invalid sort direction: " + sortDir);
    }
    return Optional.of(normalized);
  }

  public static Optional<String> sanitizeReferentialAction(String action) {
    if (action == null || action.isEmpty())
      return Optional.empty();

    String normalized = action.toUpperCase().trim();
    if (!VALID_REFERENTIAL_ACTIONS.contains(normalized)) {
      throw new IllegalArgumentException(
          "Invalid referential action: " + action);
    }
    return Optional.of(normalized);
  }

  public static String quoteColumn(Map<String, String> columnIdToName,
      String columnId) {
    String name = columnIdToName.get(columnId);
    if (name == null) {
      throw new IllegalStateException("Column not found: " + columnId);
    }
    return "`" + escapeIdentifier(name) + "`";
  }

  // ^[A-Z][A-Z0-9_ ]*$
  private static boolean isValidDataType(String value) {
    if (value.isEmpty())
      return false;

    char first = value.charAt(0);
    if (first < 'A' || first > 'Z')
      return false;

    for (int i = 1; i < value.length(); i++) {
      char c = value.charAt(i);
      boolean valid = (c >= 'A' && c <= 'Z')
          || (c >= '0' && c <= '9')
          || c == '_'
          || c == ' ';
      if (!valid)
        return false;
    }
    return true;
  }

  // ^[0-9]+(,[0-9]+)?$
  private static boolean isValidLengthScale(String value) {
    if (value.isEmpty())
      return false;

    int commaIndex = value.indexOf(',');

    if (commaIndex == -1) {
      return isDigitsOnly(value);
    }

    if (commaIndex == 0 || commaIndex == value.length() - 1)
      return false;

    String before = value.substring(0, commaIndex);
    String after = value.substring(commaIndex + 1);

    return isDigitsOnly(before) && isDigitsOnly(after);
  }

  private static boolean isDigitsOnly(String value) {
    if (value.isEmpty())
      return false;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c < '0' || c > '9')
        return false;
    }
    return true;
  }

  // ^[a-zA-Z][a-zA-Z0-9_]*$
  private static boolean isValidIdentifierFormat(String value) {
    if (value.isEmpty())
      return false;

    char first = value.charAt(0);
    boolean validFirst = (first >= 'a' && first <= 'z')
        || (first >= 'A' && first <= 'Z');
    if (!validFirst)
      return false;

    for (int i = 1; i < value.length(); i++) {
      char c = value.charAt(i);
      boolean valid = (c >= 'a' && c <= 'z')
          || (c >= 'A' && c <= 'Z')
          || (c >= '0' && c <= '9')
          || c == '_';
      if (!valid)
        return false;
    }
    return true;
  }

}
