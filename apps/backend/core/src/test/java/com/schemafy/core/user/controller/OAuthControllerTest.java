package com.schemafy.core.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.config.TestSecurityConfig;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.jwt.JwtProperties;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.common.security.jwt.JwtTokenIssuer;
import com.schemafy.core.common.security.jwt.WebExchangeErrorWriter;
import com.schemafy.core.user.oauth.GitHubOAuthProperties;
import com.schemafy.core.user.oauth.GitHubOAuthService;
import com.schemafy.core.user.oauth.GitHubUserInfo;
import com.schemafy.core.user.service.UserService;
import com.schemafy.domain.user.domain.User;

import reactor.core.publisher.Mono;

import static com.schemafy.core.user.docs.UserApiSnippets.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@WebFluxTest(controllers = OAuthController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureRestDocs
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
  private JwtProperties jwtProperties;

  @MockitoBean
  private JwtProvider jwtProvider;

  @MockitoBean
  private WebExchangeErrorWriter webExchangeErrorWriter;

  @BeforeEach
  void setUp() {
    JwtProperties.Cookie cookie = new JwtProperties.Cookie();
    cookie.setSecure(false);
    cookie.setSameSite("Strict");
    given(jwtProperties.getCookie()).willReturn(cookie);
    given(gitHubOAuthProperties.getFrontendCallbackUrl())
        .willReturn("http://localhost:3000/oauth/callback");
  }

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
          .expectHeader().valueMatches("Set-Cookie",
              ".*SameSite=Lax.*")
          .expectHeader().valueMatches("Set-Cookie",
              "^(?!.*Secure).*$")
          .expectBody()
          .consumeWith(document("oauth-github-authorize",
              oauthAuthorizeResponseHeaders()));
    }

    @Test
    @DisplayName("jwt cookie secure 설정이 true면 state 쿠키도 Secure로 설정된다")
    void authorize_setsSecureStateCookieWhenConfigured() {
      JwtProperties.Cookie cookie = new JwtProperties.Cookie();
      cookie.setSecure(true);
      cookie.setSameSite("Strict");
      given(jwtProperties.getCookie()).willReturn(cookie);
      given(gitHubOAuthService.getAuthorizeUrl(anyString()))
          .willReturn(
              "https://github.com/login/oauth/authorize?client_id=test");

      webTestClient.get()
          .uri(API_BASE_PATH + "/oauth/github/authorize")
          .exchange()
          .expectStatus().isFound()
          .expectHeader().valueMatches("Set-Cookie",
              ".*Secure.*");
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
          .expectStatus().isFound()
          .expectHeader().valueMatches("Location",
              "http://localhost:3000/oauth/callback\\?provider=github&error=state_mismatch")
          .expectHeader().valueMatches("Set-Cookie",
              ".*oauth_state=.*Max-Age=0.*");
    }

    @Test
    @DisplayName("state 쿠키가 없으면 에러를 반환한다")
    void callback_noStateCookie() {
      webTestClient.get()
          .uri(API_BASE_PATH
              + "/oauth/github/callback?code=test-code&state=some-state")
          .exchange()
          .expectStatus().isFound()
          .expectHeader().valueMatches("Location",
              "http://localhost:3000/oauth/callback\\?provider=github&error=state_mismatch")
          .expectHeader().valueMatches("Set-Cookie",
              ".*oauth_state=.*Max-Age=0.*");
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
          .expectHeader().exists("Set-Cookie")
          .expectBody().isEmpty();
    }

    @Test
    @DisplayName("provider error 콜백은 프론트엔드로 리다이렉트하고 state 쿠키를 만료한다")
    void callback_providerErrorRedirectsToFrontend() {
      String state = "valid-state";

      webTestClient.get()
          .uri(API_BASE_PATH
              + "/oauth/github/callback?state="
              + state
              + "&error=access_denied&error_description=user_denied")
          .cookie("oauth_state", state)
          .exchange()
          .expectStatus().isFound()
          .expectHeader().valueMatches("Location",
              "http://localhost:3000/oauth/callback\\?provider=github&error=access_denied&error_description=user_denied")
          .expectHeader().valueMatches("Set-Cookie",
              ".*oauth_state=.*Max-Age=0.*SameSite=Lax.*");

      verify(gitHubOAuthService, never())
          .exchangeCodeForToken(anyString());
      verify(gitHubOAuthService, never())
          .fetchGitHubUser(anyString());
      verify(userService, never()).loginOrSignUpOAuth(any());
      verify(jwtTokenIssuer, never())
          .issueTokens(anyString(), anyString());
    }

    @Test
    @DisplayName("토큰 교환 실패 시 프론트엔드로 에러 리다이렉트한다")
    void callback_tokenExchangeFailed() {
      String state = "valid-state";
      given(gitHubOAuthService.exchangeCodeForToken("test-code"))
          .willReturn(Mono.error(new RuntimeException("token exchange error")));

      webTestClient.get()
          .uri(API_BASE_PATH
              + "/oauth/github/callback?code=test-code&state="
              + state)
          .cookie("oauth_state", state)
          .exchange()
          .expectStatus().isFound()
          .expectHeader().valueMatches("Location",
              ".*error=server_error.*")
          .expectHeader().valueMatches("Set-Cookie",
              ".*oauth_state=.*Max-Age=0.*");
    }

    @Test
    @DisplayName("사용자 정보 조회 실패 시 프론트엔드로 에러 리다이렉트한다")
    void callback_userInfoFetchFailed() {
      String state = "valid-state";
      given(gitHubOAuthService.exchangeCodeForToken("test-code"))
          .willReturn(Mono.just("gho_test_token"));
      given(gitHubOAuthService.fetchGitHubUser("gho_test_token"))
          .willReturn(Mono.error(new RuntimeException("user info error")));

      webTestClient.get()
          .uri(API_BASE_PATH
              + "/oauth/github/callback?code=test-code&state="
              + state)
          .cookie("oauth_state", state)
          .exchange()
          .expectStatus().isFound()
          .expectHeader().valueMatches("Location",
              ".*error=server_error.*")
          .expectHeader().valueMatches("Set-Cookie",
              ".*oauth_state=.*Max-Age=0.*");
    }

    @Test
    @DisplayName("code와 error가 모두 없으면 잘못된 요청 에러를 반환한다")
    void callback_missingCodeAndError() {
      String state = "valid-state";

      webTestClient.get()
          .uri(API_BASE_PATH + "/oauth/github/callback?state=" + state)
          .cookie("oauth_state", state)
          .exchange()
          .expectStatus().isFound()
          .expectHeader().valueMatches("Location",
              ".*error=invalid_request.*error_description=code(%20|\\+)is(%20|\\+)missing.*")
          .expectHeader().valueMatches("Set-Cookie",
              ".*oauth_state=.*Max-Age=0.*");
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
    return User.signUpOAuth(id, email, name);
  }

}
