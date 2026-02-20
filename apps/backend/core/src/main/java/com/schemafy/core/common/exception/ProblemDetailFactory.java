package com.schemafy.core.common.exception;

import java.net.URI;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.schemafy.domain.common.exception.DomainErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProblemDetailFactory {

  private final ProblemProperties problemProperties;

  public ProblemDetail create(ServerWebExchange exchange,
      DomainErrorCode errorCode, String message) {
    HttpStatus status = errorCode.status();
    String title = status.getReasonPhrase();
    String detail = (message != null && !message.isBlank())
        ? message
        : title;
    String reason = errorCode.code();

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        status, detail);
    problemDetail.setTitle(title);
    problemDetail.setType(resolveType(reason));
    problemDetail.setInstance(resolveInstance(exchange));
    problemDetail.setProperty("reason", reason);
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
