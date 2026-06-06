package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

import com.schemafy.core.erd.column.domain.ColumnTypeArguments;

public record ChangeColumnTypeInverse(
    String columnId,
    String oldDataType,
    ColumnTypeArguments oldTypeArguments,
    List<FkColumnTypeRevert> fkRevertList) implements InversePayload {

  public ChangeColumnTypeInverse {
    fkRevertList = fkRevertList == null
        ? List.of()
        : List.copyOf(fkRevertList);
  }

  public record FkColumnTypeRevert(
      String columnId,
      String oldDataType,
      ColumnTypeArguments oldTypeArguments,
      String oldCharset,
      String oldCollation) {

  }

}
