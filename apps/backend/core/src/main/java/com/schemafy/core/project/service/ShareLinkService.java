package com.schemafy.core.project.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.project.controller.dto.response.ShareLinkAccessResponse;
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.ShareLinkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareLinkService {

  private final ShareLinkRepository shareLinkRepository;
  private final ProjectRepository projectRepository;

  public Mono<ShareLinkAccessResponse> accessByCode(String code,
      String userId, String ipAddress, String userAgent) {
    String user = userId != null ? userId : "anonymous";

    return shareLinkRepository.findValidLinkByCode(code, Instant.now())
        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SHARE_LINK_INVALID)))
        .doOnNext(shareLink -> log.info(
            "ShareLink access success - code: {}, projectId: {}, userId: {}, ip: {}, userAgent: {}",
            code, shareLink.getProjectId(), user, ipAddress, userAgent))
        .flatMap(shareLink -> shareLinkRepository.incrementAccessCount(shareLink.getId())
            .onErrorResume(e -> {
              log.error("Failed to increment access count for ShareLink id: {}", shareLink.getId(), e);
              return Mono.empty();
            })
            .then(projectRepository.findByIdAndNotDeleted(shareLink.getProjectId()))
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PROJECT_NOT_FOUND)))
            .map(ShareLinkAccessResponse::of))
        .doOnError(ex -> log.info("ShareLink access failed - code: {}, userId: {}, ip: {}, userAgent: {}, reason: {}",
            code, user, ipAddress, userAgent, ex.getMessage()));
  }

}
