package com.schemafy.core.mcp.domain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class McpTokenClaimSupport {

  public static final String TYPE = "type";
  public static final String SCOPE = "scope";
  public static final String SCOPES = "scopes";
  public static final String SCP = "scp";

  private McpTokenClaimSupport() {}

  public static Set<String> extractScopes(
      Function<String, Object> claimResolver) {
    Objects.requireNonNull(claimResolver, "claimResolver must not be null");
    return scopesFrom(
        claimResolver.apply(SCOPE),
        claimResolver.apply(SCOPES),
        claimResolver.apply(SCP));
  }

  public static Set<String> scopesFrom(Object... values) {
    Set<String> scopes = new LinkedHashSet<>();
    if (values == null) {
      return scopes;
    }
    for (Object value : values) {
      addScopes(scopes, value);
    }
    return scopes;
  }

  public static String canonicalScopeValue(Set<String> scopes) {
    Objects.requireNonNull(scopes, "scopes must not be null");
    return scopes.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(scope -> !scope.isBlank())
        .sorted()
        .collect(java.util.stream.Collectors.joining(" "));
  }

  public static Set<String> scopesFromStoredValue(String scope) {
    return Set.copyOf(scopesFrom(scope));
  }

  private static void addScopes(Set<String> scopes, Object value) {
    if (value == null) {
      return;
    }
    if (value instanceof Collection<?> values) {
      values.forEach(item -> addScopes(scopes, item));
      return;
    }
    String stringValue = value.toString();
    for (String scope : stringValue.split("[\\s,]+")) {
      String normalizedScope = scope.trim();
      if (!normalizedScope.isBlank()) {
        scopes.add(normalizedScope);
      }
    }
  }

}
