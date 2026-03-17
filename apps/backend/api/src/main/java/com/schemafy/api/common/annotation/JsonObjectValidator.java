package com.schemafy.api.common.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonObjectValidator implements ConstraintValidator<JsonObject, JsonNode> {

  private boolean nullable;

  @Override
  public void initialize(JsonObject constraintAnnotation) {
    nullable = constraintAnnotation.nullable();
  }

  @Override
  public boolean isValid(JsonNode value, ConstraintValidatorContext context) {
    if (value == null || value.isNull()) {
      return nullable;
    }
    return value.isObject();
  }

}
