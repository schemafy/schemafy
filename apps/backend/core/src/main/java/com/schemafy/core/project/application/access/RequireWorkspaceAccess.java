package com.schemafy.core.project.application.access;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.schemafy.core.project.domain.WorkspaceRole;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireWorkspaceAccess {

  WorkspaceRole role() default WorkspaceRole.MEMBER;

  String workspaceId() default "workspaceId";

  String requesterId() default "requesterId";

}
