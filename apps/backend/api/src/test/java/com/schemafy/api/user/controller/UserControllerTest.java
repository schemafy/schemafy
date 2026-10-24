package com.schemafy.api.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.testsupport.user.UserHttpTestSupport;
import com.schemafy.api.user.controller.dto.request.LoginRequest;
import com.schemafy.core.ulid.application.service.UlidGenerator;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.UserStatus;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import static com.epages.restdocs.apispec.WebTestClientRestDocumentationWrapper.document;
import static com.schemafy.api.user.docs.UserApiSnippets.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("UserController 통합 테스트")
class UserControllerTest extends UserHttpTestSupport {

  private static final String API_BASE_PATH = ApiPath.API.replace("{version}",
      "v1.0");

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
    User user = createUser("test@example.com", "Test User", "password");

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
    User userA = createUser("userA@example.com", "User A", "password");
    User userB = createUser("userB@example.com", "User B", "password");

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
    User user = createUser("test@example.com", "Test User", "password");

    webTestClient
        .get()
        .uri(API_BASE_PATH + "/users/{userId}", user.id())
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("내 정보 조회에 성공한다")
  void getMyInfoSuccess() {
    User user = createUser("TEST-ME@EXAMPLE.COM", "Test User Me", "password");
    String userId = user.id();
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
    User user = createUser("logout-test@example.com", "Logout User", "password");

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
    User userA = createUser("userA@example.com", "UserA", "password");
    User userB = createUser("userB@example.com", "UserB", "password");

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

  @Test
  @DisplayName("계정 탈퇴에 성공하고 access/refresh 쿠키를 만료시킨다")
  void withdrawSuccess() {
    User user = createUser("withdraw-test@example.com", "Withdraw User");
    String accessToken = generateAccessToken(user.id());
    String refreshToken = generateRefreshToken(user.id());

    webTestClient.delete()
        .uri(API_BASE_PATH + "/users/me")
        .header("Authorization", "Bearer " + accessToken)
        .exchange()
        .expectStatus().isNoContent()
        .expectHeader().values("Set-Cookie", cookies -> {
          Assertions.assertTrue(cookies.stream()
              .anyMatch(cookie -> cookie.contains("accessToken=;")));
          Assertions.assertTrue(cookies.stream()
              .anyMatch(cookie -> cookie.contains("refreshToken=;")));
        })
        .expectBody()
        .consumeWith(document("user-withdraw",
            withdrawRequestHeaders(),
            withdrawResponseHeaders()));

    User withdrawn = getUser(user.id());
    Assertions.assertEquals(UserStatus.INACTIVE, withdrawn.status());
    Assertions.assertNotNull(withdrawn.deletedAt());

    webTestClient.post()
        .uri(ApiPath.PUBLIC_API.replace("{version}", "v1.0") + "/users/refresh")
        .cookie("refreshToken", refreshToken)
        .exchange()
        .expectStatus().isUnauthorized()
        .expectBody()
        .jsonPath("$.reason").isEqualTo("AUTH_INVALID_REFRESH_TOKEN");

    webTestClient.post()
        .uri(ApiPath.PUBLIC_API.replace("{version}", "v1.0") + "/users/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new LoginRequest(user.email(), "password"))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.reason").isEqualTo(UserErrorCode.LOGIN_FAILED.code());
  }

  @Test
  @DisplayName("다른 멤버가 있는 workspace의 유일 ADMIN이면 계정 탈퇴를 원자적으로 차단한다")
  void withdrawBlocksSoleAdminWorkspace() {
    User admin = createUser("withdraw-admin@example.com", "Withdraw Admin");
    User member = createUser("withdraw-member@example.com", "Withdraw Member");
    String workspaceId = nextId();

    databaseClient.sql("""
        INSERT INTO workspaces (id, name, description, created_at, updated_at)
        VALUES (:id, 'Shared workspace', 'description', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """)
        .bind("id", workspaceId)
        .fetch()
        .rowsUpdated()
        .block();
    insertWorkspaceMember(workspaceId, admin.id(), "ADMIN");
    insertWorkspaceMember(workspaceId, member.id(), "MEMBER");

    webTestClient.delete()
        .uri(API_BASE_PATH + "/users/me")
        .header("Authorization", "Bearer " + generateAccessToken(admin.id()))
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("$.reason")
        .isEqualTo("WORKSPACE_LAST_ADMIN_CANNOT_LEAVE")
        .jsonPath("$.workspaceIds")
        .doesNotExist();

    User unchanged = getUser(admin.id());
    Assertions.assertEquals(UserStatus.ACTIVE, unchanged.status());
    Assertions.assertNull(unchanged.deletedAt());
    Long activeMemberships = databaseClient.sql("""
        SELECT COUNT(*) FROM workspace_members
        WHERE workspace_id = :workspaceId AND user_id = :userId AND deleted_at IS NULL
        """)
        .bind("workspaceId", workspaceId)
        .bind("userId", admin.id())
        .map((row, metadata) -> row.get(0, Long.class))
        .one()
        .block();
    Assertions.assertEquals(1L, activeMemberships);
  }

  private void insertWorkspaceMember(String workspaceId, String userId, String role) {
    databaseClient.sql("""
        INSERT INTO workspace_members
            (id, workspace_id, user_id, role, created_at, updated_at)
        VALUES (:id, :workspaceId, :userId, :role, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """)
        .bind("id", nextId())
        .bind("workspaceId", workspaceId)
        .bind("userId", userId)
        .bind("role", role)
        .fetch()
        .rowsUpdated()
        .block();
  }

}
