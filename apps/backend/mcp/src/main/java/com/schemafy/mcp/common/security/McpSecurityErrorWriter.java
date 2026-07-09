package com.schemafy.mcp.common.security;

import java.net.URI;
import java.util.Locale;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.schemafy.core.common.json.JsonCodec;

import reactor.core.publisher.Mono;

@Component
public class McpSecurityErrorWriter {

  private final JsonCodec jsonCodec;
  private final McpProblemProperties problemProperties;

  public McpSecurityErrorWriter(
      JsonCodec jsonCodec,
      McpProblemProperties problemProperties) {
    this.jsonCodec = jsonCodec;
    this.problemProperties = problemProperties;
  }

  public Mono<Void> write(ServerWebExchange exchange, McpSecurityError error) {
    var response = exchange.getResponse();
    if (response.isCommitted()) {
      return Mono.empty();
    }
    response.setStatusCode(error.status());
    response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    try {
      byte[] body = jsonCodec.toJsonBytes(createProblemDetail(exchange, error));
      return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    } catch (IllegalArgumentException e) {
      return response.setComplete();
    }
  }

  private ProblemDetail createProblemDetail(ServerWebExchange exchange,
      McpSecurityError error) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        error.status(), error.message());
    problemDetail.setTitle(error.status().getReasonPhrase());
    problemDetail.setType(resolveType(error.code()));
    problemDetail.setInstance(resolveInstance(exchange));
    problemDetail.setProperty("reason", error.code());
    return problemDetail;
  }

  private URI resolveType(String reason) {
    String baseUri = problemProperties.getTypeBaseUri();
    if (baseUri == null || baseUri.isBlank()
        || "about:blank".equals(baseUri)) {
      return URI.create("about:blank");
    }

    String normalizedBase = baseUri.endsWith("/")
        ? baseUri.substring(0, baseUri.length() - 1)
        : baseUri;
    String reasonPath = reason.toLowerCase(Locale.ROOT)
        .replace('_', '-');
    return URI.create(normalizedBase + "/" + reasonPath);
  }

  private URI resolveInstance(ServerWebExchange exchange) {
    String path = exchange.getRequest().getPath()
        .pathWithinApplication().value();
    if (path == null || path.isBlank()) {
      return URI.create("/");
    }
    return URI.create(path);
  }

}
