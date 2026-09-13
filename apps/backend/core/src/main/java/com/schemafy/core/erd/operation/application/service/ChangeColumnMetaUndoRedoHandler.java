package com.schemafy.core.erd.operation.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnMetaPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.application.service.DatatypePolicyColumnValidator;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnMetaInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnMetaInverse.FkColumnMetaRevert;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.vendor.application.service.DatatypePolicyResolver;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicy;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.COLUMN;

@Component
class ChangeColumnMetaUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeColumnMetaInverse> {

  private final ChangeColumnMetaPort changeColumnMetaPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final DatatypePolicyResolver datatypePolicyResolver;

  ChangeColumnMetaUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeColumnMetaPort changeColumnMetaPort,
      GetColumnByIdPort getColumnByIdPort,
      DatatypePolicyResolver datatypePolicyResolver) {
    super(ErdOperationType.CHANGE_COLUMN_META, ChangeColumnMetaInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeColumnMetaPort = changeColumnMetaPort;
    this.getColumnByIdPort = getColumnByIdPort;
    this.datatypePolicyResolver = datatypePolicyResolver;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeColumnMetaInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getColumnByIdPort.findColumnById(inversePayload.columnId())
        .switchIfEmpty(Mono.error(new DomainException(
            ColumnErrorCode.NOT_FOUND,
            "Column not found: " + inversePayload.columnId())))
        .flatMap(column -> datatypePolicyResolver.resolve(COLUMN, column.id())
            .flatMap(datatypePolicy -> captureFkForwardSnapshot(inversePayload)
                .flatMap(fkSnapshot -> {
                  validateInverse(
                      datatypePolicy,
                      column,
                      inversePayload,
                      fkSnapshot.columns());
                  Set<String> affectedTableIds = new HashSet<>(fkSnapshot.affectedTableIds());
                  affectedTableIds.add(column.tableId());
                  ChangeColumnMetaInverse forwardSnapshot = new ChangeColumnMetaInverse(
                      column.id(),
                      inversePayload.oldAutoIncrement() == null
                          ? null
                          : column.autoIncrement(),
                      inversePayload.oldCharset() == null
                          ? null
                          : Objects.toString(column.charset(), ""),
                      inversePayload.oldCollation() == null
                          ? null
                          : Objects.toString(column.collation(), ""),
                      inversePayload.oldComment() == null
                          ? null
                          : Objects.toString(column.comment(), ""),
                      fkSnapshot.revertList());
                  return coordinate(resolved, inversePayload,
                      () -> applyColumnMetaInverse(inversePayload)
                          .thenReturn(MutationResult.<Void>of(null, affectedTableIds)
                              .withInverse(forwardSnapshot)));
                })));
  }

  private Mono<FkForwardSnapshot> captureFkForwardSnapshot(ChangeColumnMetaInverse inversePayload) {
    return Flux.fromIterable(inversePayload.fkRevertList())
        .concatMap(revert -> getColumnByIdPort.findColumnById(revert.columnId())
            .switchIfEmpty(Mono.error(new DomainException(
                ColumnErrorCode.NOT_FOUND,
                "Column not found: " + revert.columnId()))))
        .collectList()
        .map(columns -> new FkForwardSnapshot(
            columns,
            toForwardRevertList(inversePayload.fkRevertList(), columns),
            columns.stream()
                .map(Column::tableId)
                .collect(Collectors.toSet())));
  }

  private List<FkColumnMetaRevert> toForwardRevertList(
      List<FkColumnMetaRevert> inverseRevertList,
      List<Column> columns) {
    Map<String, FkColumnMetaRevert> inverseRevertByColumnId = inverseRevertList.stream()
        .collect(Collectors.toMap(
            FkColumnMetaRevert::columnId,
            revert -> revert,
            (first, ignored) -> first));
    return columns.stream()
        .map(column -> {
          FkColumnMetaRevert inverseRevert = inverseRevertByColumnId.get(column.id());
          return new FkColumnMetaRevert(
              column.id(),
              inverseRevert.oldCharset() == null
                  ? null
                  : Objects.toString(column.charset(), ""),
              inverseRevert.oldCollation() == null
                  ? null
                  : Objects.toString(column.collation(), ""));
        })
        .toList();
  }

  private void validateInverse(
      DatatypePolicy datatypePolicy,
      Column directColumn,
      ChangeColumnMetaInverse inversePayload,
      List<Column> fkColumns) {
    DatatypePolicyColumnValidator.validate(
        datatypePolicy,
        directColumn.dataType(),
        directColumn.typeArguments(),
        inversePayload.oldAutoIncrement() == null
            ? directColumn.autoIncrement()
            : inversePayload.oldAutoIncrement(),
        inversePayload.oldCharset() == null
            ? directColumn.charset()
            : normalizeMetaValue(inversePayload.oldCharset()),
        inversePayload.oldCollation() == null
            ? directColumn.collation()
            : normalizeMetaValue(inversePayload.oldCollation()));

    java.util.stream.IntStream.range(0, inversePayload.fkRevertList().size())
        .forEach(index -> validateFkTarget(
            datatypePolicy,
            fkColumns.get(index),
            inversePayload.fkRevertList().get(index)));
  }

  private void validateFkTarget(
      DatatypePolicy datatypePolicy,
      Column currentColumn,
      FkColumnMetaRevert revert) {
    DatatypePolicyColumnValidator.validate(
        datatypePolicy,
        currentColumn.dataType(),
        currentColumn.typeArguments(),
        currentColumn.autoIncrement(),
        revert.oldCharset() == null
            ? currentColumn.charset()
            : normalizeMetaValue(revert.oldCharset()),
        revert.oldCollation() == null
            ? currentColumn.collation()
            : normalizeMetaValue(revert.oldCollation()));
  }

  private Mono<Void> applyColumnMetaInverse(ChangeColumnMetaInverse inversePayload) {
    return changeColumnMetaPort.changeColumnMeta(
        inversePayload.columnId(),
        inversePayload.oldAutoIncrement(),
        inversePayload.oldCharset(),
        inversePayload.oldCollation(),
        inversePayload.oldComment())
        .then(Flux.fromIterable(inversePayload.fkRevertList())
            .concatMap(revert -> changeColumnMetaPort.changeColumnMeta(
                revert.columnId(),
                null,
                revert.oldCharset(),
                revert.oldCollation(),
                null))
            .then());
  }

  private static String normalizeMetaValue(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private record FkForwardSnapshot(
      List<Column> columns,
      List<FkColumnMetaRevert> revertList,
      Set<String> affectedTableIds) {

  }

}
