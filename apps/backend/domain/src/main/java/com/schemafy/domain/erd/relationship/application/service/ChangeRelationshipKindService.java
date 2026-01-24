package com.schemafy.domain.erd.relationship.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipKindUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipKindPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipTargetTableNotExistException;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.relationship.domain.validator.RelationshipValidator;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;

import reactor.core.publisher.Mono;

@Service
public class ChangeRelationshipKindService implements ChangeRelationshipKindUseCase {

  private final ChangeRelationshipKindPort changeRelationshipKindPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort;

  public ChangeRelationshipKindService(
      ChangeRelationshipKindPort changeRelationshipKindPort,
      GetRelationshipByIdPort getRelationshipByIdPort,
      GetTableByIdPort getTableByIdPort,
      GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort) {
    this.changeRelationshipKindPort = changeRelationshipKindPort;
    this.getRelationshipByIdPort = getRelationshipByIdPort;
    this.getTableByIdPort = getTableByIdPort;
    this.getRelationshipsBySchemaIdPort = getRelationshipsBySchemaIdPort;
  }

  @Override
  public Mono<Void> changeRelationshipKind(ChangeRelationshipKindCommand command) {
    if (command.kind() == null) {
      return Mono.error(new IllegalArgumentException("Relationship kind is required"));
    }
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new RelationshipNotExistException("Relationship not found")))
        .flatMap(relationship -> {
          if (command.kind() != RelationshipKind.IDENTIFYING) {
            return changeRelationshipKindPort.changeRelationshipKind(
                relationship.id(),
                command.kind());
          }
          return getTableByIdPort.findTableById(relationship.fkTableId())
              .switchIfEmpty(Mono.error(new RelationshipTargetTableNotExistException(
                  "Relationship fk table not found")))
              .flatMap(table -> getRelationshipsBySchemaIdPort
                  .findRelationshipsBySchemaId(table.schemaId())
                  .defaultIfEmpty(List.of())
                  .flatMap(relationships -> validateCycleAndChange(
                      relationship,
                      relationships,
                      command)));
        });
  }

  private Mono<Void> validateCycleAndChange(
      Relationship relationship,
      List<Relationship> relationships,
      ChangeRelationshipKindCommand command) {
    RelationshipValidator.validateIdentifyingCycle(
        relationships,
        new RelationshipValidator.RelationshipKindChange(
            relationship.id(),
            command.kind()),
        null);
    return changeRelationshipKindPort.changeRelationshipKind(
        relationship.id(),
        command.kind());
  }

}
