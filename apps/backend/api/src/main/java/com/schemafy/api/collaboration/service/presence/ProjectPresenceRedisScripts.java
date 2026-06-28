package com.schemafy.api.collaboration.service.presence;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

final class ProjectPresenceRedisScripts {

  static final RedisScript<Long> WRITE_SESSION = RedisScript.of(
      new ClassPathResource("redis/presence/write-session.lua"),
      Long.class);

  static final RedisScript<String> REFRESH_SESSION = RedisScript.of(
      new ClassPathResource("redis/presence/refresh-session.lua"),
      String.class);

  static final RedisScript<String> REMOVE_EXPIRED_SESSION = RedisScript.of(
      new ClassPathResource("redis/presence/remove-expired-session.lua"),
      String.class);

  static final RedisScript<Long> CLEANUP_EMPTY_PROJECT = RedisScript.of(
      new ClassPathResource("redis/presence/cleanup-empty-project.lua"),
      Long.class);

  private ProjectPresenceRedisScripts() {}

}
