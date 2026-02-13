package com.schemafy.core.collaboration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CollaborationEventType {

  JOIN("JOIN", false),
  LEAVE("LEAVE", false),
  CURSOR("CURSOR", false),
  SCHEMA_FOCUS("SCHEMA_FOCUS", false),
  CHAT("CHAT", true),
  ERD_MUTATED("ERD_MUTATED", false);

  private final String value;
  private final boolean includeSender;

  CollaborationEventType(String value, boolean includeSender) {
    this.value = value;
    this.includeSender = includeSender;
  }

  @JsonValue
  public String getValue() { return value; }

  public boolean shouldIncludeSender() {
    return includeSender;
  }

  @JsonCreator
  public static CollaborationEventType fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (CollaborationEventType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    return null;
  }

}
