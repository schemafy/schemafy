package com.schemafy.api.collaboration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PreviewAction {

  UPDATE("UPDATE"),
  CLEAR("CLEAR");

  private final String value;

  PreviewAction(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static PreviewAction fromValue(String value) {
    if (value == null) {
      return null;
    }

    for (PreviewAction action : values()) {
      if (action.value.equalsIgnoreCase(value)) {
        return action;
      }
    }

    return null;
  }

}
