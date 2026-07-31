package com.schemafy.core.erd.relationship.application.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.inverse.ChangeRelationshipNameInverse;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameUseCase;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipNamePort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.RelationshipExistsPort;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.relationship.domain.validator.RelationshipValidator;
import com.schemafy.core.erd.vendor.application.service.IdentifierCapabilityResolver;
import com.schemafy.core.erd.vendor.domain.validator.IdentifierValidator;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.RELATIONSHIP;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = RELATIONSHIP, id = "relationshipId"))
public class ChangeRelationshipNameService implements ChangeRelationshipNameUseCase {

  private final ChangeRelationshipNamePort changeRelationshipNamePort;
  private final RelationshipExistsPort relationshipExistsPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final IdentifierCapabilityResolver identifierCapabilityResolver;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeRelationshipName(ChangeRelationshipNameCommand command) {
    return Mono.defer(() -> {
      String normalizedName = normalizeName(command.newName());
      RelationshipValidator.validateName(normalizedName);
      return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
          .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.NOT_FOUND, "Relationship not found")))
          .flatMap(relationship -> identifierCapabilityResolver.resolve(RELATIONSHIP, relationship.id())
              .flatMap(identifierCapabilities -> {
                IdentifierValidator.validateLength(
                    identifierCapabilities,
                    normalizedName,
                    RelationshipErrorCode.NAME_INVALID,
                    "Relationship name");
                Set<String> affectedTableIds = new HashSet<>();
                affectedTableIds.add(relationship.fkTableId());
                affectedTableIds.add(relationship.pkTableId());
                if (normalizedName.equals(relationship.name())) {
                  return Mono.just(MutationResult.<Void>noop(null, affectedTableIds));
                }
                return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_RELATIONSHIP_NAME, command,
                    () -> getRelationshipByIdPort.findRelationshipById(command.relationshipId())
                        .switchIfEmpty(Mono.error(new DomainException(
                            RelationshipErrorCode.NOT_FOUND,
                            "Relationship not found")))
                        .flatMap(lockedRelationship -> {
                          Set<String> lockedAffectedTableIds = affectedTableIds(lockedRelationship.fkTableId(),
                              lockedRelationship.pkTableId());
                          if (normalizedName.equals(lockedRelationship.name())) {
                            return Mono.just(MutationResult.<Void>noop(null, lockedAffectedTableIds));
                          }
                          return relationshipExistsPort.existsByFkTableIdAndNameExcludingId(
                              lockedRelationship.fkTableId(),
                              normalizedName,
                              lockedRelationship.id())
                              .flatMap(exists -> {
                                if (exists) {
                                  return Mono.error(new DomainException(RelationshipErrorCode.NAME_DUPLICATE,
                                      "Relationship name '%s' already exists in table".formatted(normalizedName)));
                                }
                                return changeRelationshipNamePort
                                    .changeRelationshipName(lockedRelationship.id(), normalizedName)
                                    .thenReturn(MutationResult.<Void>of(null, lockedAffectedTableIds)
                                        .withInverse(new ChangeRelationshipNameInverse(
                                            lockedRelationship.id(),
                                            lockedRelationship.name())));
                              });
                        }));
              }));
    });
  }

  private static Set<String> affectedTableIds(String fkTableId, String pkTableId) {
    Set<String> affectedTableIds = new HashSet<>();
    affectedTableIds.add(fkTableId);
    affectedTableIds.add(pkTableId);
    return affectedTableIds;
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

}
