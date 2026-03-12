package com.schemafy.api.erd.service.memo;

import org.springframework.stereotype.Component;

import com.schemafy.api.common.security.principal.AuthenticatedUser;
import com.schemafy.core.project.domain.ProjectRole;

@Component
public class MemoDeletePermissionPolicy {

  public boolean canDeleteOthers(AuthenticatedUser user) {
    return user.roles().contains(ProjectRole.ADMIN);
  }

}
