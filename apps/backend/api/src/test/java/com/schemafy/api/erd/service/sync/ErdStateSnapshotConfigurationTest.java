package com.schemafy.api.erd.service.sync;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.api.ApiApplication;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ERD state snapshot 설정 테스트")
class ErdStateSnapshotConfigurationTest {

  @Test
  @DisplayName("승인된 coalescing 기본값을 사용한다")
  void usesTheApprovedCoalescingDefaults() {
    ErdStateSnapshotProperties properties = new ErdStateSnapshotProperties();

    assertThat(properties.getDebounce()).isEqualTo(Duration.ofMillis(100));
    assertThat(properties.getMaxWait()).isEqualTo(Duration.ofMillis(500));
  }

  @Test
  @DisplayName("snapshot job polling을 활성화한다")
  void enablesSnapshotJobPolling() {
    assertThat(AnnotatedElementUtils.hasAnnotation(ApiApplication.class,
        EnableScheduling.class)).isTrue();
  }

  @Test
  @DisplayName("분산 coordination 설정값을 바인딩한다")
  void bindsDistributedCoordinationProperties() {
    new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class)
        .withPropertyValues(
            "collaboration.erd-state-snapshot.debounce=120ms",
            "collaboration.erd-state-snapshot.max-wait=700ms",
            "collaboration.erd-state-snapshot.lease-ttl=40s",
            "collaboration.erd-state-snapshot.worker-concurrency=6")
        .run(context -> {
          assertThat(context).hasNotFailed();
          ErdStateSnapshotProperties properties = context.getBean(
              ErdStateSnapshotProperties.class);
          assertThat(properties.getDebounce()).isEqualTo(Duration.ofMillis(120));
          assertThat(properties.getMaxWait()).isEqualTo(Duration.ofMillis(700));
          assertThat(properties.getLeaseTtl()).isEqualTo(Duration.ofSeconds(40));
          assertThat(properties.getWorkerConcurrency()).isEqualTo(6);
        });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(ErdStateSnapshotProperties.class)
  static class PropertiesConfiguration {
  }

}
