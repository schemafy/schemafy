package com.schemafy.core.user.oauth;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.schemafy.core.common.exception.OAuthErrorCode;
import com.schemafy.domain.common.exception.DomainException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubOAuthService {

  private final GitHubOAuthProperties properties;
  private final WebClient gitHubWebClient;
  private final WebClient gitHubApiWebClient;

  public String getAuthorizeUrl(String state) {
    return UriComponentsBuilder
        .fromUriString("https://github.com/login/oauth/authorize")
        .queryParam("client_id", properties.getClientId())
        .queryParam("redirect_uri", properties.getRedirectUri())
        .queryParam("scope", "user:email")
        .queryParam("state", state)
        .build()
        .toUriString();
  }

  public Mono<String> exchangeCodeForToken(String code) {
    return gitHubWebClient.post()
        .uri("/login/oauth/access_token")
        .bodyValue(Map.of(
            "client_id", properties.getClientId(),
            "client_secret", properties.getClientSecret(),
            "code", code,
            "redirect_uri", properties.getRedirectUri()))
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {
        })
        .flatMap(response -> {
          String accessToken = response.get("access_token");
          if (accessToken == null) {
            log.error("GitHub token exchange failed: {}",
                response);
            return Mono.error(new DomainException(
                OAuthErrorCode.CODE_EXCHANGE_FAILED));
          }
          return Mono.just(accessToken);
        })
        .onErrorMap(e -> !(e instanceof DomainException),
            e -> {
              log.error("GitHub token exchange error", e);
              return new DomainException(
                  OAuthErrorCode.CODE_EXCHANGE_FAILED);
            });
  }

  public Mono<GitHubUserInfo> fetchGitHubUser(String accessToken) {
    return gitHubApiWebClient.get()
        .uri("/user")
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .bodyToMono(GitHubUserInfo.class)
        .onErrorMap(e -> !(e instanceof DomainException),
            e -> {
              log.error("GitHub user info fetch error", e);
              return new DomainException(
                  OAuthErrorCode.USER_INFO_FAILED);
            });
  }

  public Mono<String> fetchGitHubUserEmail(String accessToken) {
    return gitHubApiWebClient.get()
        .uri("/user/emails")
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
        })
        .flatMap(emails -> emails.stream()
            .filter(e -> Boolean.TRUE.equals(e.get("primary"))
                && Boolean.TRUE.equals(e.get("verified")))
            .map(e -> (String) e.get("email"))
            .findFirst()
            .map(Mono::just)
            .orElse(Mono.error(new DomainException(
                OAuthErrorCode.EMAIL_NOT_AVAILABLE))))
        .onErrorMap(e -> !(e instanceof DomainException),
            e -> {
              log.error("GitHub email fetch error", e);
              return new DomainException(
                  OAuthErrorCode.USER_INFO_FAILED);
            });
  }

}
