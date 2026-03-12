package com.schemafy.domain.project.domain;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.project.domain.exception.ShareLinkErrorCode;
import com.schemafy.domain.ulid.application.service.UlidGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ShareLink")
class ShareLinkTest {

  private static final String VALID_PROJECT_ID = "01JPROJECT00000000000000001";
  private static final String VALID_CODE = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4";

  @Nested
  @DisplayName("생성 시")
  class Creation {

    @Test
    @DisplayName("기본 만료 정책으로 생성한다")
    void createsWithDefaultExpiry() {
      ShareLink shareLink = ShareLink.create(UlidGenerator.generate(), VALID_PROJECT_ID,
          VALID_CODE);

      assertThat(shareLink.getProjectId()).isEqualTo(VALID_PROJECT_ID);
      assertThat(shareLink.getCode()).isEqualTo(VALID_CODE);
      assertThat(shareLink.getIsRevoked()).isFalse();
      assertThat(shareLink.getExpiresAt()).isAfter(Instant.now());
      assertThat(shareLink.getId()).isNotNull();
    }

    @Test
    @DisplayName("projectId가 null이면 예외가 발생한다")
    void throwsWhenProjectIdIsNull() {
      assertThatThrownBy(() -> ShareLink.create(UlidGenerator.generate(), null, VALID_CODE))
          .isInstanceOf(DomainException.class)
          .satisfies(error -> assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ShareLinkErrorCode.INVALID_PROJECT_ID));
    }

    @Test
    @DisplayName("code가 blank이면 예외가 발생한다")
    void throwsWhenCodeIsBlank() {
      assertThatThrownBy(() -> ShareLink.create(UlidGenerator.generate(), VALID_PROJECT_ID, "  "))
          .isInstanceOf(DomainException.class)
          .satisfies(error -> assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ShareLinkErrorCode.INVALID_LINK));
    }

    @Test
    @DisplayName("커스텀 만료일로 생성한다")
    void createsWithCustomExpiry() {
      Instant expiresAt = Instant.now().plusSeconds(3600);

      ShareLink shareLink = ShareLink.create(UlidGenerator.generate(), VALID_PROJECT_ID,
          VALID_CODE, expiresAt);

      assertThat(shareLink.getExpiresAt()).isEqualTo(expiresAt);
    }

  }

  @Nested
  @DisplayName("상태 메서드는")
  class StatusMethods {

    @Test
    @DisplayName("만료 시점을 기준으로 만료 여부를 판단한다")
    void checksExpiration() {
      ShareLink expired = ShareLink.create(UlidGenerator.generate(), VALID_PROJECT_ID,
          VALID_CODE, Instant.now().minusSeconds(1));
      ShareLink active = ShareLink.create(UlidGenerator.generate(), VALID_PROJECT_ID,
          VALID_CODE, Instant.now().plusSeconds(3600));

      assertThat(expired.isExpired()).isTrue();
      assertThat(active.isExpired()).isFalse();
    }

    @Test
    @DisplayName("활성 여부는 revoke, expiry, delete 상태를 모두 반영한다")
    void checksActiveState() {
      ShareLink shareLink = ShareLink.create(UlidGenerator.generate(), VALID_PROJECT_ID,
          VALID_CODE);

      assertThat(shareLink.isActive()).isTrue();

      shareLink.revoke();
      assertThat(shareLink.isActive()).isFalse();

      ShareLink deleted = ShareLink.create(UlidGenerator.generate(), VALID_PROJECT_ID,
          VALID_CODE);
      deleted.delete();
      assertThat(deleted.isActive()).isFalse();
    }

  }

}
