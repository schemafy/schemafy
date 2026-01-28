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

  @BeforeEach
  void setUp() {
    userRepository.deleteAll().block();
  }

  @Test
  @DisplayName("회원가입에 성공한다")
  void signupSuccess() {
    SignUpRequest request = new SignUpRequest("test@example.com",
        "Test User", "password");

    Mono<User> result = userService.signUp(request.toCommand());

    StepVerifier.create(result)
        .expectNextMatches(
            user -> user.getEmail().equals("test@example.com"))
        .verifyComplete();

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
  @DisplayName("이메일 중복 시 User가 생성되지 않는다")
  void signUp_NoUserCreated_WhenEmailDuplicate() {
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

    // User가 중복 생성되지 않았는지 검증
    StepVerifier.create(userRepository.findAll().collectList())
        .as("Only one user should exist")
        .assertNext(users -> assertThat(users).hasSize(1))
        .verifyComplete();
  }

  @Nested
  @DisplayName("회원가입 트랜잭션")
  class SignUpTransaction {

    @Test
    @DisplayName("회원가입 성공 시 User가 생성된다")
    void signUp_CreatesUserAtomically() {
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
    }

    @Test
    @DisplayName("동시에 같은 이메일로 가입 시도해도 User는 하나만 생성된다")
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
    }

  }

}
