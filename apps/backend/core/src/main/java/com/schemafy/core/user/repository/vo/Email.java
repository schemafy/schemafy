package com.schemafy.core.user.repository.vo;

public record Email(String address) {

  public Email {
    if (address == null
        || !address.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
      throw new IllegalArgumentException("Invalid email format");
    }
  }

}
