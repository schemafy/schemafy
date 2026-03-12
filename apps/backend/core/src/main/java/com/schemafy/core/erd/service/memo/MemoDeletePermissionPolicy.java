package com.schemafy.core.erd.service.memo;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.security.principal.AuthenticatedUser;
import com.schemafy.domain.project.domain.ProjectRole;

@Component
public class MemoDeletePermissionPolicy {

  public boolean canDeleteOthers(AuthenticatedUser user) {
    return user.roles().contains(ProjectRole.ADMIN);
  }

}
