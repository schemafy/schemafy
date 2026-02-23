package com.schemafy.core.user.oauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GitHubWebClientConfig {

  @Bean
  WebClient gitHubWebClient() {
    return WebClient.builder()
        .baseUrl("https://github.com")
        .defaultHeader(HttpHeaders.ACCEPT,
            MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Bean
  WebClient gitHubApiWebClient() {
    return WebClient.builder()
        .baseUrl("https://api.github.com")
        .defaultHeader(HttpHeaders.ACCEPT,
            MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

}
