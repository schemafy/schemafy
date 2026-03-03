package com.schemafy.domain.common.exception;

import org.springframework.http.HttpStatus;

public interface DomainErrorCode {

  HttpStatus getStatus();

  default HttpStatus status() {
    return getStatus();
  }

  default String code() {
    if (this instanceof Enum<?> e) {
      String prefix = e.getDeclaringClass().getSimpleName()
          .replace("ErrorCode", "");
      return camelToScreamingSnake(prefix) + "_" + e.name();
    }
    throw new IllegalStateException(
        "DomainErrorCode must be implemented by an enum");
  }

  private static String camelToScreamingSnake(String camel) {
    return camel.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
  }

}
