package com.schemafy.core.erd.column.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;

public record ColumnTypeArguments(
    Integer length,
    Integer precision,
    Integer scale,
    List<String> values) {

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

}
