package com.schemafy.core.project.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.schemafy.core.project.repository.vo.WorkspaceRole;

public record CreateWorkspaceInvitationRequest(
    @NotBlank(message = "이메일은 필수입니다.") @Email(message = "유효한 이메일 주소를 입력해주세요.") String email,
    @NotNull(message = "역할은 필수입니다.") WorkspaceRole role) {
}
