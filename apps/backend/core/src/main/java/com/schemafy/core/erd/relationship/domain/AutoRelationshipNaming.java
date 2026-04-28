package com.schemafy.core.erd.relationship.domain;

import java.util.OptionalInt;

public final class AutoRelationshipNaming {

  private AutoRelationshipNaming() {}

  public static String buildBaseName(String fkTableName, String pkTableName) {
    String name = "rel_" + fkTableName + "_to_" + pkTableName;
    return name.trim();
  }

  public static OptionalInt parseAutoSuffix(String relationshipName, String baseName) {
    if (relationshipName == null || baseName == null) {
      return OptionalInt.empty();
    }
    if (relationshipName.equals(baseName)) {
      return OptionalInt.of(0);
    }
    String prefix = baseName + "_";
    if (!relationshipName.startsWith(prefix)) {
      return OptionalInt.empty();
    }
    String suffix = relationshipName.substring(prefix.length());
    if (suffix.isEmpty()) {
      return OptionalInt.empty();
    }
    for (int index = 0; index < suffix.length(); index++) {
      if (!Character.isDigit(suffix.charAt(index))) {
        return OptionalInt.empty();
      }
    }
    try {
      return OptionalInt.of(Integer.parseInt(suffix));
    } catch (NumberFormatException exception) {
      return OptionalInt.empty();
    }
  }

  public static boolean matches(String relationshipName, String baseName) {
    return parseAutoSuffix(relationshipName, baseName).isPresent();
  }

}
