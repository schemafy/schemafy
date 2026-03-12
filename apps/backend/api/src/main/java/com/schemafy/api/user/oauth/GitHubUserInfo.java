package com.schemafy.api.user.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubUserInfo(
    long id,
    String login,
    String email,
    String name) {

  public String displayName() {
    return name != null ? name : login;
  }

}
