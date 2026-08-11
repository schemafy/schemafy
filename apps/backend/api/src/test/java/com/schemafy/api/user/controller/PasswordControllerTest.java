package com.schemafy.api.user.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.api.testsupport.user.CapturingPasswordResetEmailTestAdapter;
import com.schemafy.api.testsupport.user.InMemoryAuthTokenTestAdapter;
import com.schemafy.api.testsupport.user.UserHttpTestSupport;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenType;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@DisplayName("PasswordController HTTP 계약 테스트")
class PasswordControllerTest extends UserHttpTestSupport {

  private static final String API_BASE_PATH = ApiPath.PUBLIC_API.replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private InMemoryAuthTokenTestAdapter authTokenTestAdapter;

  @Autowired
  private CapturingPasswordResetEmailTestAdapter passwordResetEmailTestAdapter;

  @BeforeEach
  void setUp() {
    authTokenTestAdapter.clear();
    passwordResetEmailTestAdapter.clear();
    cleanupUserFixtures().block();
  }

  @Test
  @DisplayName("로컬 비밀번호 계정의 재설정 링크 요청은 200으로 응답하고 메일을 보낸다")
  void requestPasswordResetForLocalAccount() {
    User user = createUser("reset@example.com", "Reset User", "current-password");

    webTestClient.post().uri(API_BASE_PATH + "/users/password-reset-requests")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("email", user.email()))
        .exchange()
        .expectStatus().isOk()
        .expectBody().isEmpty();

    assertThat(passwordResetEmailTestAdapter.get(user.email())).isNotNull();
  }

  @Test
  @DisplayName("존재하지 않는 계정의 재설정 링크 요청도 200으로 응답한다")
  void requestPasswordResetForUnknownAccountReturnsGenericSuccess() {
    webTestClient.post().uri(API_BASE_PATH + "/users/password-reset-requests")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("email", "unknown@example.com"))
        .exchange()
        .expectStatus().isOk()
        .expectBody().isEmpty();

    assertThat(passwordResetEmailTestAdapter.get("unknown@example.com")).isNull();
  }

  @Test
  @DisplayName("유효하지 않은 재설정 요청은 400 INVALID_PARAMETER를 반환한다")
  void requestPasswordResetRejectsInvalidEmail() {
    webTestClient.post().uri(API_BASE_PATH + "/users/password-reset-requests")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("email", "invalid-email"))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.reason").isEqualTo(CommonErrorCode.INVALID_PARAMETER.code());
  }

  @Test
  @DisplayName("72자를 넘는 새 비밀번호는 400 INVALID_PARAMETER를 반환한다")
  void changePasswordRejectsNewPasswordExceedingCharacterLimit() {
    webTestClient.post().uri(API_BASE_PATH + "/users/password")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of(
            "resetToken", "user-id.raw-token",
            "newPassword", "a".repeat(73)))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.reason").isEqualTo(CommonErrorCode.INVALID_PARAMETER.code());
  }

  @Test
  @DisplayName("유효한 재설정 token으로 비로그인 비밀번호 변경 후 token은 재사용할 수 없다")
  void changePasswordWithResetToken() {
    User user = createUser("token@example.com", "Token User", "current-password");
    String rawToken = "raw-token";
    authTokenTestAdapter.save(new AuthToken(AuthTokenType.PASSWORD_RESET, user.id(), rawToken,
        0, 1, Instant.now().plusSeconds(600))).block();
    Map<String, String> request = Map.of(
        "resetToken", user.id() + "." + rawToken,
        "newPassword", "new-password");

    webTestClient.post().uri(API_BASE_PATH + "/users/password")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isNoContent()
        .expectBody().isEmpty();

    assertThat(passwordEncoder.matches("new-password", getUser(user.id()).password())).isTrue();

    webTestClient.post().uri(API_BASE_PATH + "/users/password")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.reason").isEqualTo(UserErrorCode.PASSWORD_RESET_TOKEN_INVALID.code());
  }

  @Test
  @DisplayName("Bearer 인증 사용자는 현재 비밀번호 확인 후 204로 변경한다")
  void changePasswordWithAuthenticatedUser() {
    User user = createUser("authenticated@example.com", "Authenticated User", "current-password");

    webTestClient.post().uri(API_BASE_PATH + "/users/password")
        .header("Authorization", "Bearer " + generateAccessToken(user.id()))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of(
            "currentPassword", "current-password",
            "newPassword", "new-password"))
        .exchange()
        .expectStatus().isNoContent()
        .expectBody().isEmpty();

    assertThat(passwordEncoder.matches("new-password", getUser(user.id()).password())).isTrue();
  }

}
