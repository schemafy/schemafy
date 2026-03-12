package com.schemafy.core.project.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import com.schemafy.domain.project.domain.ProjectRole;

public record UpdateProjectMemberRoleRequest(
    @NotNull(message = "역할은 필수입니다") ProjectRole role) {
}
