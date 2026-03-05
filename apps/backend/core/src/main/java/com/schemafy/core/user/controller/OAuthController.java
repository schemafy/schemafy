package com.schemafy.core.user.controller;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.jwt.JwtProperties;
import com.schemafy.core.common.security.jwt.JwtTokenIssuer;
import com.schemafy.core.user.oauth.GitHubOAuthProperties;
import com.schemafy.core.user.oauth.GitHubOAuthService;
import com.schemafy.core.user.service.UserService;
import com.schemafy.core.user.service.dto.OAuthLoginCommand;
import com.schemafy.domain.user.domain.AuthProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(ApiPath.PUBLIC_API)
@RequiredArgsConstructor
public class OAuthController {

  private static final String OAUTH_STATE_COOKIE = "oauth_state";
  private static final Duration STATE_COOKIE_MAX_AGE = Duration
      .ofMinutes(10);

  private final GitHubOAuthService gitHubOAuthService;
  private final GitHubOAuthProperties gitHubOAuthProperties;
  private final UserService userService;
  private final JwtTokenIssuer jwtTokenIssuer;
  private final JwtProperties jwtProperties;

  @GetMapping("/oauth/github/authorize")
  public Mono<ResponseEntity<Void>> authorize() {
    String state = UUID.randomUUID().toString();
    String authorizeUrl = gitHubOAuthService.getAuthorizeUrl(state);

    ResponseCookie stateCookie = createStateCookie(state,
        STATE_COOKIE_MAX_AGE);

    return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.SET_COOKIE, stateCookie.toString())
        .location(URI.create(authorizeUrl))
        .build());
  }

  @GetMapping("/oauth/github/callback")
  public Mono<ResponseEntity<Void>> callback(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(name = "error", required = false) String error,
      @RequestParam(name = "error_description", required = false) String errorDescription,
      ServerHttpRequest request) {
    String cookieState = extractStateCookie(request);
    if (!StringUtils.hasText(state) || !state.equals(cookieState)) {
      return Mono.just(redirectToFrontendWithError("state_mismatch", null));
    }

    if (StringUtils.hasText(error)) {
      return Mono.just(
          redirectToFrontendWithError(error, errorDescription));
    }

    if (!StringUtils.hasText(code)) {
      return Mono.just(
          redirectToFrontendWithError("invalid_request", "code is missing"));
    }

    return gitHubOAuthService.exchangeCodeForToken(code)
        .flatMap(accessToken -> gitHubOAuthService
            .fetchGitHubUser(accessToken)
            .flatMap(userInfo -> {
              Mono<String> emailMono = userInfo.email() != null
                  ? Mono.just(userInfo.email())
                  : gitHubOAuthService
                      .fetchGitHubUserEmail(
                          accessToken);
              return emailMono.map(
                  email -> new OAuthLoginCommand(
                      email,
                      userInfo.displayName(),
                      AuthProvider.GITHUB,
                      String.valueOf(userInfo.id())));
            }))
        .flatMap(userService::loginOrSignUpOAuth)
        .map(user -> {
          return ResponseEntity
              .status(HttpStatus.FOUND)
              .headers(jwtTokenIssuer.issueTokens(
                  user.id(), user.name()))
              .header(HttpHeaders.SET_COOKIE,
                  expireStateCookie().toString())
              .location(buildFrontendCallbackUri(null, null))
              .<Void>build();
        })
        .onErrorResume(e -> {
          return Mono.just(
              redirectToFrontendWithError("server_error", null));
        });
  }

  private String extractStateCookie(ServerHttpRequest request) {
    var cookie = request.getCookies().getFirst(OAUTH_STATE_COOKIE);
    return cookie != null ? cookie.getValue() : null;
  }

  private ResponseCookie createStateCookie(String value, Duration maxAge) {
    return ResponseCookie.from(OAUTH_STATE_COOKIE, value)
        .httpOnly(true)
        .secure(jwtProperties.getCookie().isSecure())
        .path("/")
        .maxAge(maxAge)
        .sameSite("Lax")
        .build();
  }

  private ResponseCookie expireStateCookie() {
    return createStateCookie("", Duration.ZERO);
  }

  private URI buildFrontendCallbackUri(String error,
      String errorDescription) {
    UriComponentsBuilder builder = UriComponentsBuilder
        .fromUriString(gitHubOAuthProperties.getFrontendCallbackUrl());

    if (StringUtils.hasText(error)) {
      builder.queryParam("provider", "github")
          .queryParam("error", error);
      if (StringUtils.hasText(errorDescription)) {
        builder.queryParam("error_description",
            errorDescription);
      }
    }

    return builder.build()
        .encode()
        .toUri();
  }

  private ResponseEntity<Void> redirectToFrontendWithError(String error,
      String errorDescription) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.SET_COOKIE, expireStateCookie().toString())
        .location(buildFrontendCallbackUri(error, errorDescription))
        .build();
  }

}
