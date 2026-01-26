package com.schemafy.domain.erd.index.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexTypeUseCase;
import com.schemafy.domain.erd.index.application.port.out.ChangeIndexTypePort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.domain.erd.index.domain.Index;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.exception.IndexNotExistException;
import com.schemafy.domain.erd.index.domain.validator.IndexValidator;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ChangeIndexTypeService implements ChangeIndexTypeUseCase {

  private final ChangeIndexTypePort changeIndexTypePort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final GetIndexesByTableIdPort getIndexesByTableIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  public ChangeIndexTypeService(
      ChangeIndexTypePort changeIndexTypePort,
      GetIndexByIdPort getIndexByIdPort,
      GetIndexesByTableIdPort getIndexesByTableIdPort,
      GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort) {
    this.changeIndexTypePort = changeIndexTypePort;
    this.getIndexByIdPort = getIndexByIdPort;
    this.getIndexesByTableIdPort = getIndexesByTableIdPort;
    this.getIndexColumnsByIndexIdPort = getIndexColumnsByIndexIdPort;
  }

  @Override
  public Mono<Void> changeIndexType(ChangeIndexTypeCommand command) {
    return Mono.defer(() -> {
      IndexValidator.validateType(command.type());
      return getIndexByIdPort.findIndexById(command.indexId())
          .switchIfEmpty(Mono.error(new IndexNotExistException("Index not found")))
          .flatMap(index -> getIndexesByTableIdPort.findIndexesByTableId(index.tableId())
              .defaultIfEmpty(List.of())
              .flatMap(indexes -> fetchIndexColumns(indexes)
                  .flatMap(indexColumnsByIndexId -> getIndexColumnsByIndexIdPort
                      .findIndexColumnsByIndexId(index.id())
                      .defaultIfEmpty(List.of())
                      .flatMap(columns -> {
                        IndexValidator.validateDefinitionUniqueness(
                            indexes,
                            indexColumnsByIndexId,
                            command.type(),
                            columns,
                            index.name(),
                            index.id());
                        return changeIndexTypePort
                            .changeIndexType(index.id(), command.type());
                      }))));
    });
  }

  private Mono<Map<String, List<IndexColumn>>> fetchIndexColumns(List<Index> indexes) {
    if (indexes == null || indexes.isEmpty()) {
      return Mono.just(Map.of());
    }
    return Flux.fromIterable(indexes)
        .flatMap(index -> getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(index.id())
            .defaultIfEmpty(List.of())
            .map(columns -> Map.entry(index.id(), columns)))
        .collectMap(Map.Entry::getKey, Map.Entry::getValue);
  }

}
