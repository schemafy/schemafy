package com.schemafy.core.project.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import com.schemafy.core.project.repository.vo.ProjectRole;

public record UpdateProjectMemberRoleRequest(
    @NotNull(message = "역할은 필수입니다") ProjectRole role) {
}
