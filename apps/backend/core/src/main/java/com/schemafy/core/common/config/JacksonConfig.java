package com.schemafy.core.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.openapitools.jackson.nullable.JsonNullableModule;

@Configuration
class JacksonConfig {

  @Bean
  JsonNullableModule jsonNullableModule() {
    return new JsonNullableModule();
  }

}
