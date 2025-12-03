package com.schemafy.core.user.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.ulid.generator.UlidGenerator;
import com.schemafy.core.user.controller.dto.request.SignUpRequest;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.workspace.repository.WorkspaceMemberRepository;
import com.schemafy.core.workspace.repository.WorkspaceRepository;
import com.schemafy.core.workspace.repository.vo.WorkspaceRole;

import reactor.test.StepVerifier;

import static com.schemafy.core.user.docs.UserApiSnippets.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("UserController 통합 테스트")
class UserControllerTest {

    private static final String API_BASE_PATH = ApiPath.API.replace("{version}",
            "v1.0");
    private static final String PUBLIC_API_BASE_PATH = ApiPath.PUBLIC_API
            .replace("{version}", "v1.0");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        workspaceMemberRepository.deleteAll().block();
        workspaceRepository.deleteAll().block();
        userRepository.deleteAll().block();
    }

    private String generateAccessToken(String userId) {
        return jwtProvider.generateAccessToken(userId, new HashMap<>(),
                System.currentTimeMillis());
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
    @DisplayName("인증된 사용자는 타인의 회원 정보 조회에 성공한다")
    void getUserSuccessWhenAccessingOtherUser() {
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

        // userA로 인증하여 userB의 정보를 조회
        webTestClient
                .mutateWith(mockUser(userAId))
                .get()
                .uri(API_BASE_PATH + "/users/{userId}", userBId)
                .header("Authorization", userAAccessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo(userBId)
                .jsonPath("$.result.email").isEqualTo("userB@example.com");
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

        webTestClient
                .get()
                .uri(API_BASE_PATH + "/users/{userId}", user.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("내 정보 조회에 성공한다")
    void getMyInfoSuccess() {
        SignUpRequest signUpRequest = new SignUpRequest("test-me@example.com",
                "Test User Me", "password");

        EntityExchangeResult<byte[]> result = webTestClient.post()
                .uri(PUBLIC_API_BASE_PATH + "/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(signUpRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class).returnResult();

        String responseBody = new String(result.getResponseBody());
        String userId = JsonPath.read(responseBody, "$.result.id");
        String accessToken = generateAccessToken(userId);

        webTestClient.get().uri(API_BASE_PATH + "/users")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("user-get-me",
                        getUserRequestHeaders(),
                        getUserResponseHeaders(),
                        getUserResponse()))
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.id").isEqualTo(userId)
                .jsonPath("$.result.email").isEqualTo(signUpRequest.email());
    }

    @Test
    @DisplayName("회원가입 시 개인 워크스페이스가 자동으로 생성된다")
    void signUpCreatesDefaultWorkspace() {
        SignUpRequest request = new SignUpRequest("test@example.com",
                "Test User", "password");

        EntityExchangeResult<byte[]> result = webTestClient.post()
                .uri(PUBLIC_API_BASE_PATH + "/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class).returnResult();

        String responseBody = new String(result.getResponseBody());
        String userId = JsonPath.read(responseBody, "$.result.id");

        // 워크스페이스 생성 검증
        StepVerifier
                .create(workspaceRepository.findByOwnerIdAndNotDeleted(userId))
                .as("default workspace should be created")
                .assertNext(workspace -> {
                    assertThat(workspace.getName())
                            .isEqualTo("Test User's Workspace");
                    assertThat(workspace.getDescription())
                            .isEqualTo("Personal workspace for Test User");
                    assertThat(workspace.getOwnerId()).isEqualTo(userId);
                })
                .verifyComplete();

        // 워크스페이스 멤버 검증
        StepVerifier
                .create(workspaceMemberRepository
                        .findByUserIdAndNotDeleted(userId))
                .as("user should be added as ADMIN to workspace")
                .assertNext(member -> {
                    assertThat(member.getUserId()).isEqualTo(userId);
                    assertThat(member.getRole())
                            .isEqualTo(WorkspaceRole.ADMIN.getValue());
                    assertThat(member.isAdmin()).isTrue();
                })
                .verifyComplete();
    }
}
