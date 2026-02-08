package com.schemafy.core.erd.controller.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

public record ChangeConstraintCheckExprRequest(
    JsonNullable<String> checkExpr) {

  public ChangeConstraintCheckExprRequest {
    if (checkExpr == null) {
      checkExpr = JsonNullable.undefined();
    }
  }

}
