package com.schemafy.core.user.application.port.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.AuthProvider;
import com.schemafy.core.user.domain.UserPolicy;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LoginOrSignUpOAuthCommand")
class LoginOrSignUpOAuthCommandTest {

  @Test
  @DisplayName("OAuth name이 비어 있으면 provider 정보로 대체한다")
  void replacesBlankNameWithProviderName() {
    LoginOrSignUpOAuthCommand command = new LoginOrSignUpOAuthCommand(
        "test@example.com",
        " ",
        AuthProvider.GITHUB,
        "1");

    assertThat(command.name()).isEqualTo("github-1");
  }

  @Test
  @DisplayName("OAuth name 대체가 불가능하면 INVALID_PARAMETER를 반환한다")
  void rejectsInvalidNameWhenFallbackUnavailable() {
    assertThatThrownBy(() -> new LoginOrSignUpOAuthCommand(
        "test@example.com",
        " ",
        AuthProvider.GITHUB,
        " "))
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

  @Test
  @DisplayName("OAuth name이 정책 길이를 초과하면 최대 길이로 자른다")
  void truncatesLongName() {
    LoginOrSignUpOAuthCommand command = new LoginOrSignUpOAuthCommand(
        "test@example.com",
        "a".repeat(UserPolicy.MAX_NAME_LENGTH + 1),
        AuthProvider.GITHUB,
        "github-1");

    assertThat(command.name()).hasSize(UserPolicy.MAX_NAME_LENGTH);
  }

}
