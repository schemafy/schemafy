package com.schemafy.core.collaboration;

public final class CollaborationChannel {

  public static final String PATTERN = "collaboration:*";

  private static final String PREFIX = "collaboration:";

  private CollaborationChannel() {}

  public static String forProject(String projectId) {
    return PREFIX + projectId;
  }

  public static String extractProjectId(String channel) {
    if (channel != null && channel.startsWith(PREFIX)) {
      return channel.substring(PREFIX.length());
    }
    return channel;
  }

}
