package com.schemafy.core.project.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import com.schemafy.core.project.repository.vo.ProjectRole;

/**
 * 프로젝트 멤버 권한 변경 요청 DTO
 */
public record UpdateProjectMemberRoleRequest(
        @NotNull(message = "역할은 필수입니다") ProjectRole role) {
}
