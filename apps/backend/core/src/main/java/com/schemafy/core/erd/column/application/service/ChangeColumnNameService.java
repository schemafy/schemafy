package com.schemafy.core.erd.column.application.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameUseCase;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnNamePort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.column.domain.validator.ColumnValidator;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnNameInverse;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorUseCase;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;
import com.schemafy.core.erd.vendor.domain.validator.IdentifierValidator;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.COLUMN;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = COLUMN, id = "columnId"))
public class ChangeColumnNameService implements ChangeColumnNameUseCase {

  private final ChangeColumnNamePort changeColumnNamePort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetSchemaByIdPort getSchemaByIdPort;
  private final GetProjectDbVendorUseCase getProjectDbVendorUseCase;
  private final TransactionalOperator transactionalOperator;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeColumnName(ChangeColumnNameCommand command) {
    return Mono.defer(() -> {
      String normalizedName = normalizeName(command.newName());
      ColumnValidator.validateName(normalizedName);
      return getColumnByIdPort.findColumnById(command.columnId())
          .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found")))
          .flatMap(column -> {
            if (normalizedName.equals(column.name())) {
              return Mono.just(MutationResult.<Void>noop(null, column.tableId()));
            }
            return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_COLUMN_NAME, command,
                () -> getColumnByIdPort.findColumnById(command.columnId())
                    .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found")))
                    .flatMap(lockedColumn -> {
                      if (normalizedName.equals(lockedColumn.name())) {
                        return Mono.just(MutationResult.<Void>noop(null, lockedColumn.tableId()));
                      }
                      return fetchTableSchemaAndColumns(lockedColumn)
                          .flatMap(tuple -> getProjectDbVendorUseCase
                              .getProjectDbVendor(new GetProjectDbVendorQuery(
                                  tuple.getT2().projectId()))
                              .flatMap(dbVendor -> {
                                validateNameChange(tuple, dbVendor.name(),
                                    dbVendor.capabilities().identifiers(),
                                    normalizedName, lockedColumn.id());
                                return changeColumnNamePort
                                    .changeColumnName(lockedColumn.id(), normalizedName)
                                    .thenReturn(MutationResult.<Void>of(null,
                                        lockedColumn.tableId())
                                        .withInverse(new ChangeColumnNameInverse(
                                            lockedColumn.id(),
                                            lockedColumn.name())));
                              }));
                    }));
          });
    }).as(transactionalOperator::transactional);
  }

  private Mono<Tuple3<Table, Schema, List<Column>>> fetchTableSchemaAndColumns(Column column) {
    Mono<Table> tableMono = getTableByIdPort.findTableById(column.tableId())
        .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Table not found")));

    return tableMono.flatMap(table -> {
      Mono<Schema> schemaMono = getSchemaByIdPort.findSchemaById(table.schemaId())
          .switchIfEmpty(Mono.error(new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found")));
      Mono<List<Column>> columnsMono = getColumnsByTableIdPort.findColumnsByTableId(column.tableId())
          .defaultIfEmpty(List.of());
      return Mono.zip(Mono.just(table), schemaMono, columnsMono);
    });
  }

  private void validateNameChange(
      Tuple3<Table, Schema, List<Column>> tuple,
      String dbVendorName,
      IdentifierCapabilities identifierCapabilities,
      String normalizedName,
      String columnId) {
    List<Column> columns = tuple.getT3();

    IdentifierValidator.validateLength(
        identifierCapabilities,
        normalizedName,
        ColumnErrorCode.NAME_INVALID,
        "Column name");
    ColumnValidator.validateReservedKeyword(dbVendorName, normalizedName);
    ColumnValidator.validateNameUniqueness(columns, normalizedName, columnId);
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

}
