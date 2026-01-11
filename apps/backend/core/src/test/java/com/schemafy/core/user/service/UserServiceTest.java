package com.schemafy.core.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.TestFixture;
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.ulid.generator.UlidGenerator;
import com.schemafy.core.user.controller.dto.request.SignUpRequest;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.service.dto.LoginCommand;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("UserService 테스트")
class UserServiceTest {

  @Autowired
  UserService userService;

  @Autowired
  UserRepository userRepository;

  @Autowired
  WorkspaceRepository workspaceRepository;

  @Autowired
  WorkspaceMemberRepository workspaceMemberRepository;

  @BeforeEach
  void setUp() {
    workspaceMemberRepository.deleteAll().block();
    workspaceRepository.deleteAll().block();
    userRepository.deleteAll().block();
  }

  @Test
  @DisplayName("회원가입에 성공한다")
  void signupSuccess() {
    SignUpRequest request = new SignUpRequest("test@example.com",
        "Test User", "password");

    Mono<User> result = userService.signUp(request.toCommand());

    // 응답 검증
    StepVerifier.create(result)
        .expectNextMatches(
            user -> user.getEmail().equals("test@example.com"))
        .verifyComplete();

    // db 검증
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

  @Test
  @DisplayName("회원가입 시 개인 워크스페이스가 자동으로 생성된다")
  void signUp_CreatesDefaultWorkspace() {
    // given
    SignUpRequest request = new SignUpRequest("test@example.com",
        "Test User", "password");

    // when
    User user = userService.signUp(request.toCommand()).block();

    // then - 워크스페이스 생성 검증
    StepVerifier.create(
        workspaceRepository.findByUserIdWithPaging(user.getId(), 1, 0))
        .as("default workspace should be created")
        .assertNext(workspace -> {
          assertThat(workspace.getName())
              .isEqualTo("Test User's Workspace");
          assertThat(workspace.getDescription())
              .isEqualTo("Personal workspace for Test User");
          assertThat(workspace.getId()).isNotNull();
          assertThat(workspace.getCreatedAt()).isNotNull();
        })
        .verifyComplete();

    // then - 워크스페이스 멤버 생성 검증 (ADMIN 역할)
    StepVerifier
        .create(workspaceMemberRepository
            .findByUserIdAndNotDeleted(user.getId()))
        .as("user should be added as ADMIN to workspace")
        .assertNext(member -> {
          assertThat(member.getUserId()).isEqualTo(user.getId());
          assertThat(member.getRole())
              .isEqualTo(WorkspaceRole.ADMIN.getValue());
          assertThat(member.isAdmin()).isTrue();
          assertThat(member.getCreatedAt()).isNotNull();
          assertThat(member.getDeletedAt()).isNull();
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("회원가입시 이미 존재하는 이메일이면 실패한다")
  void signUpAlreadyExists() {
    TestFixture.createTestUser("test@example.com", "Test User", "password")
        .flatMap(userRepository::save)
        .block();

    SignUpRequest request = new SignUpRequest("test@example.com",
        "Test User", "password");

    Mono<User> result = userService.signUp(request.toCommand());

    StepVerifier.create(result)
        .expectErrorMatches(
            throwable -> throwable instanceof BusinessException &&
                ((BusinessException) throwable)
                    .getErrorCode() == ErrorCode.USER_ALREADY_EXISTS)
        .verify();
  }

  @Test
  @DisplayName("ID로 회원 조회에 성공한다")
  void getUserByIdSuccess() {
    User user = TestFixture
        .createTestUser("test@example.com", "Test User", "password")
        .flatMap(userRepository::save)
        .block();

    Mono<UserInfoResponse> result = userService.getUserById(user.getId());

    StepVerifier.create(result)
        .assertNext(res -> {
          assertThat(res.id()).isEqualTo(user.getId());
          assertThat(res.email()).isEqualTo(user.getEmail());
          assertThat(res.name()).isEqualTo(user.getName());
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("존재하지 않는 회원은 조회에 실패한다")
  void getUserByIdNotFound() {
    String id = UlidGenerator.generate();

    Mono<UserInfoResponse> result = userService.getUserById(id);

    StepVerifier.create(result)
        .expectErrorMatches(
            throwable -> throwable instanceof BusinessException &&
                ((BusinessException) throwable)
                    .getErrorCode() == ErrorCode.USER_NOT_FOUND)
        .verify();
  }

  @Test
  @DisplayName("로그인에 성공한다")
  void loginSuccess() {
    String rawPassword = "password";
    TestFixture.createTestUser("test@example.com", "Test User", rawPassword)
        .flatMap(userRepository::save)
        .block();

    LoginCommand command = new LoginCommand("test@example.com",
        rawPassword);

    Mono<User> result = userService.login(command);

    StepVerifier.create(result)
        .expectNextMatches(
            user -> user.getEmail().equals("test@example.com"))
        .verifyComplete();
  }

  @Test
  @DisplayName("로그인 시 존재하지 않는 이메일이면 실패한다")
  void login_fail_email_not_found() {
    LoginCommand command = new LoginCommand("nonexistent@example.com",
        "password");

    Mono<User> result = userService.login(command);

    StepVerifier.create(result)
        .expectErrorMatches(
            throwable -> throwable instanceof BusinessException &&
                ((BusinessException) throwable)
                    .getErrorCode() == ErrorCode.USER_NOT_FOUND)
        .verify();
  }

  @Test
  @DisplayName("로그인 시 비밀번호가 틀리면 실패한다")
  void login_fail_password_mismatch() {
    TestFixture.createTestUser("test@example.com", "Test User", "password")
        .flatMap(userRepository::save)
        .block();

    LoginCommand command = new LoginCommand("test@example.com",
        "wrong_password");

    Mono<User> result = userService.login(command);

    StepVerifier.create(result)
        .expectErrorMatches(
            throwable -> throwable instanceof BusinessException &&
                ((BusinessException) throwable)
                    .getErrorCode() == ErrorCode.LOGIN_FAILED)
        .verify();
  }

  @Test
  @DisplayName("이메일 중복 시 Workspace와 Member가 생성되지 않는다")
  void signUp_NoOrphanWorkspace_WhenEmailDuplicate() {
    TestFixture
        .createTestUser("duplicate@example.com", "Existing User",
            "password")
        .flatMap(userRepository::save)
        .block();

    SignUpRequest request = new SignUpRequest("duplicate@example.com",
        "New User", "password");

    Mono<User> result = userService.signUp(request.toCommand());

    StepVerifier.create(result)
        .expectError(BusinessException.class)
        .verify();

    // Workspace가 orphan으로 생성되지 않아야 함
    StepVerifier.create(workspaceRepository.findAll().collectList())
        .as("No orphan workspace should be created")
        .assertNext(workspaces -> assertThat(workspaces).isEmpty())
        .verifyComplete();

    // WorkspaceMember도 orphan으로 생성되지 않아야 함
    StepVerifier
        .create(workspaceMemberRepository.findAll().collectList())
        .as("No orphan workspace member should be created")
        .assertNext(members -> assertThat(members).isEmpty())
        .verifyComplete();
  }

  @Nested
  @DisplayName("회원가입 트랜잭션")
  class SignUpTransaction {

    @Test
    @DisplayName("회원가입 성공 시 User, Workspace, WorkspaceMember가 모두 생성된다")
    void signUp_CreatesAllEntitiesAtomically() {
      SignUpRequest request = new SignUpRequest("atomic@example.com",
          "Atomic User", "password");

      User user = userService.signUp(request.toCommand()).block();

      assertThat(user).isNotNull();
      assertThat(user.getId()).isNotNull();

      StepVerifier
          .create(userRepository.findByEmail("atomic@example.com"))
          .assertNext(u -> assertThat(u.getName())
              .isEqualTo("Atomic User"))
          .verifyComplete();

      StepVerifier.create(
          workspaceRepository
              .findByUserIdWithPaging(user.getId(), 1, 0))
          .assertNext(workspace -> {
            assertThat(workspace.getName())
                .isEqualTo("Atomic User's Workspace");
          })
          .verifyComplete();

      StepVerifier.create(
          workspaceMemberRepository.findByUserIdAndNotDeleted(
              user.getId()))
          .assertNext(member -> {
            assertThat(member.getUserId()).isEqualTo(user.getId());
            assertThat(member.isAdmin()).isTrue();
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("동시에 같은 이메일로 가입 시도해도 orphan이 생성되지 않는다")
    void signUp_ConcurrentDuplicateEmailTest() {
      String email = "concurrent@example.com";
      SignUpRequest request1 = new SignUpRequest(email, "User 1",
          "password");
      SignUpRequest request2 = new SignUpRequest(email, "User 2",
          "password");

      Mono<User> result1 = userService.signUp(request1.toCommand());
      Mono<User> result2 = userService.signUp(request2.toCommand());

      // 하나는 성공, 하나는 실패해야 함
      StepVerifier.create(Mono.zip(
          result1.onErrorResume(e -> Mono.empty()),
          result2.onErrorResume(e -> Mono.empty()))
          .then(Mono.defer(() -> userRepository.findAll()
              .collectList())))
          .assertNext(users -> {
            long matchingUsers = users.stream()
                .filter(u -> email.equals(u.getEmail()))
                .count();
            assertThat(matchingUsers).isEqualTo(1);
          })
          .verifyComplete();

      // Workspace orphan 확인 - 생성된 사용자 수와 동일해야 함
      long userCount = userRepository.findAll()
          .filter(u -> email.equals(u.getEmail()))
          .count().block();
      long workspaceCount = workspaceRepository.findAll()
          .filter(w -> w.getName().contains("User"))
          .count().block();

      assertThat(workspaceCount).isEqualTo(userCount);
    }

  }

}
