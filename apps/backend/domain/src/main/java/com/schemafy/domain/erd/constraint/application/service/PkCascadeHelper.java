package com.schemafy.domain.erd.constraint.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.schemafy.domain.erd.column.application.port.out.CreateColumnPort;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.constraint.application.port.out.CreateConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.CreateConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.relationship.application.port.out.CreateRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PkCascadeHelper {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final CreateColumnPort createColumnPort;
  private final DeleteColumnUseCase deleteColumnUseCase;
  private final GetTableByIdPort getTableByIdPort;
  private final GetConstraintsByTableIdPort getConstraintsByTableIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final CreateConstraintPort createConstraintPort;
  private final CreateConstraintColumnPort createConstraintColumnPort;
  private final DeleteConstraintColumnPort deleteConstraintColumnPort;
  private final DeleteConstraintPort deleteConstraintPort;
  private final GetRelationshipsByPkTableIdPort getRelationshipsByPkTableIdPort;
  private final GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;
  private final CreateRelationshipColumnPort createRelationshipColumnPort;
  private final DeleteRelationshipColumnPort deleteRelationshipColumnPort;
  private final DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsByRelationshipIdPort;
  private final DeleteRelationshipPort deleteRelationshipPort;

  public Mono<List<CascadeCreatedInfo>> cascadeAddPkColumn(
      String pkTableId,
      Column pkColumn,
      Set<String> visited) {

    String visitKey = pkTableId + ":" + pkColumn.id();
    if (!visited.add(visitKey)) {
      return Mono.just(List.of());
    }

    return getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkTableId)
        .defaultIfEmpty(List.of())
        .flatMap(relationships -> {
          if (relationships.isEmpty()) {
            return Mono.just(List.of());
          }
          return Flux.fromIterable(relationships)
              .concatMap(rel -> cascadeAddToRelationship(rel, pkColumn, visited))
              .collectList()
              .map(lists -> lists.stream()
                  .flatMap(List::stream)
                  .toList());
        });
  }

  public Mono<Void> cascadeRemovePkColumn(
      String pkTableId,
      String pkColumnId,
      Set<String> visited) {

    String visitKey = pkTableId + ":" + pkColumnId;
    if (!visited.add(visitKey)) {
      return Mono.empty();
    }

    return getRelationshipsByPkTableIdPort.findRelationshipsByPkTableId(pkTableId)
        .defaultIfEmpty(List.of())
        .flatMapMany(Flux::fromIterable)
        .concatMap(rel -> cascadeRemoveFromRelationship(rel, pkColumnId, visited))
        .then();
  }

  public Mono<Void> syncPkForKindChange(
      Relationship relationship,
      RelationshipKind oldKind,
      RelationshipKind newKind,
      Set<String> visited) {

    if (oldKind == newKind) {
      return Mono.empty();
    }

    if (newKind == RelationshipKind.IDENTIFYING) {
      return addFkColumnsToPk(relationship, visited);
    } else {
      return removeFkColumnsFromPk(relationship, visited);
    }
  }

  private Mono<List<CascadeCreatedInfo>> cascadeAddToRelationship(
      Relationship relationship,
      Column pkColumn,
      Set<String> visited) {

    String fkTableId = relationship.fkTableId();

    return Mono.zip(
        getColumnsByTableIdPort.findColumnsByTableId(fkTableId).defaultIfEmpty(List.of()),
        getRelationshipColumnsByRelationshipIdPort
            .findRelationshipColumnsByRelationshipId(relationship.id())
            .defaultIfEmpty(List.of())).flatMap(tuple -> {
              List<Column> fkColumns = tuple.getT1();
              List<RelationshipColumn> existingRelColumns = tuple.getT2();

              String fkColumnName = resolveUniqueName(
                  pkColumn.name(),
                  fkColumns.stream().map(Column::name).collect(Collectors.toSet()));

              Column fkColumn = new Column(
                  ulidGeneratorPort.generate(),
                  fkTableId,
                  fkColumnName,
                  pkColumn.dataType(),
                  pkColumn.lengthScale(),
                  fkColumns.size(),
                  false,
                  pkColumn.charset(),
                  pkColumn.collation(),
                  null);

              return createColumnPort.createColumn(fkColumn)
                  .flatMap(savedFkColumn -> {
                    RelationshipColumn relColumn = new RelationshipColumn(
                        ulidGeneratorPort.generate(),
                        relationship.id(),
                        pkColumn.id(),
                        savedFkColumn.id(),
                        existingRelColumns.size());

                    return createRelationshipColumnPort.createRelationshipColumn(relColumn)
                        .flatMap(savedRelColumn -> {
                          List<CascadeCreatedInfo> results = new ArrayList<>();
                          CascadeCreatedInfo baseInfo = new CascadeCreatedInfo(
                              savedFkColumn.id(),
                              savedFkColumn.name(),
                              fkTableId,
                              savedRelColumn.id(),
                              relationship.id(),
                              null,
                              null);

                          if (relationship.kind() == RelationshipKind.IDENTIFYING) {
                            return addColumnToFkTablePk(fkTableId, savedFkColumn)
                                .flatMap(pkInfo -> {
                                  results.add(baseInfo.withPkInfo(
                                      pkInfo.constraintColumnId(),
                                      pkInfo.constraintId()));

                                  return cascadeAddPkColumn(fkTableId, savedFkColumn, visited)
                                      .map(childResults -> {
                                        results.addAll(childResults);
                                        return results;
                                      });
                                });
                          }

                          results.add(baseInfo);
                          return Mono.just(results);
                        });
                  });
            });
  }

  private Mono<Void> cascadeRemoveFromRelationship(
      Relationship relationship,
      String pkColumnId,
      Set<String> visited) {

    return getRelationshipColumnsByRelationshipIdPort
        .findRelationshipColumnsByRelationshipId(relationship.id())
        .defaultIfEmpty(List.of())
        .flatMap(relColumns -> {
          List<RelationshipColumn> toRemove = relColumns.stream()
              .filter(rc -> rc.pkColumnId().equals(pkColumnId))
              .toList();

          if (toRemove.isEmpty()) {
            return Mono.empty();
          }

          List<String> fkColumnIds = toRemove.stream()
              .map(RelationshipColumn::fkColumnId)
              .toList();

          List<RelationshipColumn> remaining = relColumns.stream()
              .filter(rc -> !rc.pkColumnId().equals(pkColumnId))
              .toList();

          Mono<Void> identifyingCascade = Mono.empty();
          if (relationship.kind() == RelationshipKind.IDENTIFYING) {
            identifyingCascade = Flux.fromIterable(fkColumnIds)
                .concatMap(fkColumnId -> removeColumnFromFkTablePk(relationship.fkTableId(), fkColumnId)
                    .then(cascadeRemovePkColumn(relationship.fkTableId(), fkColumnId, visited)))
                .then();
          }

          Mono<Void> deleteRelColumns;
          if (remaining.isEmpty()) {
            deleteRelColumns = deleteRelationshipColumnsByRelationshipIdPort
                .deleteByRelationshipId(relationship.id())
                .then(deleteRelationshipPort.deleteRelationship(relationship.id()));
          } else {
            deleteRelColumns = Flux.fromIterable(toRemove)
                .concatMap(rc -> deleteRelationshipColumnPort.deleteRelationshipColumn(rc.id()))
                .then();
          }

          Mono<Void> deleteFkColumns = Flux.fromIterable(fkColumnIds)
              .concatMap(fkColumnId -> deleteColumnUseCase.deleteColumn(
                  new DeleteColumnCommand(fkColumnId)))
              .then();

          return identifyingCascade
              .then(deleteRelColumns)
              .then(deleteFkColumns);
        });
  }

  private Mono<PkAddResult> addColumnToFkTablePk(String fkTableId, Column fkColumn) {
    return findOrCreatePkConstraint(fkTableId)
        .flatMap(pkConstraint -> getConstraintColumnsByConstraintIdPort
            .findConstraintColumnsByConstraintId(pkConstraint.id())
            .defaultIfEmpty(List.of())
            .flatMap(existingColumns -> {
              ConstraintColumn cc = new ConstraintColumn(
                  ulidGeneratorPort.generate(),
                  pkConstraint.id(),
                  fkColumn.id(),
                  existingColumns.size());

              return createConstraintColumnPort.createConstraintColumn(cc)
                  .map(saved -> new PkAddResult(saved.id(), pkConstraint.id()));
            }));
  }

  private Mono<Void> removeColumnFromFkTablePk(String fkTableId, String fkColumnId) {
    return getConstraintsByTableIdPort.findConstraintsByTableId(fkTableId)
        .defaultIfEmpty(List.of())
        .flatMap(constraints -> {
          Optional<Constraint> pkOpt = constraints.stream()
              .filter(c -> c.kind() == ConstraintKind.PRIMARY_KEY)
              .findFirst();

          if (pkOpt.isEmpty()) {
            return Mono.empty();
          }

          Constraint pk = pkOpt.get();
          return getConstraintColumnsByConstraintIdPort
              .findConstraintColumnsByConstraintId(pk.id())
              .defaultIfEmpty(List.of())
              .flatMap(constraintColumns -> {
                Optional<ConstraintColumn> ccOpt = constraintColumns.stream()
                    .filter(cc -> cc.columnId().equals(fkColumnId))
                    .findFirst();

                if (ccOpt.isEmpty()) {
                  return Mono.empty();
                }

                return deleteConstraintColumnPort.deleteConstraintColumn(ccOpt.get().id())
                    .then(deleteOrphanPkConstraint(pk.id()));
              });
        });
  }

  private Mono<Void> deleteOrphanPkConstraint(String constraintId) {
    return getConstraintColumnsByConstraintIdPort
        .findConstraintColumnsByConstraintId(constraintId)
        .defaultIfEmpty(List.of())
        .flatMap(columns -> {
          if (columns.isEmpty()) {
            return deleteConstraintPort.deleteConstraint(constraintId);
          }
          return Mono.empty();
        });
  }

  private Mono<Constraint> findOrCreatePkConstraint(String tableId) {
    return getConstraintsByTableIdPort.findConstraintsByTableId(tableId)
        .defaultIfEmpty(List.of())
        .flatMap(constraints -> {
          Optional<Constraint> existingPk = constraints.stream()
              .filter(c -> c.kind() == ConstraintKind.PRIMARY_KEY)
              .findFirst();

          if (existingPk.isPresent()) {
            return Mono.just(existingPk.get());
          }

          return getTableByIdPort.findTableById(tableId)
              .flatMap(table -> {
                String pkName = "pk_" + table.name();
                Constraint newPk = new Constraint(
                    ulidGeneratorPort.generate(),
                    tableId,
                    pkName,
                    ConstraintKind.PRIMARY_KEY,
                    null,
                    null);

                return createConstraintPort.createConstraint(newPk);
              });
        });
  }

  private Mono<Void> addFkColumnsToPk(Relationship relationship, Set<String> visited) {
    String fkTableId = relationship.fkTableId();

    return getRelationshipColumnsByRelationshipIdPort
        .findRelationshipColumnsByRelationshipId(relationship.id())
        .defaultIfEmpty(List.of())
        .flatMap(relColumns -> {
          if (relColumns.isEmpty()) {
            return Mono.empty();
          }

          return Flux.fromIterable(relColumns)
              .concatMap(rc -> getColumnByIdPort.findColumnById(rc.fkColumnId())
                  .flatMap(fkColumn -> addColumnToFkTablePk(fkTableId, fkColumn)
                      .then(cascadeAddPkColumn(fkTableId, fkColumn, visited))))
              .then();
        });
  }

  private Mono<Void> removeFkColumnsFromPk(Relationship relationship, Set<String> visited) {
    String fkTableId = relationship.fkTableId();

    return getRelationshipColumnsByRelationshipIdPort
        .findRelationshipColumnsByRelationshipId(relationship.id())
        .defaultIfEmpty(List.of())
        .flatMap(relColumns -> {
          if (relColumns.isEmpty()) {
            return Mono.empty();
          }

          return Flux.fromIterable(relColumns)
              .concatMap(rc -> {
                String fkColumnId = rc.fkColumnId();
                return removeColumnFromFkTablePk(fkTableId, fkColumnId)
                    .then(cascadeRemovePkColumn(fkTableId, fkColumnId, visited));
              })
              .then();
        });
  }

  private String resolveUniqueName(String baseName, Set<String> existingNames) {
    if (!existingNames.contains(baseName)) {
      return baseName;
    }
    int suffix = 1;
    while (existingNames.contains(baseName + "_" + suffix)) {
      suffix++;
    }
    return baseName + "_" + suffix;
  }

  public record CascadeCreatedInfo(
      String fkColumnId,
      String fkColumnName,
      String fkTableId,
      String relationshipColumnId,
      String relationshipId,
      String constraintColumnId,
      String constraintId) {

    public CascadeCreatedInfo withPkInfo(String constraintColumnId, String constraintId) {
      return new CascadeCreatedInfo(
          this.fkColumnId,
          this.fkColumnName,
          this.fkTableId,
          this.relationshipColumnId,
          this.relationshipId,
          constraintColumnId,
          constraintId);
    }

  }

  private record PkAddResult(String constraintColumnId, String constraintId) {
  }

}
