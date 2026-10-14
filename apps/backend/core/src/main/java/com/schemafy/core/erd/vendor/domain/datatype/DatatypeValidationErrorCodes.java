package com.schemafy.core.erd.vendor.domain.datatype;

import java.util.Objects;

import com.schemafy.core.common.exception.DomainErrorCode;

public record DatatypeValidationErrorCodes(
    DomainErrorCode unsupportedType,
    DomainErrorCode requiredLength,
    DomainErrorCode requiredPrecision,
    DomainErrorCode invalidArgument,
    DomainErrorCode autoIncrementNotAllowed,
    DomainErrorCode charsetNotAllowed) {

  public DatatypeValidationErrorCodes {
    Objects.requireNonNull(unsupportedType, "unsupportedType");
    Objects.requireNonNull(requiredLength, "requiredLength");
    Objects.requireNonNull(requiredPrecision, "requiredPrecision");
    Objects.requireNonNull(invalidArgument, "invalidArgument");
    Objects.requireNonNull(autoIncrementNotAllowed, "autoIncrementNotAllowed");
    Objects.requireNonNull(charsetNotAllowed, "charsetNotAllowed");
  }

  public static DatatypeValidationErrorCodes all(DomainErrorCode errorCode) {
    return new DatatypeValidationErrorCodes(
        errorCode,
        errorCode,
        errorCode,
        errorCode,
        errorCode,
        errorCode);
  }

}
