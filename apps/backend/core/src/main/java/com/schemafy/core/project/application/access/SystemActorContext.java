package com.schemafy.core.project.application.access;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

public final class SystemActorContext {

  private static final String SYSTEM_ACTOR_KEY = SystemActorContext.class.getName()
      + ".systemActor";

  private SystemActorContext() {}

  public static Context asSystemActor() {
    return Context.of(SYSTEM_ACTOR_KEY, Boolean.TRUE);
  }

  public static boolean isSystemActor(ContextView contextView) {
    return Boolean.TRUE.equals(contextView.getOrDefault(SYSTEM_ACTOR_KEY, Boolean.FALSE));
  }

}
