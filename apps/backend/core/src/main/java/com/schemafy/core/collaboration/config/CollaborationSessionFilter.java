package com.schemafy.core.collaboration.config;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.schemafy.core.collaboration.constant.CollaborationConstants;

import reactor.core.publisher.Mono;

@Component
public class CollaborationSessionFilter implements WebFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange,
      WebFilterChain chain) {
    String sessionId = exchange.getRequest().getHeaders()
        .getFirst(CollaborationConstants.SESSION_ID_HEADER);
    if (sessionId != null) {
      return chain.filter(exchange)
          .contextWrite(ctx -> ctx.put(
              CollaborationConstants.SESSION_ID_CONTEXT_KEY,
              sessionId));
    }
    return chain.filter(exchange);
  }

}
