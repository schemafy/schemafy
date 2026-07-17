package com.schemafy.core.erd.operation.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnMetaPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnMetaInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnMetaInverse.FkColumnMetaRevert;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
class ChangeColumnMetaUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeColumnMetaInverse> {

  private final ChangeColumnMetaPort changeColumnMetaPort;
  private final GetColumnByIdPort getColumnByIdPort;

  ChangeColumnMetaUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeColumnMetaPort changeColumnMetaPort,
      GetColumnByIdPort getColumnByIdPort) {
    super(ErdOperationType.CHANGE_COLUMN_META, ChangeColumnMetaInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeColumnMetaPort = changeColumnMetaPort;
    this.getColumnByIdPort = getColumnByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeColumnMetaInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getColumnByIdPort.findColumnById(inversePayload.columnId())
        .switchIfEmpty(Mono.error(new DomainException(
            ColumnErrorCode.NOT_FOUND,
            "Column not found: " + inversePayload.columnId())))
        .flatMap(column -> captureFkForwardSnapshot(inversePayload)
            .flatMap(fkSnapshot -> {
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
            }));
  }

  private Mono<FkForwardSnapshot> captureFkForwardSnapshot(ChangeColumnMetaInverse inversePayload) {
    return Flux.fromIterable(inversePayload.fkRevertList())
        .concatMap(revert -> getColumnByIdPort.findColumnById(revert.columnId())
            .switchIfEmpty(Mono.error(new DomainException(
                ColumnErrorCode.NOT_FOUND,
                "Column not found: " + revert.columnId()))))
        .collectList()
        .map(columns -> new FkForwardSnapshot(
            toForwardRevertList(inversePayload.fkRevertList(), columns),
            columns.stream()
                .map(Column::tableId)
                .collect(Collectors.toSet())));
  }

  private List<FkColumnMetaRevert> toForwardRevertList(
      List<FkColumnMetaRevert> inverseRevertList,
      List<Column> columns) {
    return columns.stream()
        .map(column -> {
          FkColumnMetaRevert inverseRevert = inverseRevertList.stream()
              .filter(revert -> revert.columnId().equals(column.id()))
              .findFirst()
              .orElseThrow();
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

  private record FkForwardSnapshot(
      List<FkColumnMetaRevert> revertList,
      Set<String> affectedTableIds) {

  }

}
