package com.schemafy.core.collaboration.security;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@ConditionalOnRedisEnabled
public class ProjectAccessValidator {

  public Mono<Boolean> canAccess(String projectId, String userId) {
    // TODO: 프로젝트 멤버십 테이블 구현 시 아래 로직으로 변경
    // 현재: 해당 projectId에 스키마가 하나라도 존재하면 접근 허용
    // return schemaRepository.findByProjectIdAndDeletedAtIsNull(projectId)
    // .hasElements();
    return Mono.just(true);
  }

}
