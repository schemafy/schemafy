package com.schemafy.core.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.config.TestSecurityConfig;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.common.security.jwt.JwtTokenIssuer;
import com.schemafy.core.common.security.jwt.WebExchangeErrorWriter;
import com.schemafy.core.user.oauth.GitHubOAuthProperties;
import com.schemafy.core.user.oauth.GitHubOAuthService;
import com.schemafy.core.user.oauth.GitHubUserInfo;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.service.UserService;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@WebFluxTest(controllers = OAuthController.class)
@Import(TestSecurityConfig.class)
@DisplayName("OAuthController 테스트")
class OAuthControllerTest {

  private static final String API_BASE_PATH = ApiPath.PUBLIC_API.replace(
      "{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private GitHubOAuthService gitHubOAuthService;

  @MockitoBean
  private GitHubOAuthProperties gitHubOAuthProperties;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private JwtTokenIssuer jwtTokenIssuer;

  @MockitoBean
  private JwtProvider jwtProvider;

  @MockitoBean
  private WebExchangeErrorWriter webExchangeErrorWriter;

  @Nested
  @DisplayName("GitHub 인가 요청")
  class Authorize {

    @Test
    @DisplayName("GitHub 인가 URL로 리다이렉트한다")
    void authorize_redirectsToGitHub() {
      given(gitHubOAuthService.getAuthorizeUrl(anyString()))
          .willReturn(
              "https://github.com/login/oauth/authorize?client_id=test");

      webTestClient.get()
          .uri(API_BASE_PATH + "/oauth/github/authorize")
          .exchange()
          .expectStatus().isFound()
          .expectHeader().valueMatches("Location",
              "https://github.com/login/oauth/authorize.*")
          .expectHeader().exists("Set-Cookie");
    }

  }

  @Nested
  @DisplayName("GitHub 콜백")
  class Callback {

    @Test
    @DisplayName("state가 일치하지 않으면 에러를 반환한다")
    void callback_stateMismatch() {
      webTestClient.get()
          .uri(API_BASE_PATH
              + "/oauth/github/callback?code=test-code&state=wrong-state")
          .cookie("oauth_state", "correct-state")
          .exchange()
          .expectStatus().isBadRequest()
          .expectBody()
          .jsonPath("$.success").isEqualTo(false)
          .jsonPath("$.error.code")
          .isEqualTo(ErrorCode.OAUTH_STATE_MISMATCH.getCode());
    }

    @Test
    @DisplayName("state 쿠키가 없으면 에러를 반환한다")
    void callback_noStateCookie() {
      webTestClient.get()
          .uri(API_BASE_PATH
              + "/oauth/github/callback?code=test-code&state=some-state")
          .exchange()
          .expectStatus().isBadRequest()
          .expectBody()
          .jsonPath("$.success").isEqualTo(false)
          .jsonPath("$.error.code")
          .isEqualTo(ErrorCode.OAUTH_STATE_MISMATCH.getCode());
    }

    @Test
    @DisplayName("정상적인 콜백은 JWT 쿠키와 함께 프론트엔드로 리다이렉트한다")
    void callback_success() {
      String state = "valid-state";
      GitHubUserInfo userInfo = new GitHubUserInfo(
          12345L, "testuser", "test@example.com", "Test User");

      given(gitHubOAuthService.exchangeCodeForToken("test-code"))
          .willReturn(Mono.just("gho_test_token"));
      given(gitHubOAuthService.fetchGitHubUser("gho_test_token"))
          .willReturn(Mono.just(userInfo));
      given(userService.loginOrSignUpOAuth(any()))
          .willReturn(Mono.just(
              createTestUser("user-id", "test@example.com",
                  "Test User")));

      HttpHeaders jwtHeaders = new HttpHeaders();
      jwtHeaders.set("Authorization", "Bearer test-access-token");
      jwtHeaders.add("Set-Cookie",
          "accessToken=test-access-token; Path=/; HttpOnly");
      jwtHeaders.add("Set-Cookie",
          "refreshToken=test-refresh-token; Path=/; HttpOnly");
      given(jwtTokenIssuer.issueTokens("user-id", "Test User"))
          .willReturn(jwtHeaders);

      given(gitHubOAuthProperties.getFrontendCallbackUrl())
          .willReturn("http://localhost:3000/oauth/callback");

      webTestClient.get()
          .uri(API_BASE_PATH
              + "/oauth/github/callback?code=test-code&state="
              + state)
          .cookie("oauth_state", state)
          .exchange()
          .expectStatus().isFound()
          .expectHeader().valueMatches("Location",
              "http://localhost:3000/oauth/callback")
          .expectHeader().exists("Set-Cookie");
    }

    @Test
    @DisplayName("이메일이 null이면 GitHub 이메일 API로 폴백한다")
    void callback_emailFallback() {
      String state = "valid-state";
      GitHubUserInfo userInfoNoEmail = new GitHubUserInfo(
          12345L, "testuser", null, "Test User");

      given(gitHubOAuthService.exchangeCodeForToken("test-code"))
          .willReturn(Mono.just("gho_test_token"));
      given(gitHubOAuthService.fetchGitHubUser("gho_test_token"))
          .willReturn(Mono.just(userInfoNoEmail));
      given(gitHubOAuthService
          .fetchGitHubUserEmail("gho_test_token"))
          .willReturn(Mono.just("fetched@example.com"));
      given(userService.loginOrSignUpOAuth(any()))
          .willReturn(Mono.just(
              createTestUser("user-id", "fetched@example.com",
                  "Test User")));

      HttpHeaders jwtHeaders = new HttpHeaders();
      jwtHeaders.set("Authorization", "Bearer test-access-token");
      jwtHeaders.add("Set-Cookie",
          "accessToken=test-access-token; Path=/; HttpOnly");
      given(jwtTokenIssuer.issueTokens("user-id", "Test User"))
          .willReturn(jwtHeaders);

      given(gitHubOAuthProperties.getFrontendCallbackUrl())
          .willReturn("http://localhost:3000/oauth/callback");

      webTestClient.get()
          .uri(API_BASE_PATH
              + "/oauth/github/callback?code=test-code&state="
              + state)
          .cookie("oauth_state", state)
          .exchange()
          .expectStatus().isFound();
    }

  }

  private User createTestUser(String id, String email, String name) {
    User user = User.signUpOAuth(email, name);
    try {
      var idField = com.schemafy.core.common.type.BaseEntity.class
          .getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(user, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return user;
  }

}
