package com.schemafy.api.erd.controller.dto.response;

import com.schemafy.core.erd.index.domain.IndexColumn;
import com.schemafy.core.erd.index.domain.type.SortDirection;

public record IndexColumnResponse(
    String id,
    String indexId,
    String columnId,
    int seqNo,
    SortDirection sortDirection) {

  public static IndexColumnResponse from(IndexColumn column) {
    return new IndexColumnResponse(
        column.id(),
        column.indexId(),
        column.columnId(),
        column.seqNo(),
        column.sortDirection());
  }

}
