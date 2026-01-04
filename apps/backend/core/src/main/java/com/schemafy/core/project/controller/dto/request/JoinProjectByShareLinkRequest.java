package com.schemafy.core.project.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * ShareLink 토큰을 사용한 프로젝트 참여 요청 DTO
 */
public record JoinProjectByShareLinkRequest(
        @NotBlank(message = "토큰은 필수입니다") String token) {
}
