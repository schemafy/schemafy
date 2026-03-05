package com.schemafy.domain.user.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.user.application.port.in.LoginUserCommand;
import com.schemafy.domain.user.application.port.out.FindUserByEmailPort;
import com.schemafy.domain.user.application.port.out.PasswordHashPort;
import com.schemafy.domain.user.domain.User;
import com.schemafy.domain.user.domain.UserStatus;
import com.schemafy.domain.user.domain.exception.UserErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUserService")
class LoginUserServiceTest {

  @Mock
  FindUserByEmailPort findUserByEmailPort;

  @Mock
  PasswordHashPort passwordHashPort;

  @InjectMocks
  LoginUserService sut;

  @Test
  @DisplayName("loginUser: 이메일/비밀번호가 맞으면 로그인 성공")
  void loginUser_success() {
    LoginUserCommand command = new LoginUserCommand("test@example.com", "raw");
    User user = new User(
        "user-1",
        "test@example.com",
        "Tester",
        "encoded",
        UserStatus.ACTIVE,
        null,
        null,
        null);

    given(findUserByEmailPort.findUserByEmail("test@example.com"))
        .willReturn(Mono.just(user));
    given(passwordHashPort.matches("raw", "encoded"))
        .willReturn(Mono.just(true));

    StepVerifier.create(sut.loginUser(command))
        .assertNext(found -> assertThat(found.id()).isEqualTo("user-1"))
        .verifyComplete();
  }

  @Test
  @DisplayName("loginUser: 이메일이 없으면 NOT_FOUND")
  void loginUser_notFound() {
    LoginUserCommand command = new LoginUserCommand("none@example.com", "raw");

    given(findUserByEmailPort.findUserByEmail("none@example.com"))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.loginUser(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.NOT_FOUND);
        })
        .verify();
  }

  @Test
  @DisplayName("loginUser: 비밀번호가 틀리면 LOGIN_FAILED")
  void loginUser_passwordMismatch() {
    LoginUserCommand command = new LoginUserCommand("test@example.com", "raw");
    User user = new User(
        "user-1",
        "test@example.com",
        "Tester",
        "encoded",
        UserStatus.ACTIVE,
        null,
        null,
        null);

    given(findUserByEmailPort.findUserByEmail("test@example.com"))
        .willReturn(Mono.just(user));
    given(passwordHashPort.matches("raw", "encoded"))
        .willReturn(Mono.just(false));

    StepVerifier.create(sut.loginUser(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(UserErrorCode.LOGIN_FAILED);
        })
        .verify();
  }

}
