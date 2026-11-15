package com.schemafy.core.collaboration.lock;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonObjectMetadataConverter;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.project.application.port.out.ProjectPresenceReadPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CanvasExtraMutationPolicy {

  private final CanvasEditLockService locks;
  private final GetTableByIdPort tables;
  private final GetRelationshipByIdPort relationships;
  private final GetSchemaByIdPort schemas;
  private final JsonObjectMetadataConverter converter;
  private final ProjectPresenceReadPort presence;

  public static boolean isCanvasRequest(CanvasEditLockTarget target, JsonNode requested) {
    return requested != null && fields(target).stream().anyMatch(requested::has);
  }

  public Mono<String> prepare(CanvasEditLockTarget target, String resourceId,
      String stored, JsonNode requested) {
    return Mono.deferContextual(context -> {
      boolean derived = ErdOperationContexts.metadata(context)
          .derivationKindOrDefault() != ErdOperationDerivationKind.ORIGINAL;
      if (derived) {
        return Mono.justOrEmpty(converter.toStorageJson(requested));
      }
      if (!isCanvasRequest(target, requested)) {
        JsonNode current = converter.toOptionalJsonNode(stored);
        ObjectNode next = requested instanceof ObjectNode object ? object.deepCopy()
            : JsonNodeFactory.instance.objectNode();
        if (current != null)
          fields(target).stream().filter(current::has)
              .forEach(field -> next.set(field, current.get(field)));
        return next.isEmpty() && (requested == null || requested.isNull())
            ? Mono.empty()
            : Mono.just(converter.toStorageJson(next));
      }
      Mono<Void> owner = Mono.empty();
      if (locks.isEnabled()) {
        var metadata = ErdOperationContexts.metadata(context);
        owner = projectId(target, resourceId).flatMap(project -> verifyOwner(project, target,
            resourceId, metadata.sessionId(), metadata.actorUserId()));
      }
      return owner.then(Mono.fromSupplier(() -> merge(target, stored, requested)));
    });
  }

  private Mono<Void> verifyOwner(String projectId, CanvasEditLockTarget target,
      String resourceId, String sessionId, String userId) {
    if (sessionId == null || sessionId.isBlank() || userId == null || userId.isBlank()) {
      return Mono.error(new DomainException(CanvasEditLockErrorCode.NOT_OWNER));
    }
    return presence.findSession(projectId, sessionId)
        .map(session -> userId.equals(session.userId()))
        .defaultIfEmpty(false)
        // Preserve the existing fail-open policy when Redis presence is unavailable.
        .onErrorReturn(DataAccessException.class, true)
        .flatMap(matches -> matches ? locks.renew(projectId, target, resourceId, sessionId)
            : Mono.error(new DomainException(CanvasEditLockErrorCode.NOT_OWNER)))
        .flatMap(result -> result.isUnavailable() || result.isOwner() ? Mono.empty()
            : Mono.error(new DomainException(CanvasEditLockErrorCode.NOT_OWNER)));
  }

  private Mono<String> projectId(CanvasEditLockTarget target, String id) {
    Mono<String> tableId = target == CanvasEditLockTarget.TABLE_POSITION ? Mono.just(id)
        : relationships.findRelationshipById(id).map(rel -> rel.fkTableId());
    return tableId.flatMap(tables::findTableById)
        .flatMap(table -> schemas.findSchemaById(table.schemaId()))
        .map(schema -> schema.projectId())
        .switchIfEmpty(Mono.error(new DomainException(CanvasEditLockErrorCode.NOT_OWNER)));
  }

  private String merge(CanvasEditLockTarget target, String stored, JsonNode requested) {
    JsonNode current = converter.toOptionalJsonNode(stored);
    ObjectNode merged = current instanceof ObjectNode object ? object.deepCopy()
        : JsonNodeFactory.instance.objectNode();
    fields(target).stream().filter(requested::has).forEach(field -> {
      if (requested.get(field).isNull())
        merged.remove(field);
      else
        merged.set(field, requested.get(field));
    });
    return converter.toStorageJson(merged);
  }

  private static List<String> fields(CanvasEditLockTarget target) {
    return target == CanvasEditLockTarget.TABLE_POSITION ? List.of("position")
        : List.of("controlPoint1", "controlPoint2");
  }

}
