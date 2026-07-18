package com.schemafy.api.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.schemafy.core.user.application.port.out.AuthMailPolicyPort;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.mail")
public class AuthMailProperties implements AuthMailPolicyPort {

  private boolean enabled = true;
  private String from = "no-reply@schemafy.com";

}
