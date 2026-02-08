package com.schemafy.domain.erd.table.application.service;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.domain.erd.constraint.application.port.out.ConstraintExistsPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipNamePort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.RelationshipExistsPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNameInvalidException;
import com.schemafy.domain.erd.relationship.domain.validator.RelationshipValidator;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.domain.erd.table.application.port.in.ChangeTableNameUseCase;
import com.schemafy.domain.erd.table.application.port.out.ChangeTableNamePort;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.application.port.out.TableExistsPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.erd.table.domain.exception.TableNameDuplicateException;
import com.schemafy.domain.erd.table.domain.exception.TableNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
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

  @Override
  public Mono<MutationResult<Void>> changeTableName(ChangeTableNameCommand command) {
    return getTableByIdPort.findTableById(command.tableId())
        .switchIfEmpty(Mono.error(
            new TableNotExistException("Table not found: " + command.tableId())))
        .flatMap(table -> tableExistsPort.existsBySchemaIdAndName(table.schemaId(), command.newName())
            .flatMap(exists -> {
              if (exists) {
                return Mono.error(new TableNameDuplicateException(
                    "A table with the name '" + command.newName() + "' already exists in the schema."));
              }

              return changeTableNamePort.changeTableName(
                  command.tableId(),
                  command.newName())
                  .then(renamePkConstraint(table.schemaId(), command.tableId(), command.newName()))
                  .then(renameAutoRelationshipNames(table, command.newName()))
                  .thenReturn(MutationResult.<Void>of(null, table.id()));
            }))
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> renamePkConstraint(String schemaId, String tableId, String newTableName) {
    return getConstraintsByTableIdPort.findConstraintsByTableId(tableId)
        .defaultIfEmpty(List.of())
        .flatMap(constraints -> {
          Optional<Constraint> pkOpt = constraints.stream()
              .filter(c -> c.kind() == ConstraintKind.PRIMARY_KEY)
              .findFirst();

          if (pkOpt.isEmpty()) {
            return Mono.empty();
          }

          String baseName = "pk_" + newTableName;
          return resolveUniqueConstraintName(schemaId, baseName, pkOpt.get().id())
              .flatMap(newName -> changeConstraintNamePort.changeConstraintName(
                  pkOpt.get().id(), newName));
        });
  }

  private Mono<String> resolveUniqueConstraintName(
      String schemaId, String baseName, String excludeId) {
    return resolveUniqueConstraintName(schemaId, baseName, excludeId, 0);
  }

  private Mono<String> resolveUniqueConstraintName(
      String schemaId, String baseName, String excludeId, int suffix) {
    String candidate = suffix == 0 ? baseName : baseName + "_" + suffix;
    return constraintExistsPort.existsBySchemaIdAndNameExcludingId(schemaId, candidate, excludeId)
        .flatMap(exists -> {
          if (exists) {
            return resolveUniqueConstraintName(schemaId, baseName, excludeId, suffix + 1);
          }
          return Mono.just(candidate);
        });
  }

  private Mono<Void> renameAutoRelationshipNames(Table oldTable, String newTableName) {
    return getRelationshipsByTableIdPort.findRelationshipsByTableId(oldTable.id())
        .defaultIfEmpty(List.of())
        .flatMapMany(Flux::fromIterable)
        .concatMap(relationship -> renameAutoRelationshipName(relationship, oldTable, newTableName))
        .then();
  }

  private Mono<Void> renameAutoRelationshipName(
      Relationship relationship,
      Table oldTable,
      String newTableName) {
    boolean isFkRenamed = relationship.fkTableId().equals(oldTable.id());
    boolean isPkRenamed = relationship.pkTableId().equals(oldTable.id());
    if (!isFkRenamed && !isPkRenamed) {
      return Mono.empty();
    }
    if (isFkRenamed && isPkRenamed) {
      return renameAutoRelationshipName(
          relationship,
          oldTable.name(),
          oldTable.name(),
          newTableName,
          newTableName);
    }
    String otherTableId = isFkRenamed ? relationship.pkTableId() : relationship.fkTableId();
    return getTableByIdPort.findTableById(otherTableId)
        .flatMap(otherTable -> renameAutoRelationshipName(
            relationship,
            isFkRenamed ? oldTable.name() : otherTable.name(),
            isPkRenamed ? oldTable.name() : otherTable.name(),
            isFkRenamed ? newTableName : otherTable.name(),
            isPkRenamed ? newTableName : otherTable.name()))
        .switchIfEmpty(Mono.empty());
  }

  private Mono<Void> renameAutoRelationshipName(
      Relationship relationship,
      String oldFkName,
      String oldPkName,
      String newFkName,
      String newPkName) {
    String oldBaseName = normalizeName("rel_" + oldFkName + "_to_" + oldPkName);
    OptionalInt suffixOpt = parseAutoRelationshipSuffix(relationship.name(), oldBaseName);
    if (suffixOpt.isEmpty()) {
      return Mono.empty();
    }
    String newBaseName = normalizeName("rel_" + newFkName + "_to_" + newPkName);
    int suffix = suffixOpt.getAsInt();
    return resolveUniqueRelationshipName(
        relationship.fkTableId(),
        newBaseName,
        relationship.id(),
        suffix)
        .flatMap(newName -> {
          if (newName.equals(relationship.name())) {
            return Mono.empty();
          }
          if (!isValidRelationshipName(newName)) {
            return Mono.empty();
          }
          return changeRelationshipNamePort.changeRelationshipName(relationship.id(), newName);
        });
  }

  private Mono<String> resolveUniqueRelationshipName(
      String fkTableId,
      String baseName,
      String excludeId,
      int suffix) {
    String candidate = suffix == 0 ? baseName : baseName + "_" + suffix;
    return relationshipExistsPort
        .existsByFkTableIdAndNameExcludingId(fkTableId, candidate, excludeId)
        .flatMap(exists -> {
          if (exists) {
            return resolveUniqueRelationshipName(fkTableId, baseName, excludeId, suffix + 1);
          }
          return Mono.just(candidate);
        });
  }

  private static OptionalInt parseAutoRelationshipSuffix(String relationshipName, String baseName) {
    if (relationshipName == null || baseName == null) {
      return OptionalInt.empty();
    }
    if (relationshipName.equals(baseName)) {
      return OptionalInt.of(0);
    }
    String prefix = baseName + "_";
    if (!relationshipName.startsWith(prefix)) {
      return OptionalInt.empty();
    }
    String suffix = relationshipName.substring(prefix.length());
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

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

  private static boolean isValidRelationshipName(String name) {
    try {
      RelationshipValidator.validateName(name);
      return true;
    } catch (RelationshipNameInvalidException exception) {
      return false;
    }
  }

}
