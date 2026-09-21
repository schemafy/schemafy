package com.schemafy.core.erd.table.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.core.erd.constraint.application.port.out.ConstraintExistsPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.operation.application.inverse.ChangeTableNameInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeTableNameInverse.ConstraintRename;
import com.schemafy.core.erd.operation.application.inverse.ChangeTableNameInverse.RelationshipRename;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipNamePort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.core.erd.relationship.application.port.out.RelationshipExistsPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameUseCase;
import com.schemafy.core.erd.table.application.port.out.ChangeTableNamePort;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.application.port.out.TableExistsPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;
import com.schemafy.core.erd.vendor.application.service.IdentifierCapabilityResolver;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;
import com.schemafy.core.erd.vendor.domain.validator.IdentifierValidator;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.TABLE;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = TABLE, id = "tableId"))
public class ChangeTableNameService implements ChangeTableNameUseCase {

  private final ChangeTableNamePort changeTableNamePort;
  private final TransactionalOperator transactionalOperator;
  private final TableExistsPort tableExistsPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetConstraintsByTableIdPort getConstraintsByTableIdPort;
  private final ChangeConstraintNamePort changeConstraintNamePort;
  private final ConstraintExistsPort constraintExistsPort;
  private final GetRelationshipsByTableIdPort getRelationshipsByTableIdPort;
  private final ChangeRelationshipNamePort changeRelationshipNamePort;
  private final RelationshipExistsPort relationshipExistsPort;
  private final IdentifierCapabilityResolver identifierCapabilityResolver;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeTableName(ChangeTableNameCommand command) {
    return getTableByIdPort.findTableById(command.tableId())
        .switchIfEmpty(Mono.error(
            new DomainException(TableErrorCode.NOT_FOUND, "Table not found: " + command.tableId())))
        .flatMap(table -> identifierCapabilityResolver.resolve(TABLE, command.tableId())
            .flatMap(identifiers -> {
              IdentifierValidator.validateLength(
                  identifiers,
                  command.newName(),
                  TableErrorCode.INVALID_VALUE,
                  "Table name");
              if (Objects.equals(table.name(), command.newName())) {
                return Mono.just(MutationResult.<Void>noop(null, table.id()));
              }
              return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_TABLE_NAME, command,
                  () -> getTableByIdPort.findTableById(command.tableId())
                      .switchIfEmpty(Mono.error(
                          new DomainException(TableErrorCode.NOT_FOUND, "Table not found: " + command.tableId())))
                      .flatMap(lockedTable -> {
                        if (Objects.equals(lockedTable.name(), command.newName())) {
                          return Mono.just(MutationResult.<Void>noop(null, lockedTable.id()));
                        }
                        return tableExistsPort.existsBySchemaIdAndName(lockedTable.schemaId(), command.newName())
                            .flatMap(exists -> {
                              if (exists) {
                                return Mono.error(new DomainException(TableErrorCode.NAME_DUPLICATE,
                                    "A table with the name '" + command.newName()
                                        + "' already exists in the schema."));
                              }

                              return buildRenamePlan(lockedTable, command.newName(), identifiers)
                                  .flatMap(plan -> changeTableNamePort.changeTableName(
                                      command.tableId(),
                                      command.newName())
                                      .then(applyConstraintRenames(plan.constraintRenames()))
                                      .then(applyRelationshipRenames(plan.relationshipRenames()))
                                      .thenReturn(MutationResult.<Void>of(
                                          null,
                                          plan.affectedTableIds(lockedTable.id()))
                                          .withInverse(plan.toInverse(lockedTable))));
                            });
                      }));
            }))
        .as(transactionalOperator::transactional);
  }

  private Mono<TableRenamePlan> buildRenamePlan(
      Table oldTable,
      String newTableName,
      IdentifierCapabilities identifiers) {
    Set<String> reservedConstraintNames = new HashSet<>();
    Set<String> reservedRelationshipNames = new HashSet<>();
    Mono<List<ConstraintRenamePlan>> constraintRenames = buildAutoConstraintRenamePlans(
        oldTable,
        newTableName,
        reservedConstraintNames,
        identifiers);
    Mono<List<RelationshipRenamePlan>> relationshipRenames = buildAutoRelationshipRenamePlans(
        oldTable,
        newTableName,
        reservedRelationshipNames,
        identifiers);

    return Mono.zip(constraintRenames, relationshipRenames)
        .map(tuple -> new TableRenamePlan(tuple.getT1(), tuple.getT2()));
  }

  private Mono<List<ConstraintRenamePlan>> buildAutoConstraintRenamePlans(
      Table oldTable,
      String newTableName,
      Set<String> reservedConstraintNames,
      IdentifierCapabilities identifiers) {
    return getConstraintsByTableIdPort.findConstraintsByTableId(oldTable.id())
        .defaultIfEmpty(List.of())
        .flatMapMany(Flux::fromIterable)
        .concatMap(constraint -> buildAutoConstraintRenamePlan(
            constraint,
            oldTable,
            newTableName,
            reservedConstraintNames,
            identifiers))
        .collectList();
  }

  private Mono<ConstraintRenamePlan> buildAutoConstraintRenamePlan(
      Constraint constraint,
      Table oldTable,
      String newTableName,
      Set<String> reservedConstraintNames,
      IdentifierCapabilities identifiers) {
    String prefix = constraintKindPrefix(constraint.kind());
    String oldBaseName = normalizeName(prefix + oldTable.name());
    OptionalInt suffixOpt = parseAutoNameSuffix(constraint.name(), oldBaseName, identifiers);
    if (suffixOpt.isEmpty()) {
      return Mono.empty();
    }
    String newBaseName = normalizeName(prefix + newTableName);
    int suffix = suffixOpt.getAsInt();
    return resolveUniqueConstraintName(
        oldTable.schemaId(),
        newBaseName,
        constraint.id(),
        suffix,
        reservedConstraintNames,
        identifiers)
        .flatMap(newName -> {
          if (newName.equals(constraint.name())) {
            return Mono.empty();
          }
          reservedConstraintNames.add(newName);
          return Mono.just(new ConstraintRenamePlan(
              constraint.id(),
              constraint.name(),
              newName));
        });
  }

  private Mono<Void> applyConstraintRenames(List<ConstraintRenamePlan> plans) {
    return Flux.fromIterable(plans)
        .concatMap(plan -> changeConstraintNamePort.changeConstraintName(
            plan.constraintId(),
            plan.newName()))
        .then();
  }

  private Mono<String> resolveUniqueConstraintName(
      String schemaId,
      String baseName,
      String excludeId,
      int suffix,
      Set<String> reservedConstraintNames,
      IdentifierCapabilities identifiers) {
    String candidate = identifiers.fitGeneratedName(baseName, suffixValue(suffix));
    if (reservedConstraintNames.contains(candidate)) {
      return resolveUniqueConstraintName(
          schemaId,
          baseName,
          excludeId,
          suffix + 1,
          reservedConstraintNames,
          identifiers);
    }
    return constraintExistsPort.existsBySchemaIdAndNameExcludingId(schemaId, candidate, excludeId)
        .flatMap(exists -> {
          if (exists) {
            return resolveUniqueConstraintName(
                schemaId,
                baseName,
                excludeId,
                suffix + 1,
                reservedConstraintNames,
                identifiers);
          }
          return Mono.just(candidate);
        });
  }

  private Mono<List<RelationshipRenamePlan>> buildAutoRelationshipRenamePlans(
      Table oldTable,
      String newTableName,
      Set<String> reservedRelationshipNames,
      IdentifierCapabilities identifiers) {
    return getRelationshipsByTableIdPort.findRelationshipsByTableId(oldTable.id())
        .defaultIfEmpty(List.of())
        .flatMapMany(Flux::fromIterable)
        .concatMap(relationship -> buildAutoRelationshipRenamePlan(
            relationship,
            oldTable,
            newTableName,
            reservedRelationshipNames,
            identifiers))
        .collectList();
  }

  private Mono<RelationshipRenamePlan> buildAutoRelationshipRenamePlan(
      Relationship relationship,
      Table oldTable,
      String newTableName,
      Set<String> reservedRelationshipNames,
      IdentifierCapabilities identifiers) {
    boolean isFkRenamed = relationship.fkTableId().equals(oldTable.id());
    boolean isPkRenamed = relationship.pkTableId().equals(oldTable.id());
    if (!isFkRenamed && !isPkRenamed) {
      return Mono.empty();
    }
    if (isFkRenamed && isPkRenamed) {
      return buildAutoRelationshipRenamePlan(
          relationship,
          oldTable.name(),
          oldTable.name(),
          newTableName,
          newTableName,
          reservedRelationshipNames,
          identifiers);
    }
    String otherTableId = isFkRenamed ? relationship.pkTableId() : relationship.fkTableId();
    return getTableByIdPort.findTableById(otherTableId)
        .flatMap(otherTable -> buildAutoRelationshipRenamePlan(
            relationship,
            isFkRenamed ? oldTable.name() : otherTable.name(),
            isPkRenamed ? oldTable.name() : otherTable.name(),
            isFkRenamed ? newTableName : otherTable.name(),
            isPkRenamed ? newTableName : otherTable.name(),
            reservedRelationshipNames,
            identifiers))
        .switchIfEmpty(Mono.empty());
  }

  private Mono<RelationshipRenamePlan> buildAutoRelationshipRenamePlan(
      Relationship relationship,
      String oldFkName,
      String oldPkName,
      String newFkName,
      String newPkName,
      Set<String> reservedRelationshipNames,
      IdentifierCapabilities identifiers) {
    String oldBaseName = normalizeName("rel_" + oldFkName + "_to_" + oldPkName);
    OptionalInt suffixOpt = parseAutoNameSuffix(relationship.name(), oldBaseName, identifiers);
    if (suffixOpt.isEmpty()) {
      return Mono.empty();
    }
    String newBaseName = normalizeName("rel_" + newFkName + "_to_" + newPkName);
    int suffix = suffixOpt.getAsInt();
    return resolveUniqueRelationshipName(
        relationship.fkTableId(),
        newBaseName,
        relationship.id(),
        suffix,
        reservedRelationshipNames,
        identifiers)
        .flatMap(newName -> {
          if (newName.equals(relationship.name())) {
            return Mono.empty();
          }
          reservedRelationshipNames.add(relationshipNameKey(relationship.fkTableId(), newName));
          return Mono.just(new RelationshipRenamePlan(
              relationship.id(),
              relationship.fkTableId(),
              relationship.pkTableId(),
              relationship.name(),
              newName));
        });
  }

  private Mono<Void> applyRelationshipRenames(List<RelationshipRenamePlan> plans) {
    return Flux.fromIterable(plans)
        .concatMap(plan -> changeRelationshipNamePort.changeRelationshipName(
            plan.relationshipId(),
            plan.newName()))
        .then();
  }

  private Mono<String> resolveUniqueRelationshipName(
      String fkTableId,
      String baseName,
      String excludeId,
      int suffix,
      Set<String> reservedRelationshipNames,
      IdentifierCapabilities identifiers) {
    String candidate = identifiers.fitGeneratedName(baseName, suffixValue(suffix));
    if (reservedRelationshipNames.contains(relationshipNameKey(fkTableId, candidate))) {
      return resolveUniqueRelationshipName(
          fkTableId,
          baseName,
          excludeId,
          suffix + 1,
          reservedRelationshipNames,
          identifiers);
    }
    return relationshipExistsPort
        .existsByFkTableIdAndNameExcludingId(fkTableId, candidate, excludeId)
        .flatMap(exists -> {
          if (exists) {
            return resolveUniqueRelationshipName(
                fkTableId,
                baseName,
                excludeId,
                suffix + 1,
                reservedRelationshipNames,
                identifiers);
          }
          return Mono.just(candidate);
        });
  }

  private static String relationshipNameKey(String fkTableId, String relationshipName) {
    return fkTableId + "\u0000" + relationshipName;
  }

  private static OptionalInt parseAutoNameSuffix(
      String name,
      String baseName,
      IdentifierCapabilities identifiers) {
    if (name == null || baseName == null) {
      return OptionalInt.empty();
    }
    if (name.equals(baseName) || name.equals(identifiers.fitGeneratedName(baseName, ""))) {
      return OptionalInt.of(0);
    }
    String suffix;
    String legacyPrefix = baseName + "_";
    if (name.startsWith(legacyPrefix)) {
      suffix = name.substring(legacyPrefix.length());
    } else {
      int suffixSeparator = name.lastIndexOf('_');
      if (suffixSeparator < 0) {
        return OptionalInt.empty();
      }
      suffix = name.substring(suffixSeparator + 1);
      if (!name.equals(identifiers.fitGeneratedName(baseName, "_" + suffix))) {
        return OptionalInt.empty();
      }
    }
    if (suffix.isEmpty()) {
      return OptionalInt.empty();
    }
    for (int index = 0; index < suffix.length(); index++) {
      if (!Character.isDigit(suffix.charAt(index))) {
        return OptionalInt.empty();
      }
    }
    try {
      return OptionalInt.of(Integer.parseInt(suffix));
    } catch (NumberFormatException exception) {
      return OptionalInt.empty();
    }
  }

  private static String constraintKindPrefix(ConstraintKind kind) {
    return switch (kind) {
    case PRIMARY_KEY -> "pk_";
    case UNIQUE -> "uq_";
    case CHECK -> "ck_";
    case DEFAULT -> "df_";
    case NOT_NULL -> "nn_";
    };
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

  private static String suffixValue(int suffix) {
    return suffix == 0 ? "" : "_" + suffix;
  }

  private record TableRenamePlan(
      List<ConstraintRenamePlan> constraintRenames,
      List<RelationshipRenamePlan> relationshipRenames) {

    Set<String> affectedTableIds(String tableId) {
      Set<String> affectedTableIds = new HashSet<>();
      affectedTableIds.add(tableId);
      relationshipRenames.forEach(rename -> {
        affectedTableIds.add(rename.fkTableId());
        affectedTableIds.add(rename.pkTableId());
      });
      return affectedTableIds;
    }

    ChangeTableNameInverse toInverse(Table oldTable) {
      return new ChangeTableNameInverse(
          oldTable.id(),
          oldTable.name(),
          constraintRenames.stream()
              .map(rename -> new ConstraintRename(rename.constraintId(), rename.oldName()))
              .toList(),
          relationshipRenames.stream()
              .map(rename -> new RelationshipRename(
                  rename.relationshipId(),
                  rename.oldName(),
                  rename.fkTableId(),
                  rename.pkTableId()))
              .toList());
    }

  }

  private record ConstraintRenamePlan(
      String constraintId,
      String oldName,
      String newName) {

  }

  private record RelationshipRenamePlan(
      String relationshipId,
      String fkTableId,
      String pkTableId,
      String oldName,
      String newName) {

  }

}
