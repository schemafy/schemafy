package com.schemafy.api.user.adapter.out;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

final class AuthTokenRedisScripts {

  static final RedisScript<String> CONSUME_TOKEN = RedisScript.of(
      new ClassPathResource("redis/auth/consume-token.lua"),
      String.class);

  private AuthTokenRedisScripts() {}

}
