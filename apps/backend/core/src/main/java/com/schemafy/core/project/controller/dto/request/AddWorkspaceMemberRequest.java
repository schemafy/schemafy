package com.schemafy.core.project.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.schemafy.core.project.repository.vo.WorkspaceRole;

/** 워크스페이스 멤버 추가 요청 DTO */
public record AddWorkspaceMemberRequest(
    @NotBlank(message = "사용자 ID는 필수입니다") String userId,

    @NotNull(message = "역할은 필수입니다") WorkspaceRole role) {
}
