package com.schemafy.api.erd.service.sync;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

final class ErdStateSnapshotRedisScripts {

  private static final String SCRIPT_PATH = "redis/erd-state-snapshot/";

  static final RedisScript<Long> ENQUEUE_ACTIVE = longScript("enqueue-active.lua");
  static final RedisScript<Long> ENQUEUE_DELETED = longScript("enqueue-deleted.lua");
  static final RedisScript<String> CLAIM = stringScript("claim.lua");
  static final RedisScript<Long> IS_PUBLISHABLE = longScript("is-publishable.lua");
  static final RedisScript<Long> RENEW_LEASE = longScript("renew-lease.lua");
  static final RedisScript<Long> COMPLETE = longScript("complete.lua");
  static final RedisScript<Long> REQUEUE = longScript("requeue.lua");

  private ErdStateSnapshotRedisScripts() {}

  private static RedisScript<Long> longScript(String fileName) {
    return RedisScript.of(new ClassPathResource(SCRIPT_PATH + fileName), Long.class);
  }

  private static RedisScript<String> stringScript(String fileName) {
    return RedisScript.of(new ClassPathResource(SCRIPT_PATH + fileName), String.class);
  }

}
