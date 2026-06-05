package com.schemafy.core.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User 도메인")
class UserTest {

  @ParameterizedTest
  @MethodSource("invalidNames")
  @DisplayName("회원가입 유저 생성 시 이름 정책을 만족해야 한다")
  void signUp_validatesNamePolicy(String name) {
    assertThatThrownBy(() -> User.signUp(
        "user-1",
        "test@example.com",
        name,
        "encoded"))
        .matches(DomainException.hasErrorCode(UserErrorCode.INVALID_PARAMETER));
  }

  @Test
  @DisplayName("회원가입 유저 생성 시 encoded password가 있어야 한다")
  void signUp_validatesEncodedPassword() {
    assertThatThrownBy(() -> User.signUp(
        "user-1",
        "test@example.com",
        "Tester",
        " "))
        .matches(DomainException.hasErrorCode(UserErrorCode.INVALID_PARAMETER));
  }

  @ParameterizedTest
  @MethodSource("invalidNames")
  @DisplayName("OAuth 회원가입 유저 생성 시에도 이름 정책을 만족해야 한다")
  void signUpOAuth_validatesNamePolicy(String name) {
    assertThatThrownBy(() -> User.signUpOAuth(
        "user-1",
        "test@example.com",
        name))
        .matches(DomainException.hasErrorCode(UserErrorCode.INVALID_PARAMETER));
  }

  @Test
  @DisplayName("회원가입 유저 생성 시 정책을 만족하면 통과한다")
  void signUp_success() {
    assertThatCode(() -> User.signUp(
        "user-1",
        "test@example.com",
        "Tester",
        "encoded"))
        .doesNotThrowAnyException();
  }

  static String[] invalidNames() {
    return new String[] { null, "", " ", "\t", "\n", "a".repeat(201) };
  }

}
