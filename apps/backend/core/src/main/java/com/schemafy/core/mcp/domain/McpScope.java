package com.schemafy.core.mcp.domain;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum McpScope {

  MCP("mcp"),
  WORKSPACE_WRITE("mcp:workspace:write"),
  WORKSPACE_DESTRUCTIVE("mcp:workspace:destructive"),
  ERD_WRITE("mcp:erd:write"),
  ERD_DESTRUCTIVE("mcp:erd:destructive"),
  MEMO_WRITE("mcp:memo:write"),
  MEMO_DESTRUCTIVE("mcp:memo:destructive"),
  MEMBERSHIP_ADMIN("mcp:membership:admin"),
  INVITATION_WRITE("mcp:invitation:write"),
  SHARE_LINK_READ("mcp:share-link:read"),
  SHARE_LINK_WRITE("mcp:share-link:write"),
  COLLABORATION_READ("mcp:collaboration:read");

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
