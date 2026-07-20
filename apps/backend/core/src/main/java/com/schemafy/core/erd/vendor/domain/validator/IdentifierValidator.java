package com.schemafy.core.erd.vendor.domain.validator;

import com.schemafy.core.common.exception.DomainErrorCode;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;

public final class IdentifierValidator {

  private IdentifierValidator() {}

  public static void validateLength(
      IdentifierCapabilities capabilities,
      String value,
      DomainErrorCode errorCode,
      String subject) {
    if (!capabilities.allows(value)) {
      throw new DomainException(
          errorCode,
          "%s must be at most %d %s for the project's DB vendor"
              .formatted(
                  subject,
                  capabilities.maxLength(),
                  capabilities.lengthUnit().displayName()));
    }
  }

}
