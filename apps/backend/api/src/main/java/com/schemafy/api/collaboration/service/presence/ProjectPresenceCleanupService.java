package com.schemafy.api.collaboration.service.presence;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Service;

import com.schemafy.api.collaboration.service.CollaborationService;
import com.schemafy.api.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class ProjectPresenceCleanupService {

  private final CollaborationService collaborationService;
  private final CollaborationPresenceProperties properties;

  private Disposable cleanupTask;

  @PostConstruct
  public void start() {
    cleanupTask = Flux.interval(properties.getCleanupInterval())
        .concatMap(tick -> collaborationService.removeExpiredPresenceSessions()
            .onErrorResume(error -> {
              log.warn(
                  "[ProjectPresenceCleanupService] Presence cleanup failed: {}",
                  error.getMessage());
              return Mono.empty();
            }))
        .subscribe();
  }

  @PreDestroy
  public void stop() {
    if (cleanupTask != null && !cleanupTask.isDisposed()) {
      cleanupTask.dispose();
    }
  }

}
