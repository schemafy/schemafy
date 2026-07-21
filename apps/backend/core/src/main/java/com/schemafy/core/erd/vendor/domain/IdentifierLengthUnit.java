package com.schemafy.core.erd.vendor.domain;

import java.nio.charset.StandardCharsets;

public enum IdentifierLengthUnit {

  CODE_POINTS("Unicode code points") {

    @Override
    public int measure(String value) {
      return value.codePointCount(0, value.length());
    }

  },
  UTF8_BYTES("UTF-8 bytes") {

    @Override
    public int measure(String value) {
      return value.getBytes(StandardCharsets.UTF_8).length;
    }

  };

  private final String displayName;

  IdentifierLengthUnit(String displayName) {
    this.displayName = displayName;
  }

  public abstract int measure(String value);

  public String take(String value, int limit) {
    if (measure(value) <= limit) {
      return value;
    }

    int endIndex = 0;
    int measuredLength = 0;
    while (endIndex < value.length()) {
      int nextIndex = value.offsetByCodePoints(endIndex, 1);
      int nextLength = measure(value.substring(endIndex, nextIndex));
      if (measuredLength + nextLength > limit) {
        break;
      }
      measuredLength += nextLength;
      endIndex = nextIndex;
    }
    return value.substring(0, endIndex);
  }

  public String displayName() {
    return displayName;
  }

}
