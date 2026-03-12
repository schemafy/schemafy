package com.schemafy.core.erd.index.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.application.port.in.GetIndexColumnQuery;
import com.schemafy.core.erd.index.application.port.in.GetIndexColumnUseCase;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.core.erd.index.domain.IndexColumn;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetIndexColumnService implements GetIndexColumnUseCase {

  private final GetIndexColumnByIdPort getIndexColumnByIdPort;

  @Override
  public Mono<IndexColumn> getIndexColumn(GetIndexColumnQuery query) {
    return getIndexColumnByIdPort.findIndexColumnById(query.indexColumnId())
        .switchIfEmpty(Mono.error(
            new DomainException(
                IndexErrorCode.COLUMN_NOT_FOUND,
                "Index column not found: " + query.indexColumnId())));
  }

}
