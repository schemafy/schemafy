package com.schemafy.core.erd.sync;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "collaboration.erd-state-snapshot")
public class ErdStateSnapshotEnqueueProperties {

  private Duration debounce = Duration.ofMillis(100);
  private Duration maxWait = Duration.ofMillis(500);
  private Duration retryBackoff = Duration.ofMillis(100);
  private Duration maxRetryBackoff = Duration.ofMillis(400);
  private Duration completedWatermarkTtl = Duration.ofHours(4);

}
