package com.schemafy.api.collaboration.config;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.schemafy.api.collaboration.constant.CollaborationConstants;
import com.schemafy.core.erd.operation.ErdOperationContexts;

import reactor.core.publisher.Mono;

@Component
public class CollaborationSessionFilter implements WebFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange,
      WebFilterChain chain) {
    String sessionId = exchange.getRequest().getHeaders()
        .getFirst(CollaborationConstants.SESSION_ID_HEADER);
    String clientOperationId = exchange.getRequest().getHeaders()
        .getFirst(CollaborationConstants.CLIENT_OPERATION_ID_HEADER);
    Long baseSchemaRevision = parseBaseSchemaRevision(exchange);

    return chain.filter(exchange)
        .contextWrite(context -> {
          var nextContext = context;
          if (StringUtils.hasText(sessionId)) {
            nextContext = nextContext.put(
                CollaborationConstants.SESSION_ID_CONTEXT_KEY,
                sessionId);
            nextContext = ErdOperationContexts.withSessionId(sessionId)
                .apply(nextContext);
          }
          if (StringUtils.hasText(clientOperationId)) {
            nextContext = ErdOperationContexts.withClientOperationId(clientOperationId)
                .apply(nextContext);
          }
          if (baseSchemaRevision != null) {
            nextContext = ErdOperationContexts.withBaseSchemaRevision(baseSchemaRevision)
                .apply(nextContext);
          }
          return nextContext;
        });
  }

  private Long parseBaseSchemaRevision(ServerWebExchange exchange) {
    String rawRevision = exchange.getRequest().getHeaders()
        .getFirst(CollaborationConstants.BASE_SCHEMA_REVISION_HEADER);
    if (!StringUtils.hasText(rawRevision)) {
      return null;
    }
    try {
      return Long.parseLong(rawRevision);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

}
