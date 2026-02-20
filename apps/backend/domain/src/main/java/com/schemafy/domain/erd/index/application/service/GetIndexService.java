package com.schemafy.domain.erd.index.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.index.application.port.in.GetIndexQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexUseCase;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.domain.Index;
import com.schemafy.domain.erd.index.domain.exception.IndexErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetIndexService implements GetIndexUseCase {

  private final GetIndexByIdPort getIndexByIdPort;

  @Override
  public Mono<Index> getIndex(GetIndexQuery query) {
    return getIndexByIdPort.findIndexById(query.indexId())
        .switchIfEmpty(Mono.error(
            new DomainException(
                IndexErrorCode.NOT_FOUND, "Index not found: " + query.indexId())));
  }

}
