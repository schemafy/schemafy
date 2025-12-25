package com.schemafy.core.common.security;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {

  String userId() default "01ARZ3NDEKTSV4RRFFQ69G5FAV";

  String[] roles() default { "EDITOR" };

}
