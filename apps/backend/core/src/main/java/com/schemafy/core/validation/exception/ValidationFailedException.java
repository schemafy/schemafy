package com.schemafy.core.validation.exception;

import java.util.List;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;

import lombok.Getter;

@Getter
public class ValidationFailedException extends BusinessException {

  private final List<ValidationError> validationErrors;

  public ValidationFailedException(List<ValidationError> validationErrors) {
    super(ErrorCode.VALIDATION_FAILED);
    this.validationErrors = validationErrors;
  }

  public record ValidationError(String code, String message) {
  }

}
