package com.schemafy.mcp.collaboration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.collaboration.service.CollaborationEventPublisher;
import com.schemafy.core.erd.broadcast.ErdMutationBroadcaster;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.data.redis.enabled=true")
@ActiveProfiles("test")
@DisplayName("MCP collaboration publishing wiring")
class McpCollaborationPublishingIntegrationTest {

  @MockitoBean
  ReactiveStringRedisTemplate redisTemplate;

  @Autowired
  CollaborationEventPublisher eventPublisher;

  @Autowired
  ErdMutationBroadcaster mutationBroadcaster;

  @Test
  @DisplayName("Redis가 활성화되면 core 협업 발행 계층을 구성한다")
  void configuresCoreCollaborationPublishingWhenRedisIsEnabled() {
    assertThat(eventPublisher).isNotNull();
    assertThat(mutationBroadcaster).isNotNull();
  }

}
