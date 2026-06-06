package com.schemafy.core.project.application.access;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

public final class ProjectAccessRequesterContext {

  private static final String REQUESTER_ID_KEY = ProjectAccessRequesterContext.class.getName() + ".requesterId";

  private ProjectAccessRequesterContext() {}

  public static Context withRequesterId(String requesterId) {
    return Context.of(REQUESTER_ID_KEY, requesterId);
  }

  public static String requesterIdOrNull(ContextView contextView) {
    return contextView.getOrDefault(REQUESTER_ID_KEY, null);
  }

  static String requesterId(ContextView contextView) {
    if (!contextView.hasKey(REQUESTER_ID_KEY)) {
      throw new IllegalStateException("Project access requester is missing");
    }
    return contextView.get(REQUESTER_ID_KEY);
  }

  static boolean hasRequesterId(ContextView contextView) {
    return contextView.hasKey(REQUESTER_ID_KEY);
  }

}
