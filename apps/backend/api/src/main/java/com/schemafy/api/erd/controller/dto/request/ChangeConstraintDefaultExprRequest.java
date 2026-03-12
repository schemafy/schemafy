package com.schemafy.api.erd.controller.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

public record ChangeConstraintDefaultExprRequest(
    JsonNullable<String> defaultExpr) {

  public ChangeConstraintDefaultExprRequest {
    if (defaultExpr == null) {
      defaultExpr = JsonNullable.undefined();
    }
  }

}
