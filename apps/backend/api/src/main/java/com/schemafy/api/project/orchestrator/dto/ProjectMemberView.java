package com.schemafy.api.project.orchestrator.dto;

import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.user.domain.User;

public record ProjectMemberView(
    ProjectMember member,
    User user) {
}
