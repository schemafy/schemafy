package com.schemafy.api.cache.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.api.cache.config.CacheType;
import com.schemafy.api.cache.service.dto.CacheStatsDto;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CacheRouter")
class CacheRouterTest {

  @Autowired
  private CacheRouter cacheRouter;

  @MockitoBean
  @Qualifier("redisCacheService")
  private CacheService redisCacheService;

  @Test
  @DisplayName("Caffeine 캐시에 값을 저장하고 조회한다")
  void testPutAndGetWithCaffeine() {
    String key = "test-caffeine-key";
    String value = "test-caffeine-value";

    StepVerifier.create(
        cacheRouter.put(key, value, CacheType.CAFFEINE)
            .then(cacheRouter.get(key, CacheType.CAFFEINE)))
        .expectNext(value)
        .verifyComplete();
  }

  @Test
  @DisplayName("Redis 캐시에 값을 저장하고 조회한다")
  void testPutAndGetWithRedis() {
    String key = "test-redis-key";
    String value = "test-redis-value";

    // Mock Redis 동작
    when(redisCacheService.put(anyString(), anyString()))
        .thenReturn(Mono.empty());
    when(redisCacheService.get(anyString())).thenReturn(Mono.just(value));

    StepVerifier.create(
        cacheRouter.put(key, value, CacheType.REDIS)
            .then(cacheRouter.get(key, CacheType.REDIS)))
        .expectNext(value)
        .verifyComplete();
  }

  @Test
  @DisplayName("Caffeine 캐시의 값을 제거한다")
  void testEvictWithCaffeine() {
    String key = "test-evict-key";
    String value = "test-evict-value";

    StepVerifier.create(
        cacheRouter.put(key, value, CacheType.CAFFEINE)
            .then(cacheRouter.evict(key, CacheType.CAFFEINE))
            .then(cacheRouter.exists(key, CacheType.CAFFEINE)))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  @DisplayName("Caffeine 캐시에 값이 있는지 조회한다")
  void testExistsWithCaffeine() {
    String key = "test-exists-key";
    String value = "test-exists-value";

    StepVerifier.create(
        cacheRouter.put(key, value, CacheType.CAFFEINE)
            .then(cacheRouter.exists(key, CacheType.CAFFEINE)))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  @DisplayName("Caffeine 캐시 통계를 조회한다")
  void testGetStats() {
    StepVerifier.create(cacheRouter.getStats(CacheType.CAFFEINE))
        .expectNextMatches(stats -> stats != null)
        .verifyComplete();
  }

  @Test
  @DisplayName("Redis 캐시 통계를 조회한다")
  void testGetStatsWithRedis() {
    CacheStatsDto mockStats = new CacheStatsDto(100L, 10L, 0.9, 50L);
    when(redisCacheService.getStats()).thenReturn(Mono.just(mockStats));

    StepVerifier.create(cacheRouter.getStats(CacheType.REDIS))
        .expectNext(mockStats)
        .verifyComplete();
  }

  @Test
  @DisplayName("Redis 비활성화 시 Redis 캐시 요청은 실패한다")
  void redisDisabledRedisCacheRequestFails() {
    CacheService caffeineCacheService = mock(CacheService.class);
    ObjectProvider<CacheService> redisCacheServiceProvider = mock(ObjectProvider.class);
    CacheRouter router = new CacheRouter(caffeineCacheService, redisCacheServiceProvider);

    assertThatThrownBy(() -> router.get("fallback-key", CacheType.REDIS))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Redis cache is disabled");
  }

  @Test
  @DisplayName("Redis 비활성화 시 redisCacheService 없이 CacheRouter를 생성한다")
  void redisDisabledCreatesCacheRouterWithoutRedisCacheService() {
    CacheService caffeineCacheService = mock(CacheService.class);

    new ApplicationContextRunner()
        .withBean("caffeineCacheService", CacheService.class, () -> caffeineCacheService)
        .withUserConfiguration(CacheRouterConfiguration.class)
        .run(context -> assertThat(context)
            .hasSingleBean(CacheRouter.class)
            .doesNotHaveBean("redisCacheService"));
  }

  @Import(CacheRouter.class)
  private static class CacheRouterConfiguration {
  }

}
