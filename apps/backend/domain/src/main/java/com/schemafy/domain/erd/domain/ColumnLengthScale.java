package com.schemafy.domain.erd.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ColumnLengthScale(
    Integer length,
    Integer precision,
    Integer scale) {

  private static final Pattern LENGTH_PATTERN = Pattern.compile("\"length\"\\s*:\\s*(\\d+)");
  private static final Pattern PRECISION_PATTERN = Pattern.compile("\"precision\"\\s*:\\s*(\\d+)");
  private static final Pattern SCALE_PATTERN = Pattern.compile("\"scale\"\\s*:\\s*(\\d+)");

  public ColumnLengthScale {
    if (length != null && (precision != null || scale != null)) {
      throw new IllegalArgumentException("length cannot be combined with precision/scale");
    }
    if (length != null && length <= 0) {
      throw new IllegalArgumentException("length must be positive");
    }
    if (precision != null && precision <= 0) {
      throw new IllegalArgumentException("precision must be positive");
    }
    if (scale != null && scale < 0) {
      throw new IllegalArgumentException("scale must be zero or positive");
    }
    if (precision != null && scale == null) {
      throw new IllegalArgumentException("scale is required when precision is provided");
    }
    if (precision == null && scale != null) {
      throw new IllegalArgumentException("precision is required when scale is provided");
    }
  }

  public static ColumnLengthScale from(Integer length, Integer precision, Integer scale) {
    if (length == null && precision == null && scale == null) {
      return null;
    }
    return new ColumnLengthScale(length, precision, scale);
  }

  public boolean isEmpty() {
    return length == null && precision == null && scale == null;
  }

  public boolean hasLength() {
    return length != null;
  }

  public boolean hasPrecisionScale() {
    return precision != null || scale != null;
  }

  public String toJson() {
    if (isEmpty()) {
      return null;
    }
    if (length != null) {
      return "{\"length\":" + length + "}";
    }
    return "{\"precision\":" + precision + ",\"scale\":" + scale + "}";
  }

  public static ColumnLengthScale fromJson(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    Integer length = extract(json, LENGTH_PATTERN);
    Integer precision = extract(json, PRECISION_PATTERN);
    Integer scale = extract(json, SCALE_PATTERN);
    if (length == null && precision == null && scale == null) {
      return null;
    }
    return new ColumnLengthScale(length, precision, scale);
  }

  private static Integer extract(String json, Pattern pattern) {
    Matcher matcher = pattern.matcher(json);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    }
    return null;
  }
}
