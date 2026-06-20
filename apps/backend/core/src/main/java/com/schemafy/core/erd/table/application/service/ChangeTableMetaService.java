package com.schemafy.core.erd.table.application.service;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.table.application.port.in.ChangeTableMetaCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableMetaUseCase;
import com.schemafy.core.erd.table.application.port.out.ChangeTableMetaPort;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
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
public class ChangeTableMetaService implements ChangeTableMetaUseCase {

  private final ChangeTableMetaPort changeTableMetaPort;
  private final GetTableByIdPort getTableByIdPort;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeTableMeta(ChangeTableMetaCommand command) {
    String portCharset = command.charset().isPresent()
        ? normalizeForPort(command.charset().get())
        : null;
    String portCollation = command.collation().isPresent()
        ? normalizeForPort(command.collation().get())
        : null;

    return getTableByIdPort.findTableById(command.tableId())
        .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Table not found")))
        .flatMap(table -> {
          if (isNoOp(command, table)) {
            return Mono.just(MutationResult.<Void>of(null, table.id()));
          }
          return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_TABLE_META, command,
              () -> changeTableMetaPort.changeTableMeta(
                  command.tableId(),
                  portCharset,
                  portCollation)
                  .thenReturn(MutationResult.<Void>of(null, table.id())));
        });
  }

  private static boolean isNoOp(ChangeTableMetaCommand command, Table table) {
    String effectiveCharset = command.charset().isPresent()
        ? normalizeOptional(command.charset().get())
        : table.charset();
    String effectiveCollation = command.collation().isPresent()
        ? normalizeOptional(command.collation().get())
        : table.collation();
    return Objects.equals(table.charset(), effectiveCharset)
        && Objects.equals(table.collation(), effectiveCollation);
  }

  private static String normalizeForPort(String value) {
    return Objects.toString(normalizeOptional(value), "");
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

}
