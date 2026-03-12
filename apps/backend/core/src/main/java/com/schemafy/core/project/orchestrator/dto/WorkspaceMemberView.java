package com.schemafy.core.project.orchestrator.dto;

import com.schemafy.domain.project.domain.WorkspaceMember;
import com.schemafy.domain.user.domain.User;

public record WorkspaceMemberView(
    WorkspaceMember member,
    User user) {
}
