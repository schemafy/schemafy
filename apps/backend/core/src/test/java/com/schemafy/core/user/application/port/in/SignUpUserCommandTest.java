package com.schemafy.core.user.application.port.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SignUpUserCommand")
class SignUpUserCommandTest {

  @Test
  @DisplayName("raw password 정책을 만족하지 않으면 INVALID_PARAMETER를 반환한다")
  void rejectsInvalidPassword() {
    assertThatThrownBy(() -> new SignUpUserCommand(
        "test@example.com",
        "Tester",
        "passwrd",
        "signup-token"))
        .matches(DomainException.hasErrorCode(UserErrorCode.INVALID_PARAMETER));
  }

  @Test
  @DisplayName("name 정책을 만족하지 않으면 INVALID_PARAMETER를 반환한다")
  void rejectsInvalidName() {
    assertThatThrownBy(() -> new SignUpUserCommand(
        "test@example.com",
        " ",
        "password",
        "signup-token"))
        .matches(DomainException.hasErrorCode(UserErrorCode.INVALID_PARAMETER));
  }

  @Test
  @DisplayName("email 정책을 만족하지 않으면 예외가 발생한다")
  void rejectsInvalidEmail() {
    assertThatThrownBy(() -> new SignUpUserCommand(
        "invalid-email",
        "Tester",
        "password",
        "signup-token"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("signup verification token이 없어도 command를 생성한다")
  void acceptsMissingSignupVerificationToken() {
    SignUpUserCommand command = new SignUpUserCommand(
        "test@example.com",
        "Tester",
        "password",
        null);

    assertThat(command.signupVerificationToken()).isNull();
  }

  @Test
  @DisplayName("email을 정규화해 불변값으로 보관한다")
  void normalizesEmail() {
    SignUpUserCommand command = new SignUpUserCommand(
        "TEST@EXAMPLE.COM",
        "Tester",
        "password",
        "signup-token");

    assertThat(command.email()).isEqualTo("test@example.com");
  }

}
