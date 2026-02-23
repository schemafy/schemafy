package com.schemafy.core.user.controller;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.security.jwt.JwtTokenIssuer;
import com.schemafy.core.user.oauth.GitHubOAuthProperties;
import com.schemafy.core.user.oauth.GitHubOAuthService;
import com.schemafy.core.user.repository.vo.AuthProvider;
import com.schemafy.core.user.service.UserService;
import com.schemafy.core.user.service.dto.OAuthLoginCommand;

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

  @GetMapping("/oauth/github/authorize")
  public Mono<ResponseEntity<Void>> authorize() {
    String state = UUID.randomUUID().toString();
    String authorizeUrl = gitHubOAuthService.getAuthorizeUrl(state);

    ResponseCookie stateCookie = ResponseCookie
        .from(OAUTH_STATE_COOKIE, state)
        .httpOnly(true)
        .secure(false)
        .path("/")
        .maxAge(STATE_COOKIE_MAX_AGE)
        .sameSite("Lax")
        .build();

    return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
        .header("Set-Cookie", stateCookie.toString())
        .location(URI.create(authorizeUrl))
        .build());
  }

  @GetMapping("/oauth/github/callback")
  public Mono<ResponseEntity<Void>> callback(
      @RequestParam String code,
      @RequestParam String state,
      ServerHttpRequest request) {
    String cookieState = extractStateCookie(request);
    if (!state.equals(cookieState)) {
      return Mono.error(
          new BusinessException(ErrorCode.OAUTH_STATE_MISMATCH));
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
          ResponseCookie expireStateCookie = ResponseCookie
              .from(OAUTH_STATE_COOKIE, "")
              .httpOnly(true)
              .path("/")
              .maxAge(0)
              .build();

          return ResponseEntity.status(HttpStatus.FOUND)
              .headers(jwtTokenIssuer.issueTokens(
                  user.getId(), user.getName()))
              .header("Set-Cookie",
                  expireStateCookie.toString())
              .location(URI.create(
                  gitHubOAuthProperties
                      .getFrontendCallbackUrl()))
              .build();
        });
  }

  private String extractStateCookie(ServerHttpRequest request) {
    var cookie = request.getCookies().getFirst(OAUTH_STATE_COOKIE);
    return cookie != null ? cookie.getValue() : null;
  }

}
