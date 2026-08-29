package com.schemafy.core.erd.column.application.service;

import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypeDefinition;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicy;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypeValidationErrorCodes;

public final class DatatypePolicyColumnValidator {

  private static final DatatypeValidationErrorCodes ERROR_CODES = new DatatypeValidationErrorCodes(
      ColumnErrorCode.DATA_TYPE_INVALID,
      ColumnErrorCode.LENGTH_REQUIRED,
      ColumnErrorCode.PRECISION_REQUIRED,
      ColumnErrorCode.INVALID_VALUE,
      ColumnErrorCode.AUTO_INCREMENT_NOT_ALLOWED,
      ColumnErrorCode.CHARSET_NOT_ALLOWED);

  private DatatypePolicyColumnValidator() {}

  public static DatatypeDefinition validate(
      DatatypePolicy policy,
      String dataType,
      ColumnTypeArguments typeArguments,
      boolean autoIncrement,
      String charset,
      String collation) {
    return policy.validate(
        dataType,
        typeArguments,
        autoIncrement,
        charset,
        collation,
        ERROR_CODES);
  }

}
