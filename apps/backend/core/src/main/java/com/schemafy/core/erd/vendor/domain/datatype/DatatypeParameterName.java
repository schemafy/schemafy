package com.schemafy.core.erd.vendor.domain.datatype;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DatatypeParameterName {

  LENGTH("length"),
  PRECISION("precision"),
  SCALE("scale"),
  VALUES("values");

  private final String jsonName;

  DatatypeParameterName(String jsonName) {
    this.jsonName = jsonName;
  }

  @JsonValue
  public String jsonName() {
    return jsonName;
  }

  @JsonCreator
  public static DatatypeParameterName fromJson(String value) {
    return Arrays.stream(values())
        .filter(name -> name.jsonName.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown datatype parameter name: " + value));
  }

}
