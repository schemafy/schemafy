package com.schemafy.core.erd.table.application.service;

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
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraUseCase;
import com.schemafy.core.erd.table.application.port.in.ChangeTablePositionCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTablePositionUseCase;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.TABLE;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = TABLE, id = "tableId"))
public class ChangeTablePositionService implements ChangeTablePositionUseCase {

  private final GetTableByIdPort getTableByIdPort;
  private final GetSchemaByIdPort getSchemaByIdPort;
  private final ChangeTableExtraUseCase changeTableExtraUseCase;
  private final JsonObjectMetadataConverter metadataConverter;
  private final CanvasEditLockService lockService;
  private final ObjectMapper objectMapper;

  @Override
  public Mono<MutationResult<Void>> changeTablePosition(ChangeTablePositionCommand command) {
    return Mono.deferContextual(context -> {
      String sessionId = ErdOperationContexts.metadata(context).sessionId();
      if (lockService.isEnabled() && (sessionId == null || sessionId.isBlank())) {
        return Mono.error(new DomainException(CanvasEditLockErrorCode.REQUIRED));
      }
      return getTableByIdPort.findTableById(command.tableId())
          .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Table not found")))
          .flatMap(table -> getSchemaByIdPort.findSchemaById(table.schemaId())
              .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Schema not found")))
              .flatMap(schema -> assertOwner(schema.projectId(), table.id(), sessionId)
                  .then(changeTableExtraUseCase.changeTableExtra(new ChangeTableExtraCommand(table.id(),
                      withPosition(table.extra(), command.position()))))));
    });
  }

  private Mono<Void> assertOwner(String projectId, String tableId, String sessionId) {
    if (!lockService.isEnabled())
      return Mono.empty();
    return lockService.renew(projectId, CanvasEditLockTarget.TABLE_POSITION, tableId, sessionId)
        .flatMap(result -> result.isOwner() || result.isUnavailable() ? Mono.empty()
            : Mono.error(new DomainException(CanvasEditLockErrorCode.NOT_OWNER)));
  }

  private JsonNode withPosition(String storedExtra, JsonNode position) {
    ObjectNode extra = metadataConverter.toOptionalJsonNode(storedExtra) instanceof ObjectNode node
        ? node.deepCopy()
        : objectMapper.createObjectNode();
    extra.set("position", position);
    return extra;
  }

}
