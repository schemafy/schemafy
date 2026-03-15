package com.schemafy.api.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;
import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.testsupport.user.UserHttpTestSupport;
import com.schemafy.api.user.controller.dto.request.SignUpRequest;
import com.schemafy.core.ulid.application.service.UlidGenerator;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import static com.schemafy.api.user.docs.UserApiSnippets.*;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("UserController 통합 테스트")
class UserControllerTest extends UserHttpTestSupport {

  private static final String API_BASE_PATH = ApiPath.API.replace("{version}",
      "v1.0");
  private static final String PUBLIC_API_BASE_PATH = ApiPath.PUBLIC_API
      .replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    cleanupUserFixtures().block();
  }

  @Test
  @DisplayName("ID로 회원 조회에 성공한다")
  @WithMockUser(username = "test-user-id")
  void getUserSuccess() {
    SignUpRequest request = new SignUpRequest("test@example.com",
        "Test User", "password");
    User user = createUser(request.email(), request.name(), request.password());

    String userId = user.id();
    Assertions.assertNotNull(userId);
    String accessToken = generateAccessToken(user.id());

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
        .jsonPath("$.id").isEqualTo(userId)
        .jsonPath("$.email").isEqualTo("test@example.com");
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
        .jsonPath("$.status").isEqualTo(404)
        .jsonPath("$.reason")
        .isEqualTo(UserErrorCode.NOT_FOUND.code());
  }

  @Test
  @DisplayName("인증된 사용자는 타인의 회원 정보 조회에 성공한다")
  void getUserSuccessWhenAccessingOtherUser() {
    SignUpRequest userARequest = new SignUpRequest("userA@example.com",
        "User A", "password");
    User userA = createUser(userARequest.email(), userARequest.name(),
        userARequest.password());

    SignUpRequest userBRequest = new SignUpRequest("userB@example.com",
        "User B", "password");
    User userB = createUser(userBRequest.email(), userBRequest.name(),
        userBRequest.password());

    String userAId = userA.id();
    String userBId = userB.id();
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
        .jsonPath("$.id").isEqualTo(userBId)
        .jsonPath("$.email").isEqualTo("userb@example.com");
  }

  @Test
  @DisplayName("인증 없이 회원 정보 조회 시 실패한다")
  void getUserFailWhenNotAuthenticated() {
    SignUpRequest request = new SignUpRequest("test@example.com",
        "Test User", "password");
    User user = createUser(request.email(), request.name(), request.password());

    webTestClient
        .get()
        .uri(API_BASE_PATH + "/users/{userId}", user.id())
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("내 정보 조회에 성공한다")
  void getMyInfoSuccess() {
    SignUpRequest signUpRequest = new SignUpRequest("TEST-ME@EXAMPLE.COM",
        "Test User Me", "password");

    EntityExchangeResult<byte[]> result = webTestClient.post()
        .uri(PUBLIC_API_BASE_PATH + "/users/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(signUpRequest)
        .exchange()
        .expectStatus().isOk()
        .expectBody(byte[].class).returnResult();

    String responseBody = new String(result.getResponseBody());
    String userId = JsonPath.read(responseBody, "$.id");
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
        .jsonPath("$.id").isEqualTo(userId)
        .jsonPath("$.email").isEqualTo("test-me@example.com");
  }

  @Test
  @DisplayName("로그아웃에 성공한다 (인증된 사용자)")
  void logoutSuccess() {
    SignUpRequest signUpRequest = new SignUpRequest("logout-test@example.com",
        "Logout User", "password");
    User user = createUser(signUpRequest.email(), signUpRequest.name(),
        signUpRequest.password());

    String userId = user.id();
    String accessToken = generateAccessToken(userId);

    webTestClient
        .mutateWith(mockUser(userId))
        .post()
        .uri(API_BASE_PATH + "/users/logout")
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(document("user-logout",
            logoutRequestHeaders(),
            logoutResponseHeaders()))
        .consumeWith(result -> {
          var cookies = result.getResponseHeaders().get("Set-Cookie");
          Assertions.assertNotNull(cookies);
          Assertions.assertTrue(cookies.stream().anyMatch(c -> c.contains("accessToken=;")));
          Assertions.assertTrue(cookies.stream().anyMatch(c -> c.contains("refreshToken=;")));
          Assertions.assertTrue(cookies.stream().allMatch(c -> c.contains("Max-Age=0")));
        });
  }

  @Test
  @DisplayName("인증 없이 로그아웃 시 실패한다")
  void logoutFailWhenNotAuthenticated() {
    webTestClient
        .post()
        .uri(API_BASE_PATH + "/users/logout")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("사용자 A의 로그아웃이 사용자 B에게 영향을 주지 않는다")
  void logoutDoesNotAffectOtherUsers() {
    SignUpRequest userARequest = new SignUpRequest("userA@example.com",
        "UserA", "password");
    User userA = createUser(userARequest.email(), userARequest.name(),
        userARequest.password());

    SignUpRequest userBRequest = new SignUpRequest("userB@example.com",
        "UserB", "password");
    User userB = createUser(userBRequest.email(), userBRequest.name(),
        userBRequest.password());

    String userAId = userA.id();
    String userBId = userB.id();
    String userAToken = generateAccessToken(userAId);
    String userBToken = generateAccessToken(userBId);

    // 사용자 A가 로그아웃 수행
    webTestClient
        .mutateWith(mockUser(userAId))
        .post()
        .uri(API_BASE_PATH + "/users/logout")
        .header("Authorization", "Bearer " + userAToken)
        .exchange()
        .expectStatus().isOk();

    // 사용자 B는 여전히 정상적으로 자신의 리소스에 접근 가능
    webTestClient
        .mutateWith(mockUser(userBId))
        .get()
        .uri(API_BASE_PATH + "/users/{userId}", userBId)
        .header("Authorization", "Bearer " + userBToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.id").isEqualTo(userBId)
        .jsonPath("$.email").isEqualTo("userb@example.com");

    // 사용자 B는 다른 API도 정상적으로 사용 가능
    webTestClient
        .mutateWith(mockUser(userBId))
        .get()
        .uri(API_BASE_PATH + "/users")
        .header("Authorization", "Bearer " + userBToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.id").isEqualTo(userBId);
  }

}
