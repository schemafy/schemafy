package com.schemafy.core.erd.relationship.application.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schemafy.core.collaboration.lock.CanvasEditLockErrorCode;
import com.schemafy.core.collaboration.lock.CanvasEditLockService;
import com.schemafy.core.collaboration.lock.CanvasEditLockTarget;
import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonObjectMetadataConverter;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipControlPointsCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipControlPointsUseCase;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipExtraCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipExtraUseCase;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.RELATIONSHIP;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = RELATIONSHIP, id = "relationshipId"))
public class ChangeRelationshipControlPointsService implements ChangeRelationshipControlPointsUseCase {

  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetSchemaByIdPort getSchemaByIdPort;
  private final ChangeRelationshipExtraUseCase changeRelationshipExtraUseCase;
  private final JsonObjectMetadataConverter metadataConverter;
  private final CanvasEditLockService lockService;
  private final ObjectMapper objectMapper;

  @Override
  public Mono<MutationResult<Void>> changeRelationshipControlPoints(
      ChangeRelationshipControlPointsCommand command) {
    return Mono.deferContextual(context -> {
      String sessionId = ErdOperationContexts.metadata(context).sessionId();
      if (lockService.isEnabled() && (sessionId == null || sessionId.isBlank())) {
        return Mono.error(new DomainException(CanvasEditLockErrorCode.REQUIRED));
      }
      return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
          .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.NOT_FOUND,
              "Relationship not found")))
          .flatMap(relationship -> getTableSchemaId(relationship.fkTableId())
              .flatMap(schemaId -> getSchemaByIdPort.findSchemaById(schemaId)
                  .flatMap(schema -> assertOwner(schema.projectId(), relationship.id(), sessionId)
                      .then(changeRelationshipExtraUseCase.changeRelationshipExtra(
                          new ChangeRelationshipExtraCommand(relationship.id(),
                              withControlPoints(relationship.extra(), command)))))));
    });
  }

  private Mono<String> getTableSchemaId(String tableId) {
    return getTableByIdPort.findTableById(tableId)
        .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Table not found")))
        .map(table -> table.schemaId());
  }

  private Mono<Void> assertOwner(String projectId, String relationshipId, String sessionId) {
    if (!lockService.isEnabled())
      return Mono.empty();
    return lockService.renew(projectId, CanvasEditLockTarget.RELATIONSHIP_CONTROL_POINTS,
        relationshipId, sessionId)
        .flatMap(result -> result.isOwner() || result.isUnavailable() ? Mono.empty()
            : Mono.error(new DomainException(CanvasEditLockErrorCode.NOT_OWNER)));
  }

  private JsonNode withControlPoints(String storedExtra,
      ChangeRelationshipControlPointsCommand command) {
    ObjectNode extra = metadataConverter.toOptionalJsonNode(storedExtra) instanceof ObjectNode node
        ? node.deepCopy()
        : objectMapper.createObjectNode();
    extra.set("controlPoint1", command.controlPoint1());
    if (command.controlPoint2() == null || command.controlPoint2().isNull()) {
      extra.remove("controlPoint2");
    } else {
      extra.set("controlPoint2", command.controlPoint2());
    }
    return extra;
  }

}
