package com.schemafy.core.erd.index.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameUseCase;
import com.schemafy.core.erd.index.application.port.out.ChangeIndexNamePort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.application.port.out.IndexExistsPort;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.index.domain.validator.IndexValidator;
import com.schemafy.core.erd.operation.application.inverse.ChangeIndexNameInverse;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.INDEX;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = INDEX, id = "indexId"))
public class ChangeIndexNameService implements ChangeIndexNameUseCase {

  private final ChangeIndexNamePort changeIndexNamePort;
  private final IndexExistsPort indexExistsPort;
  private final GetIndexByIdPort getIndexByIdPort;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeIndexName(ChangeIndexNameCommand command) {
    return Mono.defer(() -> {
      String normalizedName = normalizeName(command.newName());
      IndexValidator.validateName(normalizedName);
      return getIndexByIdPort.findIndexById(command.indexId())
          .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.NOT_FOUND, "Index not found")))
          .flatMap(index -> {
            if (normalizedName.equals(index.name())) {
              return Mono.just(MutationResult.<Void>of(null, index.tableId()));
            }
            return indexExistsPort.existsByTableIdAndNameExcludingId(
                index.tableId(),
                normalizedName,
                index.id())
                .flatMap(exists -> {
                  if (exists) {
                    return Mono.error(new DomainException(
                        IndexErrorCode.NAME_DUPLICATE,
                        "Index name '%s' already exists in table".formatted(normalizedName)));
                  }
                  return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_INDEX_NAME, command,
                      () -> changeIndexNamePort
                          .changeIndexName(index.id(), normalizedName)
                          .thenReturn(MutationResult.<Void>of(null, index.tableId())
                              .withInverse(new ChangeIndexNameInverse(index.id(), index.name()))));
                });
          });
    });
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

}
