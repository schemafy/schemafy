package com.schemafy.api.common.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.schemafy.api.collaboration.constant.CollaborationConstants;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

  private List<String> allowedOrigins = List.of("http://localhost:3000");

  private final List<String> allowedMethods = List.of("GET", "POST", "PUT",
      "PATCH", "DELETE", "OPTIONS");

  private final List<String> allowedHeaders = List.of("Authorization",
      "Content-Type", "X-Requested-With", "Accept",
      "X-Hmac-Signature", "X-Hmac-Timestamp", "X-Hmac-Nonce",
      CollaborationConstants.SESSION_ID_HEADER,
      CollaborationConstants.CLIENT_OPERATION_ID_HEADER,
      CollaborationConstants.BASE_SCHEMA_REVISION_HEADER);

  private final List<String> exposedHeaders = List.of("Authorization",
      "Content-Type");

  /** 자격증명 허용 여부 (true면 "*" 금지) */
  private boolean allowCredentials = true;

  private long maxAgeSeconds = 3600;

}
