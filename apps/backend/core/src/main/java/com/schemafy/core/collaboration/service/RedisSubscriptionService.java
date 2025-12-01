package com.schemafy.core.collaboration.service;

import java.time.Duration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.schemafy.core.collaboration.constant.CollaborationConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisSubscriptionService {

    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final PresenceService presenceService;

    private Disposable subscription;

    @PostConstruct
    public void init() {
        subscribeToChannels();
    }

    @PreDestroy
    public void destroy() {
        shutdown();
    }

    private void subscribeToChannels() {
        subscription = redisTemplate
                .listenToPattern(CollaborationConstants.CHANNEL_PATTERN)
                .flatMap(message -> {
                    String channel = message.getChannel();
                    String payload = message.getMessage();
                    String projectId = extractProjectId(channel);

                    if (projectId == null || projectId.isBlank()) {
                        log.warn(
                                "[RedisSubscriptionService] Invalid channel format: {}",
                                channel);
                        return Mono.empty();
                    }

                    return presenceService
                            .handleRedisMessage(projectId, payload)
                            .doOnError(e -> log.error(
                                    "[RedisSubscriptionService] Failed to handle message for project {}: {}",
                                    projectId, e.getMessage()))
                            .onErrorResume(e -> Mono.empty());
                })
                .doOnError(error -> log.error(
                        "[RedisSubscriptionService] Redis subscription error",
                        error))
                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, INITIAL_BACKOFF)
                        .maxBackoff(MAX_BACKOFF)
                        .doBeforeRetry(signal -> log.info(
                                "[RedisSubscriptionService] Retrying subscription (attempt {}/{})",
                                signal.totalRetries() + 1, MAX_RETRY_ATTEMPTS))
                        .onRetryExhaustedThrow((spec, signal) -> {
                            log.error(
                                    "[RedisSubscriptionService] Max retry attempts ({}) reached. Giving up.",
                                    MAX_RETRY_ATTEMPTS);
                            return signal.failure();
                        }))
                .subscribe();
    }

    private String extractProjectId(String channel) {
        if (channel != null
                && channel.startsWith(CollaborationConstants.CHANNEL_PREFIX)) {
            return channel
                    .substring(CollaborationConstants.CHANNEL_PREFIX.length());
        }
        return channel;
    }

    public void shutdown() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

}
