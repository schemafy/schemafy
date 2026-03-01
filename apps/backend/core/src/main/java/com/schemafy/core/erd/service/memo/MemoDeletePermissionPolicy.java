package com.schemafy.core.erd.service.memo;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.security.principal.AuthenticatedUser;
import com.schemafy.core.project.repository.vo.ProjectRole;

@Component
public class MemoDeletePermissionPolicy {

  public boolean canDeleteOthers(AuthenticatedUser user) {
    return user.roles().contains(ProjectRole.OWNER)
        || user.roles().contains(ProjectRole.ADMIN);
  }

}
