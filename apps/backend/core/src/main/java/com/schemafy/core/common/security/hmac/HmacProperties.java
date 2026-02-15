package com.schemafy.core.common.security.hmac;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "hmac")
public class HmacProperties {

  private String secret = "default-hmac-secret-change-me-in-production";

  private String previousSecret;

  private int timestampToleranceSeconds = 30;

  private boolean enabled = true;

  private EnforcementMode enforcementMode = EnforcementMode.ENFORCE;

  public enum EnforcementMode {
    ENFORCE, LOG_ONLY
  }

}
