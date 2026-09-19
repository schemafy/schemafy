package com.schemafy.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.api.erd.service.sync.ErdStateSnapshotScheduler;
import com.schemafy.api.erd.service.sync.ErdStateSnapshotWorker;
import com.schemafy.api.erd.service.sync.RedisErdStateSnapshotJobStore;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ApiApplication 컨텍스트 테스트")
class ApiApplicationTests {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  @DisplayName("애플리케이션 컨텍스트가 로드된다")
  void contextLoads() {}

  @Test
  @DisplayName("Redis 비활성화 시 snapshot coordination bean을 생성하지 않는다")
  void redisDisabledDoesNotCreateSnapshotCoordinationBeans() {
    assertThat(applicationContext.getBeansOfType(
        RedisErdStateSnapshotJobStore.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(
        ErdStateSnapshotWorker.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(
        ErdStateSnapshotScheduler.class)).isEmpty();
  }

}
