package com.schemafy.core.erd.vendor.domain.datatype;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DatatypeParameterValueType {

  INTEGER("integer"),
  STRING_ARRAY("string_array");

  private final String jsonName;

  DatatypeParameterValueType(String jsonName) {
    this.jsonName = jsonName;
  }

  @JsonValue
  public String jsonName() {
    return jsonName;
  }

  @JsonCreator
  public static DatatypeParameterValueType fromJson(String value) {
    return Arrays.stream(values())
        .filter(type -> type.jsonName.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown datatype parameter value type: " + value));
  }

}
