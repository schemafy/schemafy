package com.schemafy.core.erd.broadcast;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.schemafy.core.collaboration.constant.CollaborationConstants;
import com.schemafy.core.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.core.collaboration.service.CollaborationEventPublisher;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;

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

  public Mono<Void> broadcast(Set<String> affectedTableIds) {
    if (affectedTableIds == null || affectedTableIds.isEmpty()) {
      return Mono.empty();
    }
    String anyTableId = affectedTableIds.iterator().next();
    return resolveFromTableId(anyTableId)
        .flatMap(ctx -> publish(ctx, affectedTableIds))
        .doOnError(e -> log.warn(
            "[ErdMutationBroadcaster] broadcast failed: {}",
            e.getMessage()))
        .onErrorResume(e -> Mono.empty());
  }

  public Mono<Void> broadcastSchemaChange(String schemaId) {
    return resolveFromSchemaId(schemaId)
        .flatMap(ctx -> publish(ctx, Set.of()))
        .doOnError(e -> log.warn(
            "[ErdMutationBroadcaster] broadcastSchemaChange failed: schemaId={}, error={}",
            schemaId, e.getMessage()))
        .onErrorResume(e -> Mono.empty());
  }

  public Mono<Void> broadcastWithContext(ResolvedContext ctx,
      Set<String> affectedTableIds) {
    return publish(ctx, affectedTableIds);
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
      Set<String> affectedTableIds) {
    return Mono.deferContextual(reactorCtx -> {
      String sessionId = reactorCtx.getOrDefault(
          CollaborationConstants.SESSION_ID_CONTEXT_KEY, null);
      return eventPublisher.publish(ctx.projectId(),
          CollaborationOutboundFactory.erdMutated(sessionId,
              ctx.schemaId(), affectedTableIds));
    });
  }

}
