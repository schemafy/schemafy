package com.schemafy.core.project.application.access;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.schemafy.core.project.domain.ProjectRole;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireProjectAccess {

  ProjectRole role() default ProjectRole.VIEWER;

  String projectId() default "projectId";

  String requesterId() default "requesterId";

}
