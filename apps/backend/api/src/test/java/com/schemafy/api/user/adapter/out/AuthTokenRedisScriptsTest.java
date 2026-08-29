package com.schemafy.api.user.adapter.out;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthTokenRedisScripts")
class AuthTokenRedisScriptsTest {

  @Test
  @DisplayName("consume token Lua script를 classpath에서 로드한다")
  void consumeTokenScriptLoads() {
    String script = AuthTokenRedisScripts.CONSUME_TOKEN.getScriptAsString();

    assertThat(script).contains("redis.call('GET'");
    assertThat(script).contains("redis.call('DEL'");
    assertThat(script).contains("ATTEMPTS_EXCEEDED");
  }

}
