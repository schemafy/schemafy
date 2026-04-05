package com.schemafy.api.collaboration.constant;

public final class CollaborationConstants {

  private CollaborationConstants() {
    // utility class
  }

  public static final String CHANNEL_PREFIX = "collaboration:";
  public static final String CHANNEL_PATTERN = "collaboration:*";
  public static final String SESSION_ID_HEADER = "X-Session-Id";
  public static final String CLIENT_OPERATION_ID_HEADER = "X-Client-Op-Id";
  public static final String BASE_SCHEMA_REVISION_HEADER = "X-Base-Schema-Revision";
  public static final String SESSION_ID_CONTEXT_KEY = "collaboration.sessionId";

}
