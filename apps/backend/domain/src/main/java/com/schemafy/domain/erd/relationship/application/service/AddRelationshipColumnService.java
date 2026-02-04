package com.schemafy.domain.erd.relationship.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.relationship.application.port.in.AddRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.in.AddRelationshipColumnResult;
import com.schemafy.domain.erd.relationship.application.port.in.AddRelationshipColumnUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.CreateRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipTargetTableNotExistException;
import com.schemafy.domain.erd.relationship.domain.validator.RelationshipValidator;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AddRelationshipColumnService implements AddRelationshipColumnUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateRelationshipColumnPort createRelationshipColumnPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;

  @Override
  public Mono<MutationResult<AddRelationshipColumnResult>> addRelationshipColumn(
      AddRelationshipColumnCommand command) {
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new RelationshipNotExistException("Relationship not found")))
        .flatMap(relationship -> {
          Set<String> affectedTableIds = new HashSet<>();
          affectedTableIds.add(relationship.fkTableId());
          affectedTableIds.add(relationship.pkTableId());
          return loadTables(relationship)
              .flatMap(tables -> addColumn(
                  relationship,
                  tables.fkTable(),
                  tables.pkTable(),
                  command))
              .map(result -> MutationResult.of(result, affectedTableIds));
        });
  }

  private Mono<TablePair> loadTables(Relationship relationship) {
    return getTableByIdPort.findTableById(relationship.fkTableId())
        .switchIfEmpty(Mono.error(new RelationshipTargetTableNotExistException(
            "Relationship fk table not found")))
        .flatMap(fkTable -> getTableByIdPort.findTableById(relationship.pkTableId())
            .switchIfEmpty(Mono.error(new RelationshipTargetTableNotExistException(
                "Relationship pk table not found")))
            .map(pkTable -> new TablePair(fkTable, pkTable)));
  }

  private Mono<AddRelationshipColumnResult> addColumn(
      Relationship relationship,
      Table fkTable,
      Table pkTable,
      AddRelationshipColumnCommand command) {
    return Mono.zip(
        getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(relationship.id())
            .defaultIfEmpty(List.of()),
        getColumnsByTableIdPort.findColumnsByTableId(fkTable.id()).defaultIfEmpty(List.of()),
        getColumnsByTableIdPort.findColumnsByTableId(pkTable.id()).defaultIfEmpty(List.of()))
        .flatMap(tuple -> {
          List<RelationshipColumn> existingColumns = tuple.getT1();
          List<Column> fkColumns = tuple.getT2();
          List<Column> pkColumns = tuple.getT3();

          List<RelationshipColumn> updatedColumns = new ArrayList<>(existingColumns.size() + 1);
          updatedColumns.addAll(existingColumns);
          updatedColumns.add(new RelationshipColumn(
              null,
              relationship.id(),
              command.pkColumnId(),
              command.fkColumnId(),
              command.seqNo()));

          List<Integer> seqNos = updatedColumns.stream()
              .map(RelationshipColumn::seqNo)
              .toList();

          RelationshipValidator.validateSeqNoIntegrity(seqNos);
          RelationshipValidator.validateColumnExistence(
              fkColumns,
              pkColumns,
              updatedColumns,
              relationship.name());
          RelationshipValidator.validateColumnUniqueness(updatedColumns, relationship.name());
          return Mono.fromCallable(ulidGeneratorPort::generate)
              .flatMap(relationshipColumnId -> {
                RelationshipColumn relationshipColumn = new RelationshipColumn(
                    relationshipColumnId,
                    relationship.id(),
                    command.pkColumnId(),
                    command.fkColumnId(),
                    command.seqNo());

                return createRelationshipColumnPort.createRelationshipColumn(relationshipColumn)
                    .map(savedColumn -> new AddRelationshipColumnResult(
                        savedColumn.id(),
                        savedColumn.relationshipId(),
                        savedColumn.pkColumnId(),
                        savedColumn.fkColumnId(),
                        savedColumn.seqNo()));
              });
        });
  }

  private record TablePair(Table fkTable, Table pkTable) {
  }

}
