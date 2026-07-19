package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record ChangeColumnMetaInverse(
    String columnId,
    Boolean oldAutoIncrement,
    String oldCharset,
    String oldCollation,
    String oldComment,
    List<FkColumnMetaRevert> fkRevertList) implements InversePayload {

  public ChangeColumnMetaInverse {
    fkRevertList = fkRevertList == null
        ? List.of()
        : List.copyOf(fkRevertList);
  }

  public record FkColumnMetaRevert(
      String columnId,
      String oldCharset,
      String oldCollation) {

  }

}
