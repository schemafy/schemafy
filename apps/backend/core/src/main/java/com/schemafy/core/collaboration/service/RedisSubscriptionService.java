package com.schemafy.core.collaboration.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import reactor.core.Disposable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisSubscriptionService {

    private static final String CHANNEL_PATTERN = "collaboration:*";

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
        subscription = redisTemplate.listenToPattern(CHANNEL_PATTERN)
                .doOnNext(message -> {
                    String channel = message.getChannel();
                    String payload = message.getMessage();
                    String projectId = extractProjectId(channel);

                    presenceService.handleRedisMessage(projectId, payload)
                            .subscribe();
                })
                .doOnError(error -> {
                    log.error("[RedisSubscriptionService] Redis subscription error", error);
                    resubscribe();
                })
                .subscribe();
    }

    private String extractProjectId(String channel) {
        // collaboration:{projectId}
        return channel.replace("collaboration:", "");
    }

    private void resubscribe() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
        subscribeToChannels();
    }

    public void shutdown() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

}
