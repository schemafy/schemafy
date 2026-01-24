package com.schemafy.domain.erd.relationship.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipNameUseCase;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipNamePort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.RelationshipExistsPort;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNameDuplicateException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.validator.RelationshipValidator;

import reactor.core.publisher.Mono;

@Service
public class ChangeRelationshipNameService implements ChangeRelationshipNameUseCase {

  private final ChangeRelationshipNamePort changeRelationshipNamePort;
  private final RelationshipExistsPort relationshipExistsPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;

  public ChangeRelationshipNameService(
      ChangeRelationshipNamePort changeRelationshipNamePort,
      RelationshipExistsPort relationshipExistsPort,
      GetRelationshipByIdPort getRelationshipByIdPort) {
    this.changeRelationshipNamePort = changeRelationshipNamePort;
    this.relationshipExistsPort = relationshipExistsPort;
    this.getRelationshipByIdPort = getRelationshipByIdPort;
  }

  @Override
  public Mono<Void> changeRelationshipName(ChangeRelationshipNameCommand command) {
    String normalizedName = normalizeName(command.newName());
    RelationshipValidator.validateName(normalizedName);
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new RelationshipNotExistException("Relationship not found")))
        .flatMap(relationship -> relationshipExistsPort.existsByFkTableIdAndNameExcludingId(
            relationship.fkTableId(),
            normalizedName,
            relationship.id())
            .flatMap(exists -> {
              if (exists) {
                return Mono.error(new RelationshipNameDuplicateException(
                    "Relationship name '%s' already exists in table".formatted(normalizedName)));
              }
              return changeRelationshipNamePort
                  .changeRelationshipName(relationship.id(), normalizedName);
            }));
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

}
