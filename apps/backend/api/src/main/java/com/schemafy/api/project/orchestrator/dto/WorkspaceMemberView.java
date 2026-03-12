package com.schemafy.api.project.orchestrator.dto;

import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.user.domain.User;

public record WorkspaceMemberView(
    WorkspaceMember member,
    User user) {
}
