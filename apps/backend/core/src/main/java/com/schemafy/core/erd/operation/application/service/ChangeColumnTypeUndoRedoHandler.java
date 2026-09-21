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
import com.schemafy.core.erd.column.application.service.DatatypePolicyColumnValidator;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnTypeInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnTypeInverse.FkColumnTypeRevert;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.vendor.application.service.DatatypePolicyResolver;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypeDefinition;
import com.schemafy.core.erd.vendor.domain.datatype.DatatypePolicy;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.COLUMN;

@Component
class ChangeColumnTypeUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeColumnTypeInverse> {

  private final ChangeColumnTypePort changeColumnTypePort;
  private final ChangeColumnMetaPort changeColumnMetaPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final DatatypePolicyResolver datatypePolicyResolver;

  ChangeColumnTypeUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeColumnTypePort changeColumnTypePort,
      ChangeColumnMetaPort changeColumnMetaPort,
      GetColumnByIdPort getColumnByIdPort,
      DatatypePolicyResolver datatypePolicyResolver) {
    super(ErdOperationType.CHANGE_COLUMN_TYPE, ChangeColumnTypeInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeColumnTypePort = changeColumnTypePort;
    this.changeColumnMetaPort = changeColumnMetaPort;
    this.getColumnByIdPort = getColumnByIdPort;
    this.datatypePolicyResolver = datatypePolicyResolver;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeColumnTypeInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getColumnByIdPort.findColumnById(inversePayload.columnId())
        .switchIfEmpty(Mono.error(new DomainException(
            ColumnErrorCode.NOT_FOUND,
            "Column not found: " + inversePayload.columnId())))
        .flatMap(column -> datatypePolicyResolver.resolve(COLUMN, column.id())
            .flatMap(datatypePolicy -> captureFkForwardSnapshot(inversePayload)
                .flatMap(fkSnapshot -> {
                  ValidatedTypeInverse validated = validateInverse(
                      datatypePolicy,
                      column,
                      inversePayload,
                      fkSnapshot.columns());
                  ChangeColumnTypeInverse forwardSnapshot = new ChangeColumnTypeInverse(
                      column.id(),
                      column.dataType(),
                      column.typeArguments(),
                      column.charset(),
                      column.collation(),
                      fkSnapshot.revertList());
                  Set<String> affectedTableIds = new HashSet<>(fkSnapshot.affectedTableIds());
                  affectedTableIds.add(column.tableId());
                  return coordinate(resolved, inversePayload,
                      () -> applyColumnTypeInverse(validated)
                          .thenReturn(MutationResult.<Void>of(null, affectedTableIds)
                              .withInverse(forwardSnapshot)));
                })));
  }

  private Mono<FkForwardSnapshot> captureFkForwardSnapshot(ChangeColumnTypeInverse inversePayload) {
    return Flux.fromIterable(inversePayload.fkRevertList())
        .concatMap(revert -> getColumnByIdPort.findColumnById(revert.columnId())
            .switchIfEmpty(Mono.error(new DomainException(
                ColumnErrorCode.NOT_FOUND,
                "Column not found: " + revert.columnId()))))
        .collectList()
        .map(columns -> new FkForwardSnapshot(
            columns,
            columns.stream()
                .map(this::toFkColumnTypeRevert)
                .toList(),
            columns.stream()
                .map(Column::tableId)
                .collect(Collectors.toSet())));
  }

  private ValidatedTypeInverse validateInverse(
      DatatypePolicy datatypePolicy,
      Column directColumn,
      ChangeColumnTypeInverse inversePayload,
      List<Column> fkColumns) {
    DatatypeDefinition directDatatype = DatatypePolicyColumnValidator.validate(
        datatypePolicy,
        inversePayload.oldDataType(),
        inversePayload.oldTypeArguments(),
        directColumn.autoIncrement(),
        inversePayload.oldCharset(),
        inversePayload.oldCollation());
    List<ValidatedFkTypeTarget> fkTargets = java.util.stream.IntStream
        .range(0, inversePayload.fkRevertList().size())
        .mapToObj(index -> validateFkTarget(
            datatypePolicy,
            fkColumns.get(index),
            inversePayload.fkRevertList().get(index)))
        .toList();
    return new ValidatedTypeInverse(
        directColumn.id(),
        directDatatype.sqlType(),
        inversePayload.oldTypeArguments(),
        inversePayload.oldCharset(),
        inversePayload.oldCollation(),
        fkTargets);
  }

  private ValidatedFkTypeTarget validateFkTarget(
      DatatypePolicy datatypePolicy,
      Column currentColumn,
      FkColumnTypeRevert revert) {
    DatatypeDefinition datatype = DatatypePolicyColumnValidator.validate(
        datatypePolicy,
        revert.oldDataType(),
        revert.oldTypeArguments(),
        currentColumn.autoIncrement(),
        revert.oldCharset(),
        revert.oldCollation());
    return new ValidatedFkTypeTarget(
        revert.columnId(),
        datatype.sqlType(),
        revert.oldTypeArguments(),
        revert.oldCharset(),
        revert.oldCollation());
  }

  private Mono<Void> applyColumnTypeInverse(ValidatedTypeInverse inverse) {
    return changeColumnTypePort.changeColumnType(
        inverse.columnId(),
        inverse.dataType(),
        inverse.typeArguments())
        .then(changeColumnMetaPort.changeColumnMeta(
            inverse.columnId(),
            null,
            nullableMetaValue(inverse.charset()),
            nullableMetaValue(inverse.collation()),
            null))
        .then(Flux.fromIterable(inverse.fkTargets())
            .concatMap(this::applyFkColumnTypeInverse)
            .then());
  }

  private Mono<Void> applyFkColumnTypeInverse(ValidatedFkTypeTarget revert) {
    return changeColumnTypePort.changeColumnType(
        revert.columnId(),
        revert.dataType(),
        revert.typeArguments())
        .then(changeColumnMetaPort.changeColumnMeta(
            revert.columnId(),
            null,
            nullableMetaValue(revert.charset()),
            nullableMetaValue(revert.collation()),
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
      List<Column> columns,
      List<FkColumnTypeRevert> revertList,
      Set<String> affectedTableIds) {

  }

  private record ValidatedTypeInverse(
      String columnId,
      String dataType,
      ColumnTypeArguments typeArguments,
      String charset,
      String collation,
      List<ValidatedFkTypeTarget> fkTargets) {
  }

  private record ValidatedFkTypeTarget(
      String columnId,
      String dataType,
      ColumnTypeArguments typeArguments,
      String charset,
      String collation) {
  }

}
