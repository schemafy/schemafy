package com.schemafy.core.mcp.domain;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum McpScope {

  MCP("mcp"),
  WORKSPACE_WRITE("mcp:workspace:write"),
  ERD_WRITE("mcp:erd:write"),
  MEMO_WRITE("mcp:memo:write");

  private static final Set<String> ISSUABLE_VALUES = Arrays.stream(values())
      .map(McpScope::value)
      .collect(Collectors.toUnmodifiableSet());

  private final String value;

  McpScope(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static Set<String> issuableValues() {
    return ISSUABLE_VALUES;
  }

}
