package com.schemafy.core.erd.vendor.domain.datatype;

public record DatatypeParameter(
    DatatypeParameterName name,
    String label,
    DatatypeParameterValueType valueType,
    boolean required,
    int order,
    Integer minValue,
    Integer maxValue,
    Integer minItems,
    Integer maxItems,
    Integer minItemLength,
    Integer maxItemLength) {

  public DatatypeParameter {
    if (name == null) {
      throw new IllegalArgumentException("Datatype parameter name must not be null");
    }
    if (label == null || label.isBlank()) {
      throw new IllegalArgumentException("Datatype parameter label must not be blank");
    }
    label = label.trim();
    if (valueType == null) {
      throw new IllegalArgumentException("Datatype parameter valueType must not be null");
    }
    if (order <= 0) {
      throw new IllegalArgumentException("Datatype parameter order must be positive");
    }
    validateNameAndValueType(name, valueType);
    if (valueType == DatatypeParameterValueType.INTEGER) {
      validateIntegerLimits(minValue, maxValue, minItems, maxItems,
          minItemLength, maxItemLength);
    } else {
      validateArrayLimits(minValue, maxValue, minItems, maxItems,
          minItemLength, maxItemLength);
    }
  }

  private static void validateNameAndValueType(
      DatatypeParameterName name,
      DatatypeParameterValueType valueType) {
    DatatypeParameterValueType expected = name == DatatypeParameterName.VALUES
        ? DatatypeParameterValueType.STRING_ARRAY
        : DatatypeParameterValueType.INTEGER;
    if (valueType != expected) {
      throw new IllegalArgumentException(
          "Datatype parameter %s must use %s".formatted(name.jsonName(), expected.jsonName()));
    }
  }

  private static void validateIntegerLimits(
      Integer minValue,
      Integer maxValue,
      Integer minItems,
      Integer maxItems,
      Integer minItemLength,
      Integer maxItemLength) {
    if (minValue == null || maxValue == null) {
      throw new IllegalArgumentException("Integer datatype parameter requires minValue and maxValue");
    }
    if (minValue > maxValue) {
      throw new IllegalArgumentException("Datatype parameter minValue must not exceed maxValue");
    }
    if (minItems != null || maxItems != null || minItemLength != null || maxItemLength != null) {
      throw new IllegalArgumentException("Integer datatype parameter cannot declare array limits");
    }
  }

  private static void validateArrayLimits(
      Integer minValue,
      Integer maxValue,
      Integer minItems,
      Integer maxItems,
      Integer minItemLength,
      Integer maxItemLength) {
    if (minValue != null || maxValue != null) {
      throw new IllegalArgumentException("Array datatype parameter cannot declare integer limits");
    }
    if (minItems == null || maxItems == null || minItemLength == null || maxItemLength == null) {
      throw new IllegalArgumentException(
          "Array datatype parameter requires item count and item length limits");
    }
    if (minItems < 0 || minItemLength < 0
        || minItems > maxItems || minItemLength > maxItemLength) {
      throw new IllegalArgumentException("Datatype parameter array limits are invalid");
    }
  }

}
