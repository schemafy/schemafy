package com.schemafy.api.user.oauth;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@Configuration
public class GitHubWebClientConfig {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);

  private HttpClient createHttpClient() {
    return HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
            (int) CONNECT_TIMEOUT.toMillis())
        .responseTimeout(RESPONSE_TIMEOUT);
  }

  @Bean
  WebClient gitHubWebClient() {
    return WebClient.builder()
        .clientConnector(
            new ReactorClientHttpConnector(createHttpClient()))
        .baseUrl("https://github.com")
        .defaultHeader(HttpHeaders.ACCEPT,
            MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Bean
  WebClient gitHubApiWebClient() {
    return WebClient.builder()
        .clientConnector(
            new ReactorClientHttpConnector(createHttpClient()))
        .baseUrl("https://api.github.com")
        .defaultHeader(HttpHeaders.ACCEPT,
            MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

}
