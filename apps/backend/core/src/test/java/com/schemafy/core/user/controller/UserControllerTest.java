package com.schemafy.core.user.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.ulid.generator.UlidGenerator;
import com.schemafy.core.user.controller.dto.request.SignUpRequest;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;

import static com.schemafy.core.user.docs.UserApiSnippets.*;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("UserController 통합 테스트")
class UserControllerTest {
    private static final String API_BASE_PATH = ApiPath.AUTH_API
            .replace("{version}", "v1.0");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    @DisplayName("ID로 회원 조회에 성공한다")
    @WithMockUser(username = "test-user-id")
    void getUserSuccess() {
        SignUpRequest request = new SignUpRequest("test@example.com",
                "Test User", "password");
        User user = User
                .signUp(request.toCommand().toUserInfo(), passwordEncoder)
                .flatMap(userRepository::save)
                .blockOptional()
                .orElseThrow();

        String userId = user.getId();
        Assertions.assertNotNull(userId);
        String accessToken = generateAccessToken(user.getId());

        webTestClient
                .mutateWith(mockUser(userId))
                .get()
                .uri(API_BASE_PATH + "/users/{userId}", userId)
                .header("Authorization", accessToken)
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
                .jsonPath("$.result.email").isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("존재하지 않는 회원은 조회에 실패한다")
    void getUserNotFound() {
        String nonExistentUserId = UlidGenerator.generate();

        webTestClient
                .mutateWith(mockUser(nonExistentUserId))
                .get()
                .uri(API_BASE_PATH + "/users/{userId}", nonExistentUserId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code")
                .isEqualTo(ErrorCode.USER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("타인의 회원 정보 조회 시 권한 없음으로 실패한다")
    void getUserFailWhenAccessingOtherUser() {
        // 두 명의 사용자 생성
        SignUpRequest userARequest = new SignUpRequest("userA@example.com",
                "User A", "password");
        User userA = User
                .signUp(userARequest.toCommand().toUserInfo(), passwordEncoder)
                .flatMap(userRepository::save)
                .blockOptional()
                .orElseThrow();

        SignUpRequest userBRequest = new SignUpRequest("userB@example.com",
                "User B", "password");
        User userB = User
                .signUp(userBRequest.toCommand().toUserInfo(), passwordEncoder)
                .flatMap(userRepository::save)
                .blockOptional()
                .orElseThrow();

        String userAId = userA.getId();
        String userBId = userB.getId();
        String userAAccessToken = generateAccessToken(userAId);

        // userA로 인증했지만 userB의 정보를 조회 시도
        webTestClient
                .mutateWith(mockUser(userAId))
                .get()
                .uri(API_BASE_PATH + "/users/{userId}", userBId)
                .header("Authorization", userAAccessToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("인증 없이 회원 정보 조회 시 실패한다")
    void getUserFailWhenNotAuthenticated() {
        SignUpRequest request = new SignUpRequest("test@example.com",
                "Test User", "password");
        User user = User
                .signUp(request.toCommand().toUserInfo(), passwordEncoder)
                .flatMap(userRepository::save)
                .blockOptional()
                .orElseThrow();

        String userId = user.getId();

        // 인증 없이 조회 시도
        webTestClient
                .get()
                .uri(API_BASE_PATH + "/users/{userId}", userId)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
