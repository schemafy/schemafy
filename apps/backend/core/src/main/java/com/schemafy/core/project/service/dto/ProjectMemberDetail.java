package com.schemafy.core.project.service.dto;

import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.user.repository.entity.User;

public record ProjectMemberDetail(ProjectMember member, User user) {

}
