package com.schemafy.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

import com.schemafy.api.erd.service.sync.ErdStateSnapshotProducer;
import com.schemafy.api.erd.service.sync.ErdStateSnapshotScheduler;
import com.schemafy.api.erd.service.sync.ErdStateSnapshotWorker;
import com.schemafy.api.erd.service.sync.RedisErdStateSnapshotJobStore;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ApiApplicationTests {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void contextLoads() {}

  @Test
  void redisDisabledDoesNotCreateSnapshotCoordinationBeans() {
    assertThat(applicationContext.getBeansOfType(
        RedisErdStateSnapshotJobStore.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(
        ErdStateSnapshotProducer.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(
        ErdStateSnapshotWorker.class)).isEmpty();
    assertThat(applicationContext.getBeansOfType(
        ErdStateSnapshotScheduler.class)).isEmpty();
  }

}
