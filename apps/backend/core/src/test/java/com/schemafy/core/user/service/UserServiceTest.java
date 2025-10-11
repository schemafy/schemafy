package com.schemafy.core.user.service;

import com.schemafy.core.ulid.generator.UlidGenerator;
import com.schemafy.core.common.TestFixture;
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.user.service.dto.LoginCommand;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.controller.dto.request.SignUpRequest;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
        // given
        SignUpRequest request = new SignUpRequest("test@example.com", "Test User", "password");

        // when
        Mono<User> result = userService.signUp(request.toCommand());

        // then - 응답 검증
        StepVerifier.create(result)
                .expectNextMatches(user -> user.getEmail().equals("test@example.com"))
                .verifyComplete();

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

    @Test
    @DisplayName("회원가입시 이미 존재하는 이메일이면 실패한다")
    void signUpAlreadyExists() {
        // given
        TestFixture.createTestUser("test@example.com", "Test User", "password")
                .flatMap(userRepository::save)
                .block();

        SignUpRequest request = new SignUpRequest("test@example.com", "Test User", "password");

        // when
        Mono<User> result = userService.signUp(request.toCommand());

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getErrorCode() == ErrorCode.USER_ALREADY_EXISTS)
                .verify();
    }

    @Test
    @DisplayName("ID로 회원 조회에 성공한다")
    void getUserByIdSuccess() {
        // given
        User user = TestFixture.createTestUser("test@example.com", "Test User", "password")
                .flatMap(userRepository::save)
                .block();

        // when
        Mono<UserInfoResponse> result = userService.getUserById(user.getId());

        // then
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
        // given
        String id = UlidGenerator.generate();

        // when
        Mono<UserInfoResponse> result = userService.getUserById(id);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getErrorCode() == ErrorCode.USER_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("로그인에 성공한다")
    void loginSuccess() {
        // given
        String rawPassword = "password";
        TestFixture.createTestUser("test@example.com", "Test User", rawPassword)
                .flatMap(userRepository::save)
                .block();

        LoginCommand command = new LoginCommand("test@example.com", rawPassword);

        // when
        Mono<User> result = userService.login(command);

        // then
        StepVerifier.create(result)
                .expectNextMatches(user -> user.getEmail().equals("test@example.com"))
                .verifyComplete();
    }

    @Test
    @DisplayName("로그인 시 존재하지 않는 이메일이면 실패한다")
    void login_fail_email_not_found() {
        // given
        LoginCommand command = new LoginCommand("nonexistent@example.com", "password");

        // when
        Mono<User> result = userService.login(command);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getErrorCode() == ErrorCode.USER_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("로그인 시 비밀번호가 틀리면 실패한다")
    void login_fail_password_mismatch() {
        // given
        TestFixture.createTestUser("test@example.com", "Test User", "password")
                .flatMap(userRepository::save)
                .block();

        LoginCommand command = new LoginCommand("test@example.com", "wrong_password");

        // when
        Mono<User> result = userService.login(command);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getErrorCode() == ErrorCode.LOGIN_FAILED)
                .verify();
    }
}
