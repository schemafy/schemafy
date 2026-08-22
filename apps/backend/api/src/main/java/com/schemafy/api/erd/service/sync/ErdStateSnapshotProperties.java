package com.schemafy.api.erd.service.sync;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "collaboration.erd-state-snapshot")
public class ErdStateSnapshotProperties {

  private Duration debounce = Duration.ofMillis(100);
  private Duration maxWait = Duration.ofMillis(500);
  private Duration pollInterval = Duration.ofMillis(50);
  private Duration leaseTtl = Duration.ofSeconds(30);
  private Duration leaseRenewInterval = Duration.ofSeconds(10);
  private int batchSize = 20;
  private int workerConcurrency = 4;
  private Duration retryBackoff = Duration.ofMillis(100);
  private Duration maxRetryBackoff = Duration.ofMillis(400);
  private Duration requeueBackoff = Duration.ofSeconds(1);
  private Duration maxRequeueBackoff = Duration.ofSeconds(30);
  private Duration completedWatermarkTtl = Duration.ofHours(4);

}
