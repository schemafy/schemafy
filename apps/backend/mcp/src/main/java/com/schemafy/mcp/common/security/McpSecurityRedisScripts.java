package com.schemafy.mcp.common.security;

import org.springframework.data.redis.core.script.RedisScript;

final class McpSecurityRedisScripts {

  static final RedisScript<Long> RATE_LIMIT_INCREMENT = RedisScript.of("""
      local current = redis.call('INCR', KEYS[1])
      if current == 1 then
        redis.call('PEXPIRE', KEYS[1], ARGV[1])
      end
      return current
      """, Long.class);

  private McpSecurityRedisScripts() {}

}
