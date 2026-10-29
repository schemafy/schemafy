package com.schemafy.core.erd.relationship.application.service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.core.collaboration.lock.CanvasEditLockTarget;
import com.schemafy.core.collaboration.lock.CanvasExtraMutationPolicy;
import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonObjectMetadataConverter;
import com.schemafy.core.erd.operation.application.inverse.ChangeRelationshipExtraInverse;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipExtraCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipExtraUseCase;
import com.schemafy.core.erd.relationship.application.port.out.ChangeRelationshipExtraPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.RELATIONSHIP;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = RELATIONSHIP, id = "relationshipId"))
public class ChangeRelationshipExtraService implements ChangeRelationshipExtraUseCase {

  private final ChangeRelationshipExtraPort changeRelationshipExtraPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final JsonObjectMetadataConverter jsonObjectMetadataConverter;
  private final CanvasExtraMutationPolicy canvasPolicy;

  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeRelationshipExtra(ChangeRelationshipExtraCommand command) {
    return Mono.defer(() -> {
      String canonicalExtra = jsonObjectMetadataConverter.toStorageJson(command.extra());
      return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
          .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.NOT_FOUND, "Relationship not found")))
          .flatMap(relationship -> {
            Set<String> affectedTableIds = new HashSet<>();
            affectedTableIds.add(relationship.fkTableId());
            affectedTableIds.add(relationship.pkTableId());
            if (!CanvasExtraMutationPolicy.isCanvasRequest(CanvasEditLockTarget.RELATIONSHIP_CONTROL_POINTS, command
                .extra())
                && Objects.equals(relationship.extra(), canonicalExtra)) {
              return Mono.just(MutationResult.<Void>noop(null, affectedTableIds));
            }
            return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_RELATIONSHIP_EXTRA, command,
                () -> getRelationshipByIdPort.findRelationshipById(command.relationshipId())
                    .switchIfEmpty(Mono.error(new DomainException(
                        RelationshipErrorCode.NOT_FOUND,
                        "Relationship not found")))
                    .flatMap(lockedRelationship -> {
                      Set<String> lockedAffectedTableIds = affectedTableIds(lockedRelationship.fkTableId(),
                          lockedRelationship.pkTableId());
                      Mono<String> prepared = canvasPolicy.prepare(CanvasEditLockTarget.RELATIONSHIP_CONTROL_POINTS,
                          lockedRelationship
                              .id(),
                          lockedRelationship.extra(), command.extra());
                      return prepared.defaultIfEmpty("").flatMap(value -> {
                        String nextExtra = value.isEmpty() ? null : value;
                        if (Objects.equals(lockedRelationship.extra(), nextExtra)) {
                          return Mono.just(MutationResult.<Void>noop(null, lockedAffectedTableIds));
                        }
                        return changeRelationshipExtraPort
                            .changeRelationshipExtra(lockedRelationship.id(), nextExtra)
                            .thenReturn(MutationResult.<Void>of(null, lockedAffectedTableIds)
                                .withInverse(new ChangeRelationshipExtraInverse(
                                    lockedRelationship.id(),
                                    lockedRelationship.extra())));
                      });
                    }));
          });
    });
  }

  private static Set<String> affectedTableIds(String fkTableId, String pkTableId) {
    Set<String> affectedTableIds = new HashSet<>();
    affectedTableIds.add(fkTableId);
    affectedTableIds.add(pkTableId);
    return affectedTableIds;
  }

}
