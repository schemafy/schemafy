package com.schemafy.core.erd.relationship.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonObjectMetadataConverter;
import com.schemafy.core.erd.column.application.port.out.CreateColumnPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.validator.ColumnValidator;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.core.erd.constraint.application.service.PkCascadeHelper;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.ConstraintColumn;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.operation.application.inverse.CreateRelationshipInverse;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.application.service.StructuralSnapshotService;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipResult;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.core.erd.relationship.application.port.out.CreateRelationshipColumnPort;
import com.schemafy.core.erd.relationship.application.port.out.CreateRelationshipPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.core.erd.relationship.application.port.out.RelationshipExistsPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.core.erd.relationship.domain.validator.RelationshipValidator;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.vendor.application.service.IdentifierCapabilityResolver;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.TABLE;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, targets = {
  @AccessTarget(value = TABLE, id = "fkTableId"),
  @AccessTarget(value = TABLE, id = "pkTableId")
})
public class CreateRelationshipService implements CreateRelationshipUseCase {

  private final TransactionalOperator transactionalOperator;
  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateRelationshipPort createRelationshipPort;
  private final CreateRelationshipColumnPort createRelationshipColumnPort;
  private final CreateColumnPort createColumnPort;
  private final RelationshipExistsPort relationshipExistsPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort;
  private final GetConstraintsByTableIdPort getConstraintsByTableIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final IdentifierCapabilityResolver identifierCapabilityResolver;
  private final PkCascadeHelper pkCascadeHelper;
  private final StructuralSnapshotService structuralSnapshotService;
  private final JsonObjectMetadataConverter jsonObjectMetadataConverter;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<CreateRelationshipResult>> createRelationship(
      CreateRelationshipCommand command) {
    return Mono.defer(() -> {
      String canonicalExtra = jsonObjectMetadataConverter.toStorageJson(command.extra());
      return erdMutationCoordinator.coordinate(ErdOperationType.CREATE_RELATIONSHIP, command,
          () -> loadTargetTables(command)
              .flatMap(tables -> validateCreateRelationship(command, tables)
                  .then(identifierCapabilityResolver.resolve(TABLE, tables.fkTable().id()))
                  .flatMap(identifierCapabilities -> structuralSnapshotService
                      .captureBySchemaId(tables.fkTable().schemaId())
                      .flatMap(beforeSnapshot -> {
                        Set<String> affectedTableIds = new HashSet<>();
                        affectedTableIds.add(tables.fkTable().id());
                        affectedTableIds.add(tables.pkTable().id());
                        return createRelationshipAuto(
                            tables.fkTable(),
                            tables.pkTable(),
                            command,
                            canonicalExtra,
                            affectedTableIds,
                            identifierCapabilities)
                            .flatMap(result -> structuralSnapshotService.captureBySchemaId(beforeSnapshot.schemaId())
                                .map(afterSnapshot -> result.withInverse(new CreateRelationshipInverse(
                                    beforeSnapshot.schemaId(),
                                    result.result().relationshipId(),
                                    beforeSnapshot,
                                    afterSnapshot,
                                    result.sortedAffectedTableIds()))));
                      }))));
    })
        .as(transactionalOperator::transactional);
  }

  private Mono<TargetTables> loadTargetTables(CreateRelationshipCommand command) {
    return getTableByIdPort.findTableById(command.fkTableId())
        .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.TARGET_TABLE_NOT_FOUND,
            "Relationship fk table not found")))
        .flatMap(fkTable -> getTableByIdPort.findTableById(command.pkTableId())
            .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.TARGET_TABLE_NOT_FOUND,
                "Relationship pk table not found")))
            .map(pkTable -> new TargetTables(fkTable, pkTable)));
  }

  private Mono<Void> validateCreateRelationship(
      CreateRelationshipCommand command,
      TargetTables tables) {
    if (!tables.fkTable().schemaId().equals(tables.pkTable().schemaId())) {
      return Mono.error(new DomainException(RelationshipErrorCode.TARGET_TABLE_NOT_FOUND,
          "Relationship tables must belong to the same schema"));
    }
    if (command.kind() == null || command.cardinality() == null) {
      return Mono.error(new DomainException(RelationshipErrorCode.INVALID_VALUE,
          "Relationship kind and cardinality are required"));
    }
    return Mono.empty();
  }

  private Mono<MutationResult<CreateRelationshipResult>> createRelationshipAuto(
      Table fkTable,
      Table pkTable,
      CreateRelationshipCommand command,
      String canonicalExtra,
      Set<String> affectedTableIds,
      IdentifierCapabilities identifierCapabilities) {
    return resolveAutoRelationshipName(fkTable, pkTable, identifierCapabilities)
        .flatMap(normalizedName -> Mono.zip(
            loadPkColumns(pkTable),
            getColumnsByTableIdPort.findColumnsByTableId(fkTable.id()).defaultIfEmpty(List.of()),
            getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(fkTable.schemaId())
                .defaultIfEmpty(List.of()))
            .flatMap(tuple -> {
              List<Column> pkColumns = tuple.getT1();
              List<Column> fkColumns = tuple.getT2();
              List<Relationship> relationships = tuple.getT3();

              if (command.kind() == RelationshipKind.IDENTIFYING) {
                RelationshipValidator.validateIdentifyingCycle(
                    relationships,
                    null,
                    new Relationship(
                        "new",
                        pkTable.id(),
                        fkTable.id(),
                        normalizedName,
                        command.kind(),
                        command.cardinality(),
                        canonicalExtra));
              }

              return persistAutoRelationship(
                  fkTable,
                  pkTable,
                  command,
                  canonicalExtra,
                  normalizedName,
                  pkColumns,
                  fkColumns,
                  affectedTableIds,
                  identifierCapabilities);
            }));
  }

  private Mono<MutationResult<CreateRelationshipResult>> persistAutoRelationship(
      Table fkTable,
      Table pkTable,
      CreateRelationshipCommand command,
      String canonicalExtra,
      String normalizedName,
      List<Column> pkColumns,
      List<Column> fkColumns,
      Set<String> affectedTableIds,
      IdentifierCapabilities identifierCapabilities) {
    return Mono.fromCallable(ulidGeneratorPort::generate)
        .flatMap(relationshipId -> {
          Relationship relationship = new Relationship(
              relationshipId,
              pkTable.id(),
              fkTable.id(),
              normalizedName,
              command.kind(),
              command.cardinality(),
              canonicalExtra);

          return createRelationshipPort.createRelationship(relationship)
              .flatMap(savedRelationship -> createAutoRelationshipColumns(
                  relationshipId,
                  fkTable,
                  command.kind(),
                  pkColumns,
                  fkColumns,
                  affectedTableIds,
                  identifierCapabilities)
                  .thenReturn(new CreateRelationshipResult(
                      savedRelationship.id(),
                      savedRelationship.fkTableId(),
                      savedRelationship.pkTableId(),
                      savedRelationship.name(),
                      savedRelationship.kind(),
                      savedRelationship.cardinality(),
                      savedRelationship.extra())))
              .map(result -> MutationResult.of(result, affectedTableIds));
        });
  }

  private Mono<Void> createAutoRelationshipColumns(
      String relationshipId,
      Table fkTable,
      RelationshipKind kind,
      List<Column> pkColumns,
      List<Column> existingFkColumns,
      Set<String> affectedTableIds,
      IdentifierCapabilities identifierCapabilities) {
    Set<String> existingNames = new HashSet<>();
    for (Column column : existingFkColumns) {
      existingNames.add(column.name());
    }
    int baseSeqNo = existingFkColumns.size();

    return Flux.fromIterable(pkColumns)
        .index()
        .concatMap(tuple -> {
          int seqNo = Math.toIntExact(tuple.getT1());
          Column pkColumn = tuple.getT2();
          String fkColumnName = resolveUniqueName(
              pkColumn.name(),
              existingNames,
              identifierCapabilities,
              ColumnValidator.maxNameLength());
          existingNames.add(fkColumnName);
          return Mono.fromCallable(ulidGeneratorPort::generate)
              .flatMap(fkColumnId -> {
                Column fkColumn = new Column(
                    fkColumnId,
                    fkTable.id(),
                    fkColumnName,
                    pkColumn.dataType(),
                    pkColumn.typeArguments(),
                    baseSeqNo + seqNo,
                    false,
                    pkColumn.charset(),
                    pkColumn.collation(),
                    null);

                return createColumnPort.createColumn(fkColumn)
                    .flatMap(savedFkColumn -> Mono.fromCallable(ulidGeneratorPort::generate)
                        .flatMap(relColumnId -> {
                          RelationshipColumn relColumn = new RelationshipColumn(
                              relColumnId,
                              relationshipId,
                              pkColumn.id(),
                              savedFkColumn.id(),
                              seqNo);

                          Mono<Void> createRelColumn = createRelationshipColumnPort
                              .createRelationshipColumn(relColumn)
                              .then();

                          if (kind != RelationshipKind.IDENTIFYING) {
                            return createRelColumn;
                          }
                          return createRelColumn.then(pkCascadeHelper.addPkColumnAndCascade(
                              fkTable.id(),
                              savedFkColumn,
                              new HashSet<>(),
                              affectedTableIds));
                        }));
              });
        })
        .then();
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

  private Mono<String> resolveAutoRelationshipName(
      Table fkTable,
      Table pkTable,
      IdentifierCapabilities identifierCapabilities) {
    String baseName = normalizeName("rel_" + fkTable.name() + "_to_" + pkTable.name());
    return ensureUniqueRelationshipName(fkTable.id(), baseName, identifierCapabilities, 0)
        .doOnNext(RelationshipValidator::validateName);
  }

  private Mono<String> ensureUniqueRelationshipName(
      String fkTableId,
      String baseName,
      IdentifierCapabilities identifierCapabilities,
      int suffix) {
    String suffixValue = suffix == 0 ? "" : "_" + suffix;
    String candidate = identifierCapabilities.fitGeneratedName(baseName, suffixValue);
    return relationshipExistsPort.existsByFkTableIdAndName(fkTableId, candidate)
        .flatMap(exists -> {
          if (exists) {
            return ensureUniqueRelationshipName(
                fkTableId,
                baseName,
                identifierCapabilities,
                suffix + 1);
          }
          return Mono.just(candidate);
        });
  }

  private Mono<List<Column>> loadPkColumns(Table pkTable) {
    return getConstraintsByTableIdPort.findConstraintsByTableId(pkTable.id())
        .defaultIfEmpty(List.of())
        .flatMap(constraints -> {
          Constraint pkConstraint = null;
          for (Constraint constraint : constraints) {
            if (constraint.kind() == ConstraintKind.PRIMARY_KEY) {
              pkConstraint = constraint;
              break;
            }
          }
          if (pkConstraint == null) {
            return Mono.error(new DomainException(RelationshipErrorCode.INVALID_VALUE,
                "PK constraint not found for table '%s'".formatted(pkTable.id())));
          }
          return getConstraintColumnsByConstraintIdPort
              .findConstraintColumnsByConstraintId(pkConstraint.id())
              .defaultIfEmpty(List.of())
              .flatMap(constraintColumns -> {
                if (constraintColumns.isEmpty()) {
                  return Mono.error(new DomainException(RelationshipErrorCode.INVALID_VALUE,
                      "PK constraint has no columns for table '%s'".formatted(pkTable.id())));
                }
                return getColumnsByTableIdPort.findColumnsByTableId(pkTable.id())
                    .defaultIfEmpty(List.of())
                    .flatMap(columns -> Mono.just(resolvePkColumns(
                        columns,
                        constraintColumns,
                        pkTable.id())));
              });
        });
  }

  private static List<Column> resolvePkColumns(
      List<Column> columns,
      List<ConstraintColumn> constraintColumns,
      String tableId) {
    if (constraintColumns == null || constraintColumns.isEmpty()) {
      throw new DomainException(RelationshipErrorCode.INVALID_VALUE,
          "PK constraint has no columns for table '%s'".formatted(tableId));
    }
    Map<String, Column> columnsById = new HashMap<>();
    if (columns != null) {
      for (Column column : columns) {
        columnsById.put(normalizeId(column.id()), column);
      }
    }
    List<ConstraintColumn> ordered = new ArrayList<>(constraintColumns);
    ordered.sort((left, right) -> Integer.compare(left.seqNo(), right.seqNo()));

    List<Column> pkColumns = new ArrayList<>(ordered.size());
    for (ConstraintColumn constraintColumn : ordered) {
      Column column = columnsById.get(normalizeId(constraintColumn.columnId()));
      if (column == null) {
        throw new DomainException(RelationshipErrorCode.INVALID_VALUE,
            "PK column '%s' not found in table '%s'".formatted(
                constraintColumn.columnId(),
                tableId));
      }
      pkColumns.add(column);
    }
    if (pkColumns.isEmpty()) {
      throw new DomainException(RelationshipErrorCode.INVALID_VALUE,
          "PK constraint has no columns for table '%s'".formatted(tableId));
    }
    return pkColumns;
  }

  private static String resolveUniqueName(
      String baseName,
      Set<String> existingNames,
      IdentifierCapabilities identifierCapabilities,
      int localMaxLength) {
    int suffix = 0;
    while (true) {
      String suffixValue = suffix == 0 ? "" : "_" + suffix;
      String candidate = identifierCapabilities.fitGeneratedName(
          baseName,
          suffixValue,
          localMaxLength);
      if (!existingNames.contains(candidate)) {
        return candidate;
      }
      suffix++;
    }
  }

  private static String normalizeId(String value) {
    return value == null ? null : value.toUpperCase(Locale.ROOT);
  }

  private record TargetTables(Table fkTable, Table pkTable) {
  }

}
