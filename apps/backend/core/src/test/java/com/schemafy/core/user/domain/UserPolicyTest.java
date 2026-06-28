package com.schemafy.core.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserPolicy")
class UserPolicyTest {

  @Test
  @DisplayName("name 정책을 검증한다")
  void validatesName() {
    assertThat(UserPolicy.isValidName("Tester")).isTrue();
    assertThat(UserPolicy.isValidName("a".repeat(UserPolicy.MAX_NAME_LENGTH))).isTrue();
    assertThat(UserPolicy.isValidName(null)).isFalse();
    assertThat(UserPolicy.isValidName(" ")).isFalse();
    assertThat(UserPolicy.isValidName("a".repeat(UserPolicy.MAX_NAME_LENGTH + 1))).isFalse();
  }

  @Test
  @DisplayName("name 정책을 만족하지 않으면 INVALID_PARAMETER를 반환한다")
  void validateNameRejectsInvalidName() {
    assertThatThrownBy(() -> UserPolicy.validateName(" "))
        .matches(DomainException.hasErrorCode(UserErrorCode.INVALID_PARAMETER));
  }

  @Test
  @DisplayName("name을 정책 최대 길이로 자른다")
  void truncatesName() {
    assertThat(UserPolicy.truncateName(null)).isNull();
    assertThat(UserPolicy.truncateName("Tester")).isEqualTo("Tester");
    assertThat(UserPolicy.truncateName("a".repeat(UserPolicy.MAX_NAME_LENGTH + 1)))
        .hasSize(UserPolicy.MAX_NAME_LENGTH);
  }

}
