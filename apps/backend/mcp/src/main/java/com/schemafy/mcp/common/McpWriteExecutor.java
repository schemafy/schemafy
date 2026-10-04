package com.schemafy.mcp.common;

import java.util.Map;
import java.util.function.Function;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;

import com.schemafy.core.mcp.domain.McpScope;
import com.schemafy.core.project.application.access.ProjectAccessRequesterContext;
import com.schemafy.mcp.common.security.McpAuthenticatedPrincipal;
import com.schemafy.mcp.common.security.McpSecurityAuditLogger;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class McpWriteExecutor {

  private final McpResponseWriter responseWriter;
  private final McpSecurityAuditLogger auditLogger;

  public Mono<McpSchema.CallToolResult> execute(
      McpSchema.CallToolRequest request,
      String tool,
      McpScope scope,
      boolean confirmed,
      String targetId,
      Function<McpAuthenticatedPrincipal, Mono<?>> action) {
    return responseWriter.toolResult(principal().flatMap(principal -> {
      if (!principal.scopes().contains(scope.value())) {
        return reject(principal, tool, targetId, "MCP token scope is insufficient");
      }
      if (!confirmed) {
        return reject(principal, tool, targetId, "confirmed must be true for this MCP tool");
      }
      return responseWriter.toolPayload(Mono.defer(() -> action.apply(principal))
          .doOnSuccess(result -> auditLogger.writeToolSucceeded(
              principal, tool, targetId, isNoOp(result)))
          .doOnError(error -> auditLogger.writeToolFailed(
              principal, tool, targetId, error)));
    }));
  }

  public Mono<McpAuthenticatedPrincipal> principal() {
    return currentPrincipal();
  }

  private Mono<McpSchema.CallToolResult> reject(
      McpAuthenticatedPrincipal principal,
      String tool,
      String targetId,
      String message) {
    AccessDeniedException error = new AccessDeniedException(message);
    auditLogger.writeToolFailed(principal, tool, targetId, error);
    return Mono.just(responseWriter.toolError(message));
  }

  private Mono<McpAuthenticatedPrincipal> currentPrincipal() {
    return Mono.deferContextual(context -> ReactiveSecurityContextHolder.getContext()
        .map(securityContext -> securityContext.getAuthentication().getPrincipal())
        .cast(McpAuthenticatedPrincipal.class)
        .switchIfEmpty(Mono.error(new AccessDeniedException("MCP principal is missing")))
        .map(principal -> {
          String requesterId = ProjectAccessRequesterContext.requesterIdOrNull(context);
          if (requesterId == null || !requesterId.equals(principal.userId())) {
            throw new AccessDeniedException("MCP requester context is invalid");
          }
          return principal;
        }));
  }

  private boolean isNoOp(Object result) {
    return result instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get("noOp"));
  }

}
