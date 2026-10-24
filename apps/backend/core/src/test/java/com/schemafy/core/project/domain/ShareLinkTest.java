package com.schemafy.core.project.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.domain.exception.ShareLinkErrorCode;
import com.schemafy.core.ulid.application.service.UlidGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ShareLink")
class ShareLinkTest {

  private static final String VALID_PROJECT_ID = "01JPROJECT00000000000000001";
  private static final String VALID_CODE = "0123456789abcdef0123456789abcdef";

  @Nested
  @DisplayName("생성 시")
  class Creation {

    @Test
    @DisplayName("활성 상태로 생성한다")
    void createsActiveLink() {
      ShareLink shareLink = ShareLink.create(
          UlidGenerator.generate(), VALID_PROJECT_ID, VALID_CODE);

      assertThat(shareLink.getProjectId()).isEqualTo(VALID_PROJECT_ID);
      assertThat(shareLink.getCode()).matches("[0-9a-f]{32}");
      assertThat(shareLink.isActive()).isTrue();
      assertThat(shareLink.getId()).isNotNull();
    }

    @Test
    @DisplayName("projectId가 null이면 예외가 발생한다")
    void throwsWhenProjectIdIsNull() {
      assertThatThrownBy(() -> ShareLink.create(
          UlidGenerator.generate(), null, VALID_CODE))
          .isInstanceOf(DomainException.class)
          .satisfies(error -> assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ShareLinkErrorCode.INVALID_PROJECT_ID));
    }

    @Test
    @DisplayName("code가 null이면 예외가 발생한다")
    void throwsWhenCodeIsNull() {
      assertThatThrownBy(() -> ShareLink.create(
          UlidGenerator.generate(), VALID_PROJECT_ID, null))
          .isInstanceOf(DomainException.class)
          .satisfies(error -> assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ShareLinkErrorCode.INVALID_LINK));
    }

    @Test
    @DisplayName("code가 비어 있으면 예외가 발생한다")
    void throwsWhenCodeIsBlank() {
      assertThatThrownBy(() -> ShareLink.create(
          UlidGenerator.generate(), VALID_PROJECT_ID, " "))
          .isInstanceOf(DomainException.class)
          .satisfies(error -> assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ShareLinkErrorCode.INVALID_LINK));
    }

    @Test
    @DisplayName("code 형식이 32자리 소문자 hexadecimal이 아니면 예외가 발생한다")
    void throwsWhenCodeFormatIsInvalid() {
      assertThatThrownBy(() -> ShareLink.create(
          UlidGenerator.generate(), VALID_PROJECT_ID, "not-a-share-link-code"))
          .isInstanceOf(DomainException.class)
          .satisfies(error -> assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ShareLinkErrorCode.INVALID_LINK));
    }

  }

  @Nested
  @DisplayName("상태 메서드는")
  class StatusMethods {

    @Test
    @DisplayName("활성 여부는 상태 및 삭제 여부를 반영한다")
    void checksActiveState() {
      ShareLink shareLink = ShareLink.create(
          UlidGenerator.generate(), VALID_PROJECT_ID, VALID_CODE);

      assertThat(shareLink.isActive()).isTrue();

      shareLink.deactivate();
      assertThat(shareLink.isActive()).isFalse();

      ShareLink deleted = ShareLink.create(
          UlidGenerator.generate(), VALID_PROJECT_ID, VALID_CODE);
      deleted.delete();
      assertThat(deleted.isActive()).isFalse();
    }

  }

}
