package com.schemafy.core.project.service.dto;

import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.user.repository.entity.User;

public record WorkspaceMemberDetail(WorkspaceMember member, User user) {

}
