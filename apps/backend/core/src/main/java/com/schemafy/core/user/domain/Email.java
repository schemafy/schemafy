package com.schemafy.core.user.domain;

import java.util.Locale;

public record Email(String address) {

  public static Email from(String address) {
    return new Email(address);
  }

  public Email {
    address = normalizeAddress(address);

    if (address == null
        || !address.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
      throw new IllegalArgumentException("Invalid email format");
    }
  }

  private static String normalizeAddress(String address) {
    return address == null ? null : address.toLowerCase(Locale.ROOT);
  }

}
