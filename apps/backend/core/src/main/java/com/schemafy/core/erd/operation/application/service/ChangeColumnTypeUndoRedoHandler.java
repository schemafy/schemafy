package com.schemafy.core.erd.operation.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnMetaPort;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnTypePort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnTypeInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnTypeInverse.FkColumnTypeRevert;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
class ChangeColumnTypeUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeColumnTypeInverse> {

  private final ChangeColumnTypePort changeColumnTypePort;
  private final ChangeColumnMetaPort changeColumnMetaPort;
  private final GetColumnByIdPort getColumnByIdPort;

  ChangeColumnTypeUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeColumnTypePort changeColumnTypePort,
      ChangeColumnMetaPort changeColumnMetaPort,
      GetColumnByIdPort getColumnByIdPort) {
    super(ErdOperationType.CHANGE_COLUMN_TYPE, ChangeColumnTypeInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeColumnTypePort = changeColumnTypePort;
    this.changeColumnMetaPort = changeColumnMetaPort;
    this.getColumnByIdPort = getColumnByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeColumnTypeInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getColumnByIdPort.findColumnById(inversePayload.columnId())
        .switchIfEmpty(Mono.error(new DomainException(
            ColumnErrorCode.NOT_FOUND,
            "Column not found: " + inversePayload.columnId())))
        .flatMap(column -> captureFkForwardSnapshot(inversePayload)
            .flatMap(fkSnapshot -> {
              ChangeColumnTypeInverse forwardSnapshot = new ChangeColumnTypeInverse(
                  column.id(),
                  column.dataType(),
                  column.typeArguments(),
                  fkSnapshot.revertList());
              Set<String> affectedTableIds = new HashSet<>(fkSnapshot.affectedTableIds());
              affectedTableIds.add(column.tableId());
              return coordinate(resolved, inversePayload,
                  () -> applyColumnTypeInverse(inversePayload)
                      .thenReturn(MutationResult.<Void>of(null, affectedTableIds)
                          .withInverse(forwardSnapshot)));
            }));
  }

  private Mono<FkForwardSnapshot> captureFkForwardSnapshot(ChangeColumnTypeInverse inversePayload) {
    return Flux.fromIterable(inversePayload.fkRevertList())
        .concatMap(revert -> getColumnByIdPort.findColumnById(revert.columnId())
            .switchIfEmpty(Mono.error(new DomainException(
                ColumnErrorCode.NOT_FOUND,
                "Column not found: " + revert.columnId()))))
        .collectList()
        .map(columns -> new FkForwardSnapshot(
            columns.stream()
                .map(this::toFkColumnTypeRevert)
                .toList(),
            columns.stream()
                .map(Column::tableId)
                .collect(Collectors.toSet())));
  }

  private Mono<Void> applyColumnTypeInverse(ChangeColumnTypeInverse inversePayload) {
    return changeColumnTypePort.changeColumnType(
        inversePayload.columnId(),
        inversePayload.oldDataType(),
        inversePayload.oldTypeArguments())
        .then(Flux.fromIterable(inversePayload.fkRevertList())
            .concatMap(this::applyFkColumnTypeInverse)
            .then());
  }

  private Mono<Void> applyFkColumnTypeInverse(FkColumnTypeRevert revert) {
    return changeColumnTypePort.changeColumnType(
        revert.columnId(),
        revert.oldDataType(),
        revert.oldTypeArguments())
        .then(changeColumnMetaPort.changeColumnMeta(
            revert.columnId(),
            null,
            nullableMetaValue(revert.oldCharset()),
            nullableMetaValue(revert.oldCollation()),
            null));
  }

  private FkColumnTypeRevert toFkColumnTypeRevert(Column column) {
    return new FkColumnTypeRevert(
        column.id(),
        column.dataType(),
        column.typeArguments(),
        column.charset(),
        column.collation());
  }

  private static String nullableMetaValue(String value) {
    return value == null ? "" : value;
  }

  private record FkForwardSnapshot(
      List<FkColumnTypeRevert> revertList,
      Set<String> affectedTableIds) {

  }

}
