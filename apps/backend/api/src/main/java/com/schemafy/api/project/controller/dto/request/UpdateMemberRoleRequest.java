package com.schemafy.api.project.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import com.schemafy.core.project.domain.WorkspaceRole;

public record UpdateMemberRoleRequest(
    @NotNull(message = "역할은 필수입니다") WorkspaceRole role) {
}
