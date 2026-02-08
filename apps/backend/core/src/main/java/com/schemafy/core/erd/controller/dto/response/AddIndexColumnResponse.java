package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.domain.erd.index.application.port.in.AddIndexColumnResult;
import com.schemafy.domain.erd.index.domain.type.SortDirection;

public record AddIndexColumnResponse(
    String id,
    String indexId,
    String columnId,
    int seqNo,
    SortDirection sortDirection) {

  public static AddIndexColumnResponse from(AddIndexColumnResult result) {
    return new AddIndexColumnResponse(
        result.indexColumnId(),
        result.indexId(),
        result.columnId(),
        result.seqNo(),
        result.sortDirection());
  }

}
