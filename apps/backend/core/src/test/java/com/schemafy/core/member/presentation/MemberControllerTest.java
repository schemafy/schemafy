package com.schemafy.core.member.presentation;

import com.github.f4b6a3.ulid.UlidCreator;
import com.jayway.jsonpath.JsonPath;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.member.domain.repository.MemberRepository;
import com.schemafy.core.member.presentation.dto.request.LoginRequest;
import com.schemafy.core.member.presentation.dto.request.SignUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.stream.Stream;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@DisplayName("MemberController 통합 테스트")
class MemberControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll().block();
    }

    @Test
    @DisplayName("회원가입에 성공한다")
    void signUpSuccess() {
        // given
        SignUpRequest request = new SignUpRequest("test@example.com", "Test User", "password");

        // when & then
        webTestClient.post().uri("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(System.out::println) // Print response
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isNotEmpty()
                .jsonPath("$.result.email").isEqualTo("test@example.com");
    }

    @DisplayName("유효하지 않은 회원가입 요청은 실패한다")
    @ParameterizedTest
    @MethodSource("invalidSignUpRequests")
    void signUp_fail_invalid_request(SignUpRequest request) {
        // when & then
        webTestClient.post().uri("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo(ErrorCode.COMMON_INVALID_PARAMETER.getCode());
    }

    static Stream<Arguments> invalidSignUpRequests() {
        return Stream.of(
                Arguments.of(new SignUpRequest("", "Test User", "password")), // empty email
                Arguments.of(new SignUpRequest("invalid-email", "Test User", "password")), // invalid email
                Arguments.of(new SignUpRequest("test@example.com", "", "password")), // empty name
                Arguments.of(new SignUpRequest("test@example.com", "Test User", "")) // empty password
        );
    }

    @Test
    @DisplayName("ID로 회원 조회에 성공한다")
    void getMemberSuccess() {
        // given
        SignUpRequest signUpRequest = new SignUpRequest("test2@example.com", "Test User 2", "password");

        EntityExchangeResult<byte[]> result = webTestClient.post().uri("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(signUpRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class).returnResult();

        String responseBody = new String(result.getResponseBody());
        String memberId = JsonPath.read(responseBody, "$.result.id");

        // when & then
        webTestClient.get().uri("/api/v1/members/{memberId}", memberId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo(memberId)
                .jsonPath("$.result.email").isEqualTo(signUpRequest.email());
    }

    @Test
    @DisplayName("존재하지 않는 회원은 실패한다")
    void getMember_fail_not_found() {
        // given
        String nonExistentMemberId = UlidCreator.getUlid().toString();

        // when & then
        webTestClient.get().uri("/api/v1/members/{memberId}", nonExistentMemberId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo(ErrorCode.MEMBER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("로그인에 성공한다")
    void login_success() {
        // given
        SignUpRequest signUpRequest = new SignUpRequest("test@example.com", "Test User", "password");
        webTestClient.post().uri("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(signUpRequest)
                .exchange()
                .expectStatus().isOk();

        LoginRequest loginRequest = new LoginRequest("test@example.com", "password");

        // when & then
        webTestClient.post().uri("/api/v1/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.email").isEqualTo("test@example.com");
    }

    @DisplayName("유효하지 않은 로그인 요청은 실패한다")
    @ParameterizedTest
    @MethodSource("invalidLoginRequests")
    void login_fail_invalid_request(LoginRequest request) {
        webTestClient.post().uri("/api/v1/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo(ErrorCode.COMMON_INVALID_PARAMETER.getCode());
    }

    static Stream<Arguments> invalidLoginRequests() {
        return Stream.of(
                Arguments.of(new LoginRequest("", "password")), // empty email
                Arguments.of(new LoginRequest("invalid-email", "password")), // invalid email
                Arguments.of(new LoginRequest("test@example.com", "")) // empty password
        );
    }

    @Test
    @DisplayName("로그인 시 존재하지 않는 이메일이면 실패한다")
    void login_fail_email_not_found() {
        // given
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "password");

        // when & then
        webTestClient.post().uri("/api/v1/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo(ErrorCode.MEMBER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("로그인 시 비밀번호가 틀리면 실패한다")
    void login_fail_password_mismatch() {
        // given
        SignUpRequest signUpRequest = new SignUpRequest("test@example.com", "Test User", "password");
        webTestClient.post().uri("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(signUpRequest)
                .exchange()
                .expectStatus().isOk();

        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrong_password");

        // when & then
        webTestClient.post().uri("/api/v1/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo(ErrorCode.LOGIN_FAILED.getCode());
    }
}
