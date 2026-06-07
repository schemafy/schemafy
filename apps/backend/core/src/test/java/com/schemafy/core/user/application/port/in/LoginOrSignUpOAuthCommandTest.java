package com.schemafy.core.user.application.port.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.AuthProvider;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LoginOrSignUpOAuthCommand")
class LoginOrSignUpOAuthCommandTest {

  @Test
  @DisplayName("name 정책을 만족하지 않으면 INVALID_PARAMETER를 반환한다")
  void rejectsInvalidName() {
    assertThatThrownBy(() -> new LoginOrSignUpOAuthCommand(
        "test@example.com",
        " ",
        AuthProvider.GITHUB,
        "github-1"))
        .matches(DomainException.hasErrorCode(UserErrorCode.INVALID_PARAMETER));
  }

  @Test
  @DisplayName("email 정책을 만족하지 않으면 예외가 발생한다")
  void rejectsInvalidEmail() {
    assertThatThrownBy(() -> new LoginOrSignUpOAuthCommand(
        "invalid-email",
        "Tester",
        AuthProvider.GITHUB,
        "github-1"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("email을 정규화해 불변값으로 보관한다")
  void normalizesEmail() {
    LoginOrSignUpOAuthCommand command = new LoginOrSignUpOAuthCommand(
        "TEST@EXAMPLE.COM",
        "Tester",
        AuthProvider.GITHUB,
        "github-1");

    assertThat(command.email()).isEqualTo("test@example.com");
  }

}
