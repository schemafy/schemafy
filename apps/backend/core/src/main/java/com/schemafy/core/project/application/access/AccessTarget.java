package com.schemafy.core.project.application.access;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessTarget {

  ProjectAccessResourceType value();

  String id();

}
