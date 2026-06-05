package com.schemafy.core.user.application.port.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SignUpUserCommand")
class SignUpUserCommandTest {

  @Test
  @DisplayName("raw password가 blank이면 INVALID_PARAMETER를 반환한다")
  void rejectsBlankPassword() {
    assertThatThrownBy(() -> new SignUpUserCommand(
        "test@example.com",
        "Tester",
        " "))
        .isInstanceOfSatisfying(DomainException.class,
            error -> assertThat(error.getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_PARAMETER));
  }

  @Test
  @DisplayName("raw password가 8자 미만이면 INVALID_PARAMETER를 반환한다")
  void rejectsShortPassword() {
    assertThatThrownBy(() -> new SignUpUserCommand(
        "test@example.com",
        "Tester",
        "passwrd"))
        .isInstanceOfSatisfying(DomainException.class,
            error -> assertThat(error.getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_PARAMETER));
  }

  @Test
  @DisplayName("raw password가 8자 이상이면 통과한다")
  void acceptsValidPassword() {
    assertThatCode(() -> new SignUpUserCommand(
        "test@example.com",
        "Tester",
        "password"))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("이메일을 변경할 때 raw password 정책을 유지한다")
  void withEmailKeepsPasswordPolicy() {
    SignUpUserCommand command = new SignUpUserCommand(
        "test@example.com",
        "Tester",
        "password");

    SignUpUserCommand changed = command.withEmail("changed@example.com");

    assertThat(changed.email()).isEqualTo("changed@example.com");
    assertThat(changed.name()).isEqualTo("Tester");
    assertThat(changed.password()).isEqualTo("password");
  }

}
