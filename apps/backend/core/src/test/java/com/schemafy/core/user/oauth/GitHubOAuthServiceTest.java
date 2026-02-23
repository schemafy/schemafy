package com.schemafy.core.user.oauth;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubOAuthService 테스트")
class GitHubOAuthServiceTest {

  @Mock
  private WebClient gitHubWebClient;

  @Mock
  private WebClient gitHubApiWebClient;

  @Mock
  private RequestBodyUriSpec requestBodyUriSpec;

  @Mock
  private RequestBodySpec requestBodySpec;

  @Mock
  private RequestHeadersSpec<?> requestHeadersSpec;

  @Mock
  @SuppressWarnings("rawtypes")
  private RequestHeadersUriSpec requestHeadersUriSpec;

  @Mock
  private ResponseSpec responseSpec;

  private GitHubOAuthService gitHubOAuthService;

  @BeforeEach
  void setUp() {
    GitHubOAuthProperties properties = new GitHubOAuthProperties();
    properties.setClientId("test-client-id");
    properties.setClientSecret("test-client-secret");
    properties.setRedirectUri("http://localhost:8080/callback");
    properties.setFrontendCallbackUrl("http://localhost:3000/callback");

    gitHubOAuthService = new GitHubOAuthService(
        properties, gitHubWebClient, gitHubApiWebClient);
  }

  @Test
  @DisplayName("인가 URL을 올바르게 생성한다")
  void getAuthorizeUrl() {
    String url = gitHubOAuthService.getAuthorizeUrl("test-state");

    assertThat(url).contains("client_id=test-client-id");
    assertThat(url).contains("state=test-state");
    assertThat(url).contains("scope=user:email");
    assertThat(url).contains("redirect_uri=http://localhost:8080/callback");
  }

  @SuppressWarnings("unchecked")
  private void stubTokenExchange(Mono<?> responseMono) {
    given(gitHubWebClient.post()).willReturn(requestBodyUriSpec);
    given(requestBodyUriSpec.uri(anyString()))
        .willReturn(requestBodySpec);
    doReturn(requestHeadersSpec).when(requestBodySpec)
        .bodyValue(any());
    given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
    given(responseSpec.bodyToMono(
        any(ParameterizedTypeReference.class)))
        .willReturn(responseMono);
  }

  @SuppressWarnings("unchecked")
  private void stubApiGet(Mono<?> responseMono, Class<?> responseType) {
    given(gitHubApiWebClient.get()).willReturn(requestHeadersUriSpec);
    doReturn(requestHeadersSpec).when(requestHeadersUriSpec)
        .uri(anyString());
    doReturn(requestHeadersSpec).when(requestHeadersSpec)
        .header(anyString(), anyString());
    given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
    if (responseType != null) {
      doReturn(responseMono).when(responseSpec)
          .bodyToMono(responseType);
    } else {
      given(responseSpec.bodyToMono(
          any(ParameterizedTypeReference.class)))
          .willReturn(responseMono);
    }
  }

  @Nested
  @DisplayName("토큰 교환")
  class ExchangeCodeForToken {

    @Test
    @DisplayName("코드로 액세스 토큰을 교환한다")
    void exchangeCodeForToken_success() {
      stubTokenExchange(
          Mono.just(Map.of("access_token", "gho_test_token")));

      StepVerifier
          .create(
              gitHubOAuthService.exchangeCodeForToken("test-code"))
          .expectNext("gho_test_token")
          .verifyComplete();
    }

    @Test
    @DisplayName("토큰이 응답에 없으면 실패한다")
    void exchangeCodeForToken_noToken() {
      stubTokenExchange(
          Mono.just(Map.of("error", "bad_verification_code")));

      StepVerifier
          .create(
              gitHubOAuthService.exchangeCodeForToken("bad-code"))
          .expectErrorMatches(
              e -> e instanceof BusinessException
                  && ((BusinessException) e)
                      .getErrorCode() == ErrorCode.OAUTH_CODE_EXCHANGE_FAILED)
          .verify();
    }

  }

  @Nested
  @DisplayName("사용자 정보 조회")
  class FetchGitHubUser {

    @Test
    @DisplayName("GitHub 사용자 정보를 조회한다")
    void fetchGitHubUser_success() {
      GitHubUserInfo userInfo = new GitHubUserInfo(
          12345L, "testuser", "test@example.com", "Test User");

      stubApiGet(Mono.just(userInfo), GitHubUserInfo.class);

      StepVerifier
          .create(
              gitHubOAuthService.fetchGitHubUser("gho_test_token"))
          .assertNext(info -> {
            assertThat(info.id()).isEqualTo(12345L);
            assertThat(info.login()).isEqualTo("testuser");
            assertThat(info.email()).isEqualTo("test@example.com");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("사용자 정보 조회 실패 시 에러를 반환한다")
    void fetchGitHubUser_error() {
      stubApiGet(Mono.error(new RuntimeException("API error")),
          GitHubUserInfo.class);

      StepVerifier
          .create(
              gitHubOAuthService.fetchGitHubUser("invalid_token"))
          .expectErrorMatches(
              e -> e instanceof BusinessException
                  && ((BusinessException) e)
                      .getErrorCode() == ErrorCode.OAUTH_USER_INFO_FAILED)
          .verify();
    }

  }

  @Nested
  @DisplayName("이메일 조회")
  class FetchGitHubUserEmail {

    @Test
    @DisplayName("primary verified 이메일을 조회한다")
    void fetchGitHubUserEmail_success() {
      List<Map<String, Object>> emails = List.of(
          Map.of("email", "secondary@example.com", "primary", false,
              "verified", true),
          Map.of("email", "primary@example.com", "primary", true,
              "verified", true));

      stubApiGet(Mono.just(emails), null);

      StepVerifier
          .create(gitHubOAuthService
              .fetchGitHubUserEmail("gho_test_token"))
          .expectNext("primary@example.com")
          .verifyComplete();
    }

    @Test
    @DisplayName("primary verified 이메일이 없으면 실패한다")
    void fetchGitHubUserEmail_noVerifiedEmail() {
      List<Map<String, Object>> emails = List.of(
          Map.of("email", "unverified@example.com", "primary", true,
              "verified", false));

      stubApiGet(Mono.just(emails), null);

      StepVerifier
          .create(gitHubOAuthService
              .fetchGitHubUserEmail("gho_test_token"))
          .expectErrorMatches(
              e -> e instanceof BusinessException
                  && ((BusinessException) e)
                      .getErrorCode() == ErrorCode.OAUTH_EMAIL_NOT_AVAILABLE)
          .verify();
    }

  }

}
