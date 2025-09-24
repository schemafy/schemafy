package com.schemafy.core.user.service;

import com.github.f4b6a3.ulid.UlidCreator;
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
        Mono<UserInfoResponse> result = userService.signUp(request.toCommand());

        // then
        StepVerifier.create(result)
                .expectNextMatches(res -> res.email().equals("test@example.com"))
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
        Mono<UserInfoResponse> result = userService.signUp(request.toCommand());

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_ALREADY_EXISTS
                )
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
                    assertThat(res.email()).isEqualTo(user.getEmail()) ;
                    assertThat(res.name()).isEqualTo(user.getName());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 회원은 조회에 실패한다")
    void getUserByIdNotFound() {
        // given
        String id = UlidCreator.getUlid().toString();

        // when
        Mono<UserInfoResponse> result = userService.getUserById(id);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_NOT_FOUND
                )
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
        Mono<UserInfoResponse> result = userService.login(command);

        // then
        StepVerifier.create(result)
                .expectNextMatches(response -> response.email().equals("test@example.com"))
                .verifyComplete();
    }

    @Test
    @DisplayName("로그인 시 존재하지 않는 이메일이면 실패한다")
    void login_fail_email_not_found() {
        // given
        LoginCommand command = new LoginCommand("nonexistent@example.com", "password");

        // when
        Mono<UserInfoResponse> result = userService.login(command);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getErrorCode() == ErrorCode.MEMBER_NOT_FOUND
                )
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
        Mono<UserInfoResponse> result = userService.login(command);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getErrorCode() == ErrorCode.LOGIN_FAILED
                )
                .verify();
    }
}
