package com.schemafy.core.project.orchestrator.dto;

import com.schemafy.domain.project.domain.ProjectMember;
import com.schemafy.domain.user.domain.User;

public record ProjectMemberView(
    ProjectMember member,
    User user) {
}
