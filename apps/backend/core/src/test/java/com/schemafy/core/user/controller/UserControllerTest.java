package com.schemafy.core.user.controller;

import java.util.HashMap;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.jayway.jsonpath.JsonPath;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.ulid.generator.UlidGenerator;
import com.schemafy.core.user.controller.dto.request.LoginRequest;
import com.schemafy.core.user.controller.dto.request.SignUpRequest;
import com.schemafy.core.user.repository.UserRepository;

import reactor.test.StepVerifier;

import static com.schemafy.core.user.docs.UserApiSnippets.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("MemberController 통합 테스트")
class UserControllerTest {

    private static final String API_BASE_PATH = ApiPath.PUBLIC_API.replace(
            "{version}",
            "v1.0");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll().block();
    }

    private String generateAccessToken(String userId) {
        return jwtProvider.generateAccessToken(userId, new HashMap<>(),
                System.currentTimeMillis());
    }

    private String generateRefreshToken(String userId) {
        return jwtProvider.generateRefreshToken(userId);
    }

    @Test
    @DisplayName("회원가입에 성공한다")
    void signUpSuccess() {
        // given
        SignUpRequest request = new SignUpRequest("test@example.com",
                "Test User", "password");

        // when & then
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
                        signUpResponseHeaders(),
                        signUpResponse()))
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isNotEmpty()
                .jsonPath("$.result.email").isEqualTo("test@example.com");

        // then - db 검증
        StepVerifier.create(userRepository.findByEmail("test@example.com"))
                .as("user should be persisted with auditing columns")
                .assertNext(user -> {
                    assertThat(user.getEmail()).isEqualTo("test@example.com");
                    assertThat(user.getName()).isEqualTo("Test User");
                    assertThat(user.getId()).isNotNull();
                    assertThat(user.getCreatedAt()).isNotNull();
                    assertThat(user.getUpdatedAt()).isNotNull();
                    assertThat(user.getDeletedAt()).isNull();
                })
                .verifyComplete();
    }

    @DisplayName("유효하지 않은 회원가입 요청은 실패한다")
    @ParameterizedTest
    @MethodSource("invalidSignUpRequests")
    void signUpFail(SignUpRequest request) {
        // when & then
        webTestClient.post().uri(API_BASE_PATH + "/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code")
                .isEqualTo(ErrorCode.COMMON_INVALID_PARAMETER.getCode());
    }

    static Stream<Arguments> invalidSignUpRequests() {
        return Stream.of(
                Arguments.of(new SignUpRequest("", "Test User", "password")), // empty
                // email
                Arguments.of(new SignUpRequest("invalid-email", "Test User",
                        "password")), // invalid
                // email
                Arguments.of(
                        new SignUpRequest("test@example.com", "", "password")), // empty
                // name
                Arguments.of(
                        new SignUpRequest("test@example.com", "Test User", "")) // empty
                                                                                            // password
        );
    }

    @Test
    @DisplayName("ID로 회원 조회에 성공한다")
    void getUserSuccess() {
        // given
        SignUpRequest signUpRequest = new SignUpRequest("test2@example.com",
                "Test User 2", "password");

        EntityExchangeResult<byte[]> result = webTestClient.post()
                .uri(API_BASE_PATH + "/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(signUpRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class).returnResult();

        String responseBody = new String(result.getResponseBody());
        String userId = JsonPath.read(responseBody, "$.result.id");
        String accessToken = generateAccessToken(userId);

        // when & then
        webTestClient.get().uri(API_BASE_PATH + "/users/{userId}", userId)
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("user-get",
                        getUserPathParameters(),
                        getUserRequestHeaders(),
                        getUserResponseHeaders(),
                        getUserResponse()))
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo(userId)
                .jsonPath("$.result.email").isEqualTo(signUpRequest.email());
    }

    @Test
    @DisplayName("존재하지 않는 회원은 조회에 실패한다")
    void getUserNotFound() {
        // given
        String nonExistentUserId = UlidGenerator.generate();
        String accessToken = generateAccessToken(nonExistentUserId);

        // when & then
        webTestClient.get()
                .uri(API_BASE_PATH + "/users/{userId}", nonExistentUserId)
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code")
                .isEqualTo(ErrorCode.USER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("로그인에 성공한다")
    void loginSuccess() {
        // given
        SignUpRequest signUpRequest = new SignUpRequest("test@example.com",
                "Test User", "password");
        webTestClient.post().uri(API_BASE_PATH + "/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(signUpRequest)
                .exchange()
                .expectStatus().isOk();

        LoginRequest loginRequest = new LoginRequest("test@example.com",
                "password");

        // when & then
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
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.email").isEqualTo("test@example.com");
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
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code")
                .isEqualTo(ErrorCode.COMMON_INVALID_PARAMETER.getCode());
    }

    static Stream<Arguments> invalidLoginRequests() {
        return Stream.of(
                Arguments.of(new LoginRequest("", "password")), // empty email
                Arguments.of(new LoginRequest("invalid-email", "password")), // invalid
                // email
                Arguments.of(new LoginRequest("test@example.com", "")) // empty
                                                                      // password
        );
    }

    @Test
    @DisplayName("로그인 시 존재하지 않는 이메일이면 실패한다")
    void loginFailEmailNotFound() {
        // given
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com",
                "password");

        // when & then
        webTestClient.post().uri(API_BASE_PATH + "/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code")
                .isEqualTo(ErrorCode.USER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("로그인 시 비밀번호가 틀리면 실패한다")
    void loginFailPasswordMismatch() {
        // given
        SignUpRequest signUpRequest = new SignUpRequest("test@example.com",
                "Test User", "password");
        webTestClient.post().uri(API_BASE_PATH + "/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(signUpRequest)
                .exchange()
                .expectStatus().isOk();

        LoginRequest loginRequest = new LoginRequest("test@example.com",
                "wrong_password");

        // when & then
        webTestClient.post().uri(API_BASE_PATH + "/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code")
                .isEqualTo(ErrorCode.LOGIN_FAILED.getCode());
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 토큰 갱신에 성공한다")
    void refreshTokenSuccess() {
        // given
        SignUpRequest signUpRequest = new SignUpRequest("test@example.com",
                "Test User", "password");
        EntityExchangeResult<byte[]> signupResult = webTestClient.post()
                .uri(API_BASE_PATH + "/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(signUpRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class).returnResult();

        String responseBody = new String(signupResult.getResponseBody());
        String userId = JsonPath.read(responseBody, "$.result.id");
        String refreshToken = generateRefreshToken(userId);

        // when & then - 쿠키로 Refresh Token 전달
        webTestClient.post().uri(API_BASE_PATH + "/users/refresh")
                .cookie("refreshToken", refreshToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("Authorization")
                .expectHeader().exists("Set-Cookie")
                .expectBody()
                .consumeWith(document("user-refresh",
                        refreshTokenRequestCookies(),
                        refreshTokenResponseHeaders(),
                        refreshTokenResponse()))
                .jsonPath("$.success").isEqualTo(true);
    }

    @Test
    @DisplayName("잘못된 타입의 토큰으로 갱신 시 실패한다")
    void refreshTokenFailWithAccessToken() {
        // given
        SignUpRequest signUpRequest = new SignUpRequest("test@example.com",
                "Test User", "password");
        EntityExchangeResult<byte[]> signupResult = webTestClient.post()
                .uri(API_BASE_PATH + "/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(signUpRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class).returnResult();

        String responseBody = new String(signupResult.getResponseBody());
        String userId = JsonPath.read(responseBody, "$.result.id");
        String accessToken = generateAccessToken(userId);

        // when & then - 쿠키로 Access Token 전달 (잘못된 토큰 타입)
        webTestClient.post().uri(API_BASE_PATH + "/users/refresh")
                .cookie("refreshToken", accessToken)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("A004");
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키가 없으면 갱신 시 실패한다")
    void refreshTokenFailWithoutCookie() {
        // when & then - 쿠키 없이 요청
        webTestClient.post().uri(API_BASE_PATH + "/users/refresh")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code")
                .isEqualTo(ErrorCode.MISSING_REFRESH_TOKEN.getCode());
    }

}
