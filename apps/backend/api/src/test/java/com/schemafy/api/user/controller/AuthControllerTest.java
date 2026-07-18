package com.schemafy.api.user.controller;

import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.jayway.jsonpath.JsonPath;
import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.exception.AuthErrorCode;
import com.schemafy.api.common.exception.CommonErrorCode;
import com.schemafy.api.testsupport.user.CapturingEmailVerificationTestAdapter;
import com.schemafy.api.testsupport.user.InMemoryAuthTokenTestAdapter;
import com.schemafy.api.testsupport.user.UserHttpTestSupport;
import com.schemafy.api.user.controller.dto.request.LoginRequest;
import com.schemafy.api.user.controller.dto.request.SendSignUpEmailCodeRequest;
import com.schemafy.api.user.controller.dto.request.SignUpRequest;
import com.schemafy.api.user.controller.dto.request.VerifySignUpEmailRequest;
import com.schemafy.core.ulid.application.service.UlidGenerator;
import com.schemafy.core.user.domain.UserStatus;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import static com.epages.restdocs.apispec.WebTestClientRestDocumentationWrapper.document;
import static com.schemafy.api.user.docs.UserApiSnippets.*;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("AuthController 통합 테스트")
class AuthControllerTest extends UserHttpTestSupport {

  private static final String API_BASE_PATH = ApiPath.PUBLIC_API.replace(
      "{version}",
      "v1.0");
  private static final String VALID_SIGNUP_VERIFICATION_TOKEN = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private CapturingEmailVerificationTestAdapter emailVerificationTestAdapter;

  @Autowired
  private InMemoryAuthTokenTestAdapter authTokenTestAdapter;

  @BeforeEach
  void setUp() {
    emailVerificationTestAdapter.clear();
    authTokenTestAdapter.clear();
    cleanupUserFixtures().block();
  }

  @Test
  @DisplayName("회원가입 이메일 인증 코드 발송에 성공한다")
  void sendSignUpEmailCodeSuccess() {
    SendSignUpEmailCodeRequest request = new SendSignUpEmailCodeRequest(
        "test@example.com");

    webTestClient.post().uri(API_BASE_PATH + "/users/signup/email-code")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isAccepted()
        .expectHeader().doesNotExist("Authorization")
        .expectBody()
        .consumeWith(document("user-signup-email-code",
            sendSignUpEmailCodeRequest(),
            signUpEmailVerificationResponse()))
        .jsonPath("$.email").isEqualTo("test@example.com")
        .jsonPath("$.expiresAt").isNotEmpty();

    assertThat(getUserByEmail("test@example.com")).isNull();
    assertThat(emailVerificationTestAdapter.get("test@example.com")).isNotNull();
  }

  @Test
  @DisplayName("기존 회원가입 이메일 인증 코드가 유효하면 같은 인증 만료 시각을 반환하고 인증 메일을 추가 발송하지 않는다")
  void sendSignUpEmailCodeReturnsVerificationExpiresAtWithoutSendingMailWhenCodeIsStillValid() {
    SendSignUpEmailCodeRequest request = new SendSignUpEmailCodeRequest(
        "test@example.com");

    webTestClient.post().uri(API_BASE_PATH + "/users/signup/email-code")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isAccepted()
        .expectBody()
        .jsonPath("$.email").isEqualTo("test@example.com")
        .jsonPath("$.expiresAt").isNotEmpty();

    CapturingEmailVerificationTestAdapter.SentVerificationCode firstSentCode = emailVerificationTestAdapter.get(
        "test@example.com");

    webTestClient.post().uri(API_BASE_PATH + "/users/signup/email-code")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isAccepted()
        .expectBody()
        .jsonPath("$.email").isEqualTo("test@example.com")
        .jsonPath("$.expiresAt").isEqualTo(firstSentCode.expiresAt().toString());

    CapturingEmailVerificationTestAdapter.SentVerificationCode secondSentCode = emailVerificationTestAdapter.get(
        "test@example.com");
    assertThat(secondSentCode.code()).isEqualTo(firstSentCode.code());
    assertThat(emailVerificationTestAdapter.sendCount("test@example.com"))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("회원가입 이메일 인증에 성공하면 signup verification token을 발급한다")
  void verifySignUpEmailSuccess() {
    webTestClient.post().uri(API_BASE_PATH + "/users/signup/email-code")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new SendSignUpEmailCodeRequest("test@example.com"))
        .exchange()
        .expectStatus().isAccepted();

    String code = emailVerificationTestAdapter.get("test@example.com").code();
    VerifySignUpEmailRequest verifyRequest = new VerifySignUpEmailRequest(
        "test@example.com", code);

    webTestClient.post().uri(API_BASE_PATH + "/users/signup/email-code/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(verifyRequest)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().doesNotExist("Authorization")
        .expectBody()
        .consumeWith(document("user-signup-email-code-verify",
            verifySignUpEmailRequest(),
            verifySignUpEmailResponse()))
        .jsonPath("$.email").isEqualTo("test@example.com")
        .jsonPath("$.signupVerificationToken").isNotEmpty()
        .jsonPath("$.expiresAt").isNotEmpty();
  }

  @Test
  @DisplayName("회원가입 이메일 인증 코드는 재사용할 수 없다")
  void verifySignUpEmailCodeCannotBeReused() {
    webTestClient.post().uri(API_BASE_PATH + "/users/signup/email-code")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new SendSignUpEmailCodeRequest("test@example.com"))
        .exchange()
        .expectStatus().isAccepted();

    String code = emailVerificationTestAdapter.get("test@example.com").code();
    VerifySignUpEmailRequest verifyRequest = new VerifySignUpEmailRequest(
        "test@example.com", code);

    webTestClient.post().uri(API_BASE_PATH + "/users/signup/email-code/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(verifyRequest)
        .exchange()
        .expectStatus().isOk();

    webTestClient.post().uri(API_BASE_PATH + "/users/signup/email-code/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(verifyRequest)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.reason")
        .isEqualTo(UserErrorCode.VERIFICATION_CODE_EXPIRED.code());
  }

  @Test
  @DisplayName("이메일 인증이 완료된 회원가입에 성공하면 토큰을 발급한다")
  void signUpSuccess() {
    String signupVerificationToken = verifyEmailAndGetSignupToken("test@example.com");
    SignUpRequest request = new SignUpRequest("test@example.com",
        "Test User", "password", signupVerificationToken);

    webTestClient.post().uri(API_BASE_PATH + "/users/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().exists("Authorization")
        .expectHeader().exists("Set-Cookie")
        .expectBody()
        .consumeWith(document("user-signup",
            signUpRequest(),
            loginResponseHeaders(),
            loginResponse()))
        .jsonPath("$.email").isEqualTo("test@example.com");

    assertThat(getUserByEmail("test@example.com").status())
        .isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  @DisplayName("이메일 검증 토큰 없이 회원가입하면 EMAIL_NOT_VERIFIED를 반환한다")
  void signUpFailEmailNotVerified() {
    SignUpRequest request = new SignUpRequest("test@example.com",
        "Test User", "password", null);

    webTestClient.post().uri(API_BASE_PATH + "/users/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isForbidden()
        .expectBody()
        .jsonPath("$.reason")
        .isEqualTo(UserErrorCode.EMAIL_NOT_VERIFIED.code());

    assertThat(getUserByEmail("test@example.com")).isNull();
  }

  @DisplayName("유효하지 않은 회원가입 요청은 실패한다")
  @ParameterizedTest
  @MethodSource("invalidSignUpRequests")
  void signUpFail(SignUpRequest request) {
    webTestClient.post().uri(API_BASE_PATH + "/users/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo(400)
        .jsonPath("$.reason")
        .isEqualTo(CommonErrorCode.INVALID_PARAMETER.code());
  }

  static Stream<Arguments> invalidSignUpRequests() {
    return Stream.of(
        Arguments.of(new SignUpRequest("", "Test User", "password", "token")),
        Arguments.of(new SignUpRequest("invalid-email", "Test User",
            "password", "token")),
        Arguments.of(
            new SignUpRequest("test@example.com", "", "password", "token")),
        Arguments.of(new SignUpRequest("test@example.com", "a".repeat(201),
            "password", "token")),
        Arguments.of(new SignUpRequest("test@example.com", "Test User",
            "passwrd", "token")),
        Arguments.of(new SignUpRequest("test@example.com", "Test User",
            "", "token")),
        Arguments.of(new SignUpRequest("test@example.com", "Test User",
            "password", "")),
        Arguments.of(new SignUpRequest("test@example.com", "Test User",
            "password", "invalid-token")));
  }

  private String verifyEmailAndGetSignupToken(String email) {
    webTestClient.post().uri(API_BASE_PATH + "/users/signup/email-code")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new SendSignUpEmailCodeRequest(email))
        .exchange()
        .expectStatus().isAccepted();
    String code = emailVerificationTestAdapter.get(email).code();
    byte[] body = webTestClient.post().uri(API_BASE_PATH + "/users/signup/email-code/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new VerifySignUpEmailRequest(email, code))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.signupVerificationToken").isNotEmpty()
        .returnResult()
        .getResponseBody();
    return JsonPath.read(new String(body), "$.signupVerificationToken");
  }

  @Test
  @DisplayName("로그인에 성공한다")
  void loginSuccess() {
    createUser("test@example.com", "Test User", "password");

    LoginRequest loginRequest = new LoginRequest("test@example.com",
        "password");

    webTestClient.post().uri(API_BASE_PATH + "/users/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(loginRequest)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().exists("Authorization")
        .expectHeader().exists("Set-Cookie")
        .expectBody()
        .consumeWith(document("user-login",
            loginRequest(),
            loginResponseHeaders(),
            loginResponse()))
        .jsonPath("$.email").isEqualTo("test@example.com");
  }

  @DisplayName("유효하지 않은 로그인 요청은 실패한다")
  @ParameterizedTest
  @MethodSource("invalidLoginRequests")
  void loginFail(LoginRequest request) {
    webTestClient.post().uri(API_BASE_PATH + "/users/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo(400)
        .jsonPath("$.reason")
        .isEqualTo(CommonErrorCode.INVALID_PARAMETER.code());
  }

  static Stream<Arguments> invalidLoginRequests() {
    return Stream.of(
        Arguments.of(new LoginRequest("", "password")),
        Arguments.of(new LoginRequest("invalid-email", "password")),
        Arguments.of(new LoginRequest("test@example.com", "")));
  }

  @Test
  @DisplayName("로그인 시 존재하지 않는 이메일이면 실패한다")
  void loginFailEmailNotFound() {
    LoginRequest loginRequest = new LoginRequest("nonexistent@example.com",
        "password");

    webTestClient.post().uri(API_BASE_PATH + "/users/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(loginRequest)
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo(404)
        .jsonPath("$.reason")
        .isEqualTo(UserErrorCode.NOT_FOUND.code());
  }

  @Test
  @DisplayName("로그인 시 비밀번호가 틀리면 실패한다")
  void loginFailPasswordMismatch() {
    createUser("test@example.com", "Test User", "password");

    LoginRequest loginRequest = new LoginRequest("test@example.com",
        "wrong_password");

    webTestClient.post().uri(API_BASE_PATH + "/users/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(loginRequest)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo(400)
        .jsonPath("$.reason").isEqualTo(UserErrorCode.LOGIN_FAILED.code());
  }

  @Test
  @DisplayName("유효한 리프레시 토큰으로 토큰 갱신에 성공한다")
  void refreshTokenSuccess() {
    String userId = createUser("test@example.com", "Test User", "password").id();
    String refreshToken = generateRefreshToken(userId);

    webTestClient.post().uri(API_BASE_PATH + "/users/refresh")
        .cookie("refreshToken", refreshToken)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().exists("Authorization")
        .expectHeader().exists("Set-Cookie")
        .expectBody()
        .consumeWith(document("user-refresh",
            refreshTokenRequestCookies(),
            refreshTokenResponseHeaders()));
  }

  @Test
  @DisplayName("잘못된 타입의 토큰으로 갱신 시 실패한다")
  void refreshTokenFailWithAccessToken() {
    String userId = createUser("test@example.com", "Test User", "password").id();
    String accessToken = generateAccessToken(userId);

    // 쿠키로 Access Token 전달 (잘못된 토큰 타입)
    webTestClient.post().uri(API_BASE_PATH + "/users/refresh")
        .cookie("refreshToken", accessToken)
        .exchange()
        .expectStatus().isUnauthorized()
        .expectBody()
        .jsonPath("$.status").isEqualTo(401)
        .jsonPath("$.reason")
        .isEqualTo(AuthErrorCode.INVALID_TOKEN_TYPE.code());
  }

  @Test
  @DisplayName("리프레시 토큰 쿠키가 없으면 갱신 시 실패한다")
  void refreshTokenFailWithoutCookie() {
    webTestClient.post().uri(API_BASE_PATH + "/users/refresh")
        .exchange()
        .expectStatus().isUnauthorized()
        .expectBody()
        .jsonPath("$.status").isEqualTo(401)
        .jsonPath("$.reason")
        .isEqualTo(AuthErrorCode.MISSING_REFRESH_TOKEN.code());
  }

  @Test
  @DisplayName("유효하지 않은 리프레시 토큰이면 갱신 시 실패한다")
  void refreshTokenFailWithInvalidRefreshToken() {
    webTestClient.post().uri(API_BASE_PATH + "/users/refresh")
        .cookie("refreshToken", "invalid-refresh-token")
        .exchange()
        .expectStatus().isUnauthorized()
        .expectBody()
        .jsonPath("$.status").isEqualTo(401)
        .jsonPath("$.reason")
        .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN.code());
  }

  @Test
  @DisplayName("토큰의 사용자 정보가 없으면 갱신 시 실패한다")
  void refreshTokenFailWhenUserNotFound() {
    String unknownUserId = UlidGenerator.generate();
    String refreshToken = generateRefreshToken(unknownUserId);

    webTestClient.post().uri(API_BASE_PATH + "/users/refresh")
        .cookie("refreshToken", refreshToken)
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo(404)
        .jsonPath("$.reason")
        .isEqualTo(UserErrorCode.NOT_FOUND.code());
  }

}
