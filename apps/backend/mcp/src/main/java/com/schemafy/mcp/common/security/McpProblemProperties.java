package com.schemafy.mcp.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "problem")
public class McpProblemProperties {

  private String typeBaseUri = "about:blank";

  public String getTypeBaseUri() { return typeBaseUri; }

  public void setTypeBaseUri(String typeBaseUri) { this.typeBaseUri = typeBaseUri; }

}
