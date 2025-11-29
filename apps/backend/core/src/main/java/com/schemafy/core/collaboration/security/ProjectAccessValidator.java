package com.schemafy.core.collaboration.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.schemafy.core.erd.repository.SchemaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ProjectAccessValidator {

    private final SchemaRepository schemaRepository;

    public Mono<Boolean> canAccess(String projectId, String userId) {
        // TODO: 프로젝트 멤버십 테이블 구현 시 아래 로직으로 변경
        // 현재: 해당 projectId에 스키마가 하나라도 존재하면 접근 허용
        return schemaRepository.findByProjectIdAndDeletedAtIsNull(projectId)
                .hasElements();
    }

}
