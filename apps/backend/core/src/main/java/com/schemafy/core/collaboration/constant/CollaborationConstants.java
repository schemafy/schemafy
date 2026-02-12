package com.schemafy.core.collaboration.constant;

public final class CollaborationConstants {

  private CollaborationConstants() {
    // utility class
  }

  public static final String CHANNEL_PREFIX = "collaboration:";
  public static final String CHANNEL_PATTERN = "collaboration:*";
  public static final String SESSION_ID_HEADER = "X-Session-Id";
  public static final String SESSION_ID_CONTEXT_KEY = "collaboration.sessionId";

}
