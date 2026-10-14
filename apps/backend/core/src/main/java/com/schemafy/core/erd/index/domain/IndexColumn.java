package com.schemafy.core.erd.index.domain;

import com.schemafy.core.erd.index.domain.type.SortDirection;

public record IndexColumn(
    String id,
    String indexId,
    String columnId,
    int seqNo,
    SortDirection sortDirection) {

  public IndexColumn withSeqNo(int nextSeqNo) {
    return new IndexColumn(
        id,
        indexId,
        columnId,
        nextSeqNo,
        sortDirection);
  }

}
