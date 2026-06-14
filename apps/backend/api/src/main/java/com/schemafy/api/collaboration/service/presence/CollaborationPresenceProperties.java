package com.schemafy.api.collaboration.service.presence;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "collaboration.presence")
public class CollaborationPresenceProperties {

  private Duration sessionTtl = Duration.ofSeconds(90);
  private Duration heartbeatInterval = Duration.ofSeconds(30);
  private Duration cleanupInterval = Duration.ofSeconds(30);

  public Duration getSessionTtl() { return sessionTtl; }

  public void setSessionTtl(Duration sessionTtl) { this.sessionTtl = sessionTtl; }

  public Duration getHeartbeatInterval() { return heartbeatInterval; }

  public void setHeartbeatInterval(Duration heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }

  public Duration getCleanupInterval() { return cleanupInterval; }

  public void setCleanupInterval(Duration cleanupInterval) { this.cleanupInterval = cleanupInterval; }

}
