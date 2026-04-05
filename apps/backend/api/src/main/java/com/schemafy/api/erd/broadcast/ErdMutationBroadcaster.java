package com.schemafy.api.erd.broadcast;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.schemafy.api.collaboration.constant.CollaborationConstants;
import com.schemafy.api.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.api.collaboration.service.CollaborationEventPublisher;
import com.schemafy.api.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class ErdMutationBroadcaster {

  private final GetTableByIdPort getTableByIdPort;
  private final GetSchemaByIdPort getSchemaByIdPort;
  private final CollaborationEventPublisher eventPublisher;

  public record ResolvedContext(String projectId, String schemaId) {
  }

  public Mono<Void> broadcast(Set<String> affectedTableIds,
      CommittedErdOperation operation) {
    if (affectedTableIds == null || affectedTableIds.isEmpty()) {
      return Mono.empty();
    }
    String anyTableId = affectedTableIds.iterator().next();
    return resolveFromTableId(anyTableId)
        .flatMap(ctx -> publish(ctx, affectedTableIds, operation))
        .doOnError(e -> log.warn(
            "[ErdMutationBroadcaster] broadcast failed: {}",
            e.getMessage()))
        .onErrorResume(e -> Mono.empty());
  }

  public Mono<Void> broadcastSchemaChange(String schemaId,
      CommittedErdOperation operation) {
    return resolveFromSchemaId(schemaId)
        .flatMap(ctx -> publish(ctx, Set.of(), operation))
        .doOnError(e -> log.warn(
            "[ErdMutationBroadcaster] broadcastSchemaChange failed: schemaId={}, error={}",
            schemaId, e.getMessage()))
        .onErrorResume(e -> Mono.empty());
  }

  public Mono<Void> broadcastWithContext(ResolvedContext ctx,
      Set<String> affectedTableIds,
      CommittedErdOperation operation) {
    return publish(ctx, affectedTableIds, operation)
        .doOnError(e -> log.warn(
            "[ErdMutationBroadcaster] broadcastWithContext failed: {}",
            e.getMessage()))
        .onErrorResume(e -> Mono.empty());
  }

  public Mono<ResolvedContext> resolveFromSchemaId(String schemaId) {
    return getSchemaByIdPort.findSchemaById(schemaId)
        .map(schema -> new ResolvedContext(schema.projectId(),
            schema.id()));
  }

  public Mono<ResolvedContext> resolveFromTableId(String tableId) {
    return getTableByIdPort.findTableById(tableId)
        .flatMap(table -> resolveFromSchemaId(table.schemaId()));
  }

  private Mono<Void> publish(ResolvedContext ctx,
      Set<String> affectedTableIds,
      CommittedErdOperation operation) {
    return Mono.deferContextual(reactorCtx -> {
      String sessionId = reactorCtx.getOrDefault(
          CollaborationConstants.SESSION_ID_CONTEXT_KEY, null);
      return eventPublisher.publish(ctx.projectId(),
          CollaborationOutboundFactory.erdMutated(sessionId,
              ctx.schemaId(), affectedTableIds, operation));
    });
  }

}
