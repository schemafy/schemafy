package com.schemafy.domain.erd.column.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.column.domain.exception.ColumnErrorCode;

public record ColumnTypeArguments(
    Integer length,
    Integer precision,
    Integer scale,
    List<String> values) {

  private static final Pattern LENGTH_PATTERN = Pattern.compile("\"length\"\\s*:\\s*(\\d+)");
  private static final Pattern PRECISION_PATTERN = Pattern.compile("\"precision\"\\s*:\\s*(\\d+)");
  private static final Pattern SCALE_PATTERN = Pattern.compile("\"scale\"\\s*:\\s*(\\d+)");
  private static final Pattern VALUES_KEY_PATTERN = Pattern.compile("\"values\"\\s*:");
  private static final Pattern VALUES_PATTERN = Pattern.compile("\"values\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
  private static final Pattern VALUE_ITEM_PATTERN = Pattern.compile("\"((?:\\\\.|[^\\\\\"])*)\"");

  public ColumnTypeArguments(Integer length, Integer precision, Integer scale) {
    this(length, precision, scale, null);
  }

  public ColumnTypeArguments {
    if (length != null && (precision != null || scale != null || values != null)) {
      throw new DomainException(ColumnErrorCode.INVALID_VALUE, "length cannot be combined with precision/scale/values");
    }
    if (values != null && (precision != null || scale != null)) {
      throw new DomainException(ColumnErrorCode.INVALID_VALUE, "values cannot be combined with precision/scale");
    }
    if (length != null && length <= 0) {
      throw new DomainException(ColumnErrorCode.INVALID_VALUE, "length must be positive");
    }
    if (precision != null && precision <= 0) {
      throw new DomainException(ColumnErrorCode.INVALID_VALUE, "precision must be positive");
    }
    if (scale != null && scale < 0) {
      throw new DomainException(ColumnErrorCode.INVALID_VALUE, "scale must be zero or positive");
    }
    if (precision != null && scale == null) {
      throw new DomainException(ColumnErrorCode.INVALID_VALUE, "scale is required when precision is provided");
    }
    if (precision == null && scale != null) {
      throw new DomainException(ColumnErrorCode.INVALID_VALUE, "precision is required when scale is provided");
    }
    if (values != null) {
      if (values.isEmpty()) {
        throw new DomainException(ColumnErrorCode.INVALID_VALUE, "values must not be empty");
      }
      List<String> normalizedValues = new ArrayList<>(values.size());
      Set<String> uniqueValues = new HashSet<>();
      for (String value : values) {
        if (value == null || value.isBlank()) {
          throw new DomainException(ColumnErrorCode.INVALID_VALUE, "values must not contain blank items");
        }
        String trimmed = value.trim();
        if (!uniqueValues.add(trimmed)) {
          throw new DomainException(ColumnErrorCode.INVALID_VALUE, "values must not contain duplicates");
        }
        normalizedValues.add(trimmed);
      }
      values = List.copyOf(normalizedValues);
    }
  }

  public static ColumnTypeArguments from(Integer length, Integer precision, Integer scale) {
    return from(length, precision, scale, null);
  }

  public static ColumnTypeArguments from(Integer length, Integer precision, Integer scale, List<String> values) {
    if (length == null && precision == null && scale == null && values == null) {
      return null;
    }
    return new ColumnTypeArguments(length, precision, scale, values);
  }

  public boolean isEmpty() { return length == null && precision == null && scale == null && !hasValues(); }

  public boolean hasLength() {
    return length != null;
  }

  public boolean hasPrecisionScale() {
    return precision != null || scale != null;
  }

  public boolean hasValues() {
    return values != null && !values.isEmpty();
  }

  public String toJson() {
    if (isEmpty()) {
      return null;
    }
    if (length != null) {
      return "{\"length\":" + length + "}";
    }
    if (precision != null) {
      return "{\"precision\":" + precision + ",\"scale\":" + scale + "}";
    }
    String escapedValues = values.stream()
        .map(ColumnTypeArguments::escapeJsonString)
        .map(v -> "\"" + v + "\"")
        .collect(Collectors.joining(","));
    return "{\"values\":[" + escapedValues + "]}";
  }

  public static ColumnTypeArguments fromJson(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    Integer length = extract(json, LENGTH_PATTERN);
    Integer precision = extract(json, PRECISION_PATTERN);
    Integer scale = extract(json, SCALE_PATTERN);
    boolean hasValuesKey = VALUES_KEY_PATTERN.matcher(json).find();
    List<String> values = extractValues(json);
    if (hasValuesKey && values == null) {
      return null;
    }
    return from(length, precision, scale, values);
  }

  private static Integer extract(String json, Pattern pattern) {
    Matcher matcher = pattern.matcher(json);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    }
    return null;
  }

  private static List<String> extractValues(String json) {
    Matcher matcher = VALUES_PATTERN.matcher(json);
    if (!matcher.find()) {
      return null;
    }
    String rawValues = matcher.group(1).trim();
    if (rawValues.isEmpty()) {
      return List.of();
    }

    List<String> parsedValues = new ArrayList<>();
    Matcher itemMatcher = VALUE_ITEM_PATTERN.matcher(rawValues);
    int cursor = 0;
    boolean first = true;
    while (itemMatcher.find()) {
      String between = rawValues.substring(cursor, itemMatcher.start()).trim();
      if (first) {
        if (!between.isEmpty()) {
          return null;
        }
      } else if (!",".equals(between)) {
        return null;
      }
      parsedValues.add(unescapeJsonString(itemMatcher.group(1)));
      cursor = itemMatcher.end();
      first = false;
    }

    String tail = rawValues.substring(cursor).trim();
    if (!tail.isEmpty()) {
      return null;
    }
    return parsedValues;
  }

  private static String escapeJsonString(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"");
  }

  private static String unescapeJsonString(String value) {
    StringBuilder sb = new StringBuilder(value.length());
    boolean escaping = false;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (escaping) {
        switch (c) {
        case 'n' -> sb.append('\n');
        case 'r' -> sb.append('\r');
        case 't' -> sb.append('\t');
        case 'b' -> sb.append('\b');
        case 'f' -> sb.append('\f');
        default -> sb.append(c);
        }
        escaping = false;
      } else if (c == '\\') {
        escaping = true;
      } else {
        sb.append(c);
      }
    }
    if (escaping) {
      sb.append('\\');
    }
    return sb.toString();
  }

}
