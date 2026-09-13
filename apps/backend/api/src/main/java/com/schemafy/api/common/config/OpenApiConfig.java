package com.schemafy.api.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.WebFilter;

import com.schemafy.api.common.constant.ApiPath;

@Configuration
public class OpenApiConfig {

  @Bean
  @Profile("server")
  public WebFilter disabledOpenApiEndpointsFilter() {
    return (exchange, chain) -> {
      String path = exchange.getRequest().getPath()
          .pathWithinApplication().value();
      if (!ApiPath.isOpenApiDocsPath(path)) {
        return chain.filter(exchange);
      }
      exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
      return exchange.getResponse().setComplete();
    };
  }

}
