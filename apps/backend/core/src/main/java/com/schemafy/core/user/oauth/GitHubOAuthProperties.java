package com.schemafy.core.user.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "oauth.github")
public class GitHubOAuthProperties {

  private String clientId = "";
  private String clientSecret = "";
  private String redirectUri = "";
  private String frontendCallbackUrl = "";

}
