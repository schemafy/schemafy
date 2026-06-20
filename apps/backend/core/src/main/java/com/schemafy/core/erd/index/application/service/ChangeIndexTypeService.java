package com.schemafy.core.erd.index.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeUseCase;
import com.schemafy.core.erd.index.application.port.out.ChangeIndexTypePort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.core.erd.index.domain.Index;
import com.schemafy.core.erd.index.domain.IndexColumn;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.index.domain.validator.IndexValidator;
import com.schemafy.core.erd.operation.application.inverse.ChangeIndexTypeInverse;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.INDEX;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = INDEX, id = "indexId"))
public class ChangeIndexTypeService implements ChangeIndexTypeUseCase {

  private final ChangeIndexTypePort changeIndexTypePort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final GetIndexesByTableIdPort getIndexesByTableIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeIndexType(ChangeIndexTypeCommand command) {
    return Mono.defer(() -> {
      IndexValidator.validateType(command.type());
      return getIndexByIdPort.findIndexById(command.indexId())
          .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.NOT_FOUND, "Index not found")))
          .flatMap(index -> {
            if (index.type() == command.type()) {
              return Mono.just(MutationResult.<Void>of(null, index.tableId()));
            }
            return getIndexesByTableIdPort.findIndexesByTableId(index.tableId())
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
                          return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_INDEX_TYPE, command,
                              () -> changeIndexTypePort
                                  .changeIndexType(index.id(), command.type())
                                  .thenReturn(MutationResult.<Void>of(null, index.tableId())
                                      .withInverse(new ChangeIndexTypeInverse(index.id(), index.type()))));
                        })));
          });
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
