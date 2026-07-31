package com.schemafy.core.erd.table.application.service;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonObjectMetadataConverter;
import com.schemafy.core.erd.operation.application.inverse.ChangeTableExtraInverse;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraUseCase;
import com.schemafy.core.erd.table.application.port.out.ChangeTableExtraPort;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.TABLE;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = TABLE, id = "tableId"))
public class ChangeTableExtraService implements ChangeTableExtraUseCase {

  private final ChangeTableExtraPort changeTableExtraPort;
  private final GetTableByIdPort getTableByIdPort;
  private final JsonObjectMetadataConverter jsonObjectMetadataConverter;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeTableExtra(ChangeTableExtraCommand command) {
    return Mono.defer(() -> {
      String canonicalExtra = jsonObjectMetadataConverter.toStorageJson(command.extra());
      return getTableByIdPort.findTableById(command.tableId())
          .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Table not found")))
          .flatMap(table -> {
            if (Objects.equals(table.extra(), canonicalExtra)) {
              return Mono.just(MutationResult.<Void>noop(null, table.id()));
            }
            return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_TABLE_EXTRA, command,
                () -> getTableByIdPort.findTableById(command.tableId())
                    .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Table not found")))
                    .flatMap(lockedTable -> {
                      if (Objects.equals(lockedTable.extra(), canonicalExtra)) {
                        return Mono.just(MutationResult.<Void>noop(null, lockedTable.id()));
                      }
                      return changeTableExtraPort
                          .changeTableExtra(command.tableId(), canonicalExtra)
                          .thenReturn(MutationResult.<Void>of(null, lockedTable.id())
                              .withInverse(new ChangeTableExtraInverse(
                                  lockedTable.id(),
                                  lockedTable.extra())));
                    }));
          });
    });
  }

}
