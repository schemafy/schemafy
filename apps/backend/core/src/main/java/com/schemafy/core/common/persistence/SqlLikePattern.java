package com.schemafy.core.common.persistence;

import java.util.Locale;

public final class SqlLikePattern {

  private SqlLikePattern() {}

  public static String contains(String value) {
    String escaped = value.toLowerCase(Locale.ROOT)
        .replace("!", "!!")
        .replace("%", "!%")
        .replace("_", "!_");
    return "%" + escaped + "%";
  }

}
