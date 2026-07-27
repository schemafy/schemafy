package com.schemafy.api.user.controller;

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
import com.schemafy.api.testsupport.user.CapturingEmailVerificationTestAdapter;
import com.schemafy.api.testsupport.user.UserHttpTestSupport;
import com.schemafy.api.user.controller.dto.request.SendSignUpEmailCodeRequest;
import com.schemafy.api.user.controller.dto.request.SignUpRequest;
import com.schemafy.api.user.controller.dto.request.VerifySignUpEmailRequest;
import com.schemafy.core.user.domain.UserStatus;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(properties = "auth.mail.enabled=false")
@AutoConfigureWebTestClient
@DisplayName("AuthController 인증 메일 비활성화 통합 테스트")
class AuthControllerMailDisabledTest extends UserHttpTestSupport {

  private static final String API_BASE_PATH = ApiPath.PUBLIC_API.replace(
      "{version}",
      "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private CapturingEmailVerificationTestAdapter emailVerificationTestAdapter;

  @BeforeEach
  void setUp() {
    emailVerificationTestAdapter.clear();
    cleanupUserFixtures().block();
  }

  @Test
  @DisplayName("인증 메일이 비활성화되면 검증 토큰 없이 가입한다")
  void signUpWithoutVerificationToken() {
    SignUpRequest request = new SignUpRequest(
        "test@example.com", "Test User", "password", null);

    webTestClient.post().uri(API_BASE_PATH + "/users/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().exists("Authorization")
        .expectHeader().exists("Set-Cookie")
        .expectBody()
        .jsonPath("$.email").isEqualTo("test@example.com");

    assertThat(getUserByEmail("test@example.com").status())
        .isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  @DisplayName("인증 메일이 비활성화되면 코드 발송 요청에 409를 반환한다")
  void sendSignUpEmailCodeMailDisabled() {
    webTestClient.post().uri(API_BASE_PATH + "/users/signup/email-code")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new SendSignUpEmailCodeRequest("test@example.com"))
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("$.reason").isEqualTo(UserErrorCode.AUTH_MAIL_DISABLED.code());

    assertThat(emailVerificationTestAdapter.get("test@example.com")).isNull();
  }

  @Test
  @DisplayName("인증 메일이 비활성화되면 코드 검증 요청에 409를 반환한다")
  void verifySignUpEmailMailDisabled() {
    webTestClient.post().uri(API_BASE_PATH + "/users/signup/email-code/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new VerifySignUpEmailRequest("test@example.com", "123456"))
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("$.reason").isEqualTo(UserErrorCode.AUTH_MAIL_DISABLED.code());
  }

}
