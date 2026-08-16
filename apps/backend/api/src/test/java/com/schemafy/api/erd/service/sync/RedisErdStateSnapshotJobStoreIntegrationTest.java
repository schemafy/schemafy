package com.schemafy.api.erd.service.sync;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.json.JsonCodec;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisErdStateSnapshotJobStoreIntegrationTest {

  private static final String DUE_KEY = "erd:state-snapshot:{coord}:due";

  @Container
  private static final GenericContainer<?> REDIS = new GenericContainer<>(
      DockerImageName.parse("redis:8.4-alpine"))
      .withExposedPorts(6379);

  private static LettuceConnectionFactory connectionFactory;
  private static ReactiveStringRedisTemplate redisTemplate;

  private final AtomicLong now = new AtomicLong(1_000L);
  private final ErdStateSnapshotProperties properties = properties();

  private RedisErdStateSnapshotJobStore firstStore;
  private RedisErdStateSnapshotJobStore secondStore;

  @BeforeAll
  static void setUpRedis() {
    RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(
        6379));
    connectionFactory = new LettuceConnectionFactory(configuration);
    connectionFactory.afterPropertiesSet();
    redisTemplate = new ReactiveStringRedisTemplate(connectionFactory);
  }

  @AfterAll
  static void tearDownRedis() {
    connectionFactory.destroy();
  }

  @BeforeEach
  void setUp() throws Exception {
    REDIS.execInContainer("redis-cli", "FLUSHALL");
    JsonCodec jsonCodec = new JsonCodec(new ObjectMapper().findAndRegisterModules());
    firstStore = new RedisErdStateSnapshotJobStore(redisTemplate, jsonCodec,
        properties, now::get);
    secondStore = new RedisErdStateSnapshotJobStore(redisTemplate, jsonCodec,
        properties, now::get);
  }

  @Test
  void coalescesActiveRevisionsAcrossInstances() {
    firstStore.enqueueActive("project-1", "schema-1", 10L).block();
    secondStore.enqueueActive("project-1", "schema-1", 12L).block();

    List<String> dueJobKeys = firstStore.findDueJobKeys(1_100L, 20)
        .collectList().block();
    ErdStateSnapshotJob claimed = secondStore.claim(dueJobKeys.getFirst(),
        "lease-1", 1_100L, Duration.ofSeconds(30)).block();

    assertThat(dueJobKeys).hasSize(1);
    assertThat(claimed.targetRevision()).isEqualTo(12L);
    assertThat(claimed.kind()).isEqualTo(ErdStateSnapshotJobKind.ACTIVE);
  }

  @Test
  void capsContinuousDebounceAtMaxWait() {
    firstStore.enqueueActive("project-1", "schema-1", 1L).block();
    for (int offset = 90; offset <= 450; offset += 90) {
      now.set(1_000L + offset);
      firstStore.enqueueActive("project-1", "schema-1", offset / 90 + 1L)
          .block();
    }

    assertThat(firstStore.findDueJobKeys(1_499L, 20).collectList().block())
        .isEmpty();
    assertThat(firstStore.findDueJobKeys(1_500L, 20).collectList().block())
        .hasSize(1);
  }

  @Test
  void grantsOnlyOneLeaseForConcurrentClaims() {
    firstStore.enqueueActive("project-1", "schema-1", 10L).block();
    String jobKey = firstStore.findDueJobKeys(1_100L, 20).blockFirst();

    List<ErdStateSnapshotJob> claims = Flux.merge(
        firstStore.claim(jobKey, "lease-1", 1_100L, Duration.ofSeconds(30)),
        secondStore.claim(jobKey, "lease-2", 1_100L, Duration.ofSeconds(30)))
        .collectList().block();

    assertThat(claims).hasSize(1);
    assertThat(claims.getFirst().leaseToken()).isIn("lease-1", "lease-2");
  }

  @Test
  void reclaimsAJobAfterItsLeaseExpires() {
    firstStore.enqueueActive("project-1", "schema-1", 10L).block();
    String jobKey = firstStore.findDueJobKeys(1_100L, 20).blockFirst();
    firstStore.claim(jobKey, "lease-1", 1_100L, Duration.ofSeconds(30)).block();

    assertThat(secondStore.findDueJobKeys(31_099L, 20).collectList().block())
        .isEmpty();
    ErdStateSnapshotJob reclaimed = secondStore.findDueJobKeys(31_100L, 20)
        .next()
        .flatMap(key -> secondStore.claim(key, "lease-2", 31_100L,
            Duration.ofSeconds(30)))
        .block();

    assertThat(reclaimed.leaseToken()).isEqualTo("lease-2");
  }

  @Test
  void renewalExtendsOnlyTheCurrentLease() {
    firstStore.enqueueActive("project-1", "schema-1", 10L).block();
    String jobKey = firstStore.findDueJobKeys(1_100L, 20).blockFirst();
    ErdStateSnapshotJob job = firstStore.claim(jobKey, "lease-1", 1_100L,
        Duration.ofSeconds(30)).block();

    assertThat(firstStore.renewLease(job, 20_000L, Duration.ofSeconds(30)).block())
        .isTrue();
    ErdStateSnapshotJob staleToken = new ErdStateSnapshotJob(job.jobKey(),
        job.projectId(), job.schemaId(), job.kind(), job.targetRevision(),
        job.generation(), "stale-token", job.failureCount());
    assertThat(secondStore.renewLease(staleToken, 20_000L,
        Duration.ofSeconds(30)).block()).isFalse();
    assertThat(secondStore.findDueJobKeys(49_999L, 20).collectList().block())
        .isEmpty();
    assertThat(secondStore.findDueJobKeys(50_000L, 20).collectList().block())
        .hasSize(1);
  }

  @Test
  void newerActiveRevisionMakesAnOlderCandidateUnpublishable() {
    firstStore.enqueueActive("project-1", "schema-1", 10L).block();
    String jobKey = firstStore.findDueJobKeys(1_100L, 20).blockFirst();
    ErdStateSnapshotJob job = firstStore.claim(jobKey, "lease-1", 1_100L,
        Duration.ofSeconds(30)).block();

    now.set(1_200L);
    secondStore.enqueueActive("project-1", "schema-1", 12L).block();

    assertThat(firstStore.isPublishable(job, 10L).block()).isFalse();
    assertThat(firstStore.isPublishable(job, 12L).block()).isTrue();
  }

  @Test
  void deletionInvalidatesAnOlderActiveLease() {
    firstStore.enqueueActive("project-1", "schema-1", 10L).block();
    String jobKey = firstStore.findDueJobKeys(1_100L, 20).blockFirst();
    ErdStateSnapshotJob activeJob = firstStore.claim(jobKey, "lease-active",
        1_100L, Duration.ofSeconds(30)).block();

    now.set(1_200L);
    secondStore.enqueueDeleted("project-1", "schema-1", 11L).block();

    assertThat(firstStore.isPublishable(activeJob, 10L).block()).isFalse();
    ErdStateSnapshotJob deletedJob = secondStore.findDueJobKeys(1_200L, 20)
        .next()
        .flatMap(key -> secondStore.claim(key, "lease-deleted", 1_200L,
            Duration.ofSeconds(30)))
        .block();
    assertThat(deletedJob.kind()).isEqualTo(ErdStateSnapshotJobKind.DELETED);
    assertThat(deletedJob.generation()).isGreaterThan(activeJob.generation());
  }

  @Test
  void ignoresCompletionAndRequeueFromAStaleGeneration() {
    firstStore.enqueueActive("project-1", "schema-1", 10L).block();
    String jobKey = firstStore.findDueJobKeys(1_100L, 20).blockFirst();
    ErdStateSnapshotJob staleJob = firstStore.claim(jobKey, "lease-active",
        1_100L, Duration.ofSeconds(30)).block();
    now.set(1_200L);
    secondStore.enqueueDeleted("project-1", "schema-1", 11L).block();

    firstStore.complete(staleJob, 10L, 1_200L).block();
    firstStore.requeue(staleJob, 1_200L, Duration.ofMinutes(1), true).block();

    ErdStateSnapshotJob currentJob = secondStore.findDueJobKeys(1_200L, 20)
        .next()
        .flatMap(key -> secondStore.claim(key, "lease-deleted", 1_200L,
            Duration.ofSeconds(30)))
        .block();
    assertThat(currentJob.kind()).isEqualTo(ErdStateSnapshotJobKind.DELETED);
    assertThat(currentJob.targetRevision()).isEqualTo(11L);
  }

  @Test
  void ignoresAnAlreadyCompletedRevision() {
    firstStore.enqueueActive("project-1", "schema-1", 10L).block();
    String jobKey = firstStore.findDueJobKeys(1_100L, 20).blockFirst();
    ErdStateSnapshotJob job = firstStore.claim(jobKey, "lease-1", 1_100L,
        Duration.ofSeconds(30)).block();
    firstStore.complete(job, 10L, 1_200L).block();

    secondStore.enqueueActive("project-1", "schema-1", 10L).block();

    assertThat(firstStore.findDueJobKeys(Long.MAX_VALUE, 20).collectList().block())
        .isEmpty();
  }

  @Test
  void removesAStaleDueMemberWithoutAJobHash() {
    String staleJobKey = "erd:state-snapshot:{coord}:job:missing:missing";
    redisTemplate.opsForZSet().add(DUE_KEY, staleJobKey, 1_000D).block();

    assertThat(firstStore.findDueJobKeys(1_000L, 20).collectList().block())
        .isEmpty();
    assertThat(redisTemplate.opsForZSet().score(DUE_KEY, staleJobKey).block())
        .isNull();
  }

  @Test
  void processesActiveJobsEvenIfStaleKeysPrecedeThemLimited() {
    String staleJobKey = "erd:state-snapshot:{coord}:job:missing:missing";
    redisTemplate.opsForZSet().add(DUE_KEY, staleJobKey, 1_000D).block();

    firstStore.enqueueActive("project-1", "schema-1", 10L).block();
    String activeJobKey = "erd:state-snapshot:{coord}:job:project-1:schema-1";

    List<String> due = firstStore.findDueJobKeys(1_100L, 1).collectList().block();
    assertThat(due).containsExactly(activeJobKey);
    assertThat(redisTemplate.opsForZSet().score(DUE_KEY, staleJobKey).block()).isNull();
  }

  @Test
  void requeueCanConditionallyIncrementFailureCount() {
    firstStore.enqueueActive("project-1", "schema-1", 10L).block();
    String jobKey = firstStore.findDueJobKeys(1_100L, 20).blockFirst();
    ErdStateSnapshotJob job = firstStore.claim(jobKey, "lease-1", 1_100L,
        Duration.ofSeconds(30)).block();

    firstStore.requeue(job, 1_200L, Duration.ZERO, false).block();
    ErdStateSnapshotJob reclaimed1 = firstStore.claim(jobKey, "lease-2", 1_200L,
        Duration.ofSeconds(30)).block();
    assertThat(reclaimed1.failureCount()).isEqualTo(0);

    firstStore.requeue(reclaimed1, 1_300L, Duration.ZERO, true).block();
    ErdStateSnapshotJob reclaimed2 = firstStore.claim(jobKey, "lease-3", 1_300L,
        Duration.ofSeconds(30)).block();
    assertThat(reclaimed2.failureCount()).isEqualTo(1);
  }

  private static ErdStateSnapshotProperties properties() {
    ErdStateSnapshotProperties properties = new ErdStateSnapshotProperties();
    properties.setDebounce(Duration.ofMillis(100));
    properties.setMaxWait(Duration.ofMillis(500));
    properties.setCompletedWatermarkTtl(Duration.ofHours(24));
    return properties;
  }

}
