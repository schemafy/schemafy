package com.schemafy.core.project.repository.entity;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.project.exception.ShareLinkErrorCode;
import com.schemafy.domain.common.exception.DomainException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ShareLink 엔티티 단위 테스트")
class ShareLinkTest {

  private static final String VALID_PROJECT_ID = "01JPROJECT00000000000000001";
  private static final String VALID_CODE = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"; // 32자 UUID (하이픈 제거)

  @Nested
  @DisplayName("create(projectId, code) - 기본 만료 정책 적용")
  class CreateWithDefaultExpiry {

    @Test
    @DisplayName("유효한 projectId와 code로 ShareLink를 생성한다")
    void success() {
      ShareLink shareLink = ShareLink.create(VALID_PROJECT_ID, VALID_CODE);

      assertThat(shareLink.getProjectId()).isEqualTo(VALID_PROJECT_ID);
      assertThat(shareLink.getCode()).isEqualTo(VALID_CODE);
      assertThat(shareLink.getIsRevoked()).isFalse();
      assertThat(shareLink.getExpiresAt()).isAfter(Instant.now());
      assertThat(shareLink.getId()).isNotNull();
    }

    @Test
    @DisplayName("projectId가 null이면 예외가 발생한다")
    void nullProjectId_throwsException() {
      assertThatThrownBy(() -> ShareLink.create(null, VALID_CODE))
          .isInstanceOf(DomainException.class)
          .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
              .isEqualTo(ShareLinkErrorCode.INVALID_PROJECT_ID));
    }

    @Test
    @DisplayName("projectId가 blank이면 예외가 발생한다")
    void blankProjectId_throwsException() {
      assertThatThrownBy(() -> ShareLink.create("  ", VALID_CODE))
          .isInstanceOf(DomainException.class)
          .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
              .isEqualTo(ShareLinkErrorCode.INVALID_PROJECT_ID));
    }

    @Test
    @DisplayName("code가 null이면 예외가 발생한다")
    void nullCode_throwsException() {
      assertThatThrownBy(() -> ShareLink.create(VALID_PROJECT_ID, null))
          .isInstanceOf(DomainException.class)
          .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
              .isEqualTo(ShareLinkErrorCode.INVALID_LINK));
    }

    @Test
    @DisplayName("code가 blank이면 예외가 발생한다")
    void blankCode_throwsException() {
      assertThatThrownBy(() -> ShareLink.create(VALID_PROJECT_ID, "  "))
          .isInstanceOf(DomainException.class)
          .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
              .isEqualTo(ShareLinkErrorCode.INVALID_LINK));
    }

    @Test
    @DisplayName("code 길이 제한이 없으므로 짧은 code도 허용된다")
    void shortCode_succeeds() {
      ShareLink shareLink = ShareLink.create(VALID_PROJECT_ID, "x");

      assertThat(shareLink.getCode()).isEqualTo("x");
    }

  }

  @Nested
  @DisplayName("create(projectId, code, expiresAt) - 커스텀 만료일 지정")
  class CreateWithCustomExpiry {

    @Test
    @DisplayName("유효한 인자로 ShareLink를 생성한다")
    void success() {
      Instant expiresAt = Instant.now().plusSeconds(3600);

      ShareLink shareLink = ShareLink.create(VALID_PROJECT_ID, VALID_CODE, expiresAt);

      assertThat(shareLink.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("expiresAt이 null이면 만료 시간 없이 생성된다")
    void nullExpiresAt_succeeds() {
      ShareLink shareLink = ShareLink.create(VALID_PROJECT_ID, VALID_CODE, null);

      assertThat(shareLink.getExpiresAt()).isNull();
    }

  }

  @Nested
  @DisplayName("isExpired()")
  class IsExpired {

    @Test
    @DisplayName("만료 시간이 지났으면 true를 반환한다")
    void expiredLink_returnsTrue() {
      ShareLink shareLink = ShareLink.create(VALID_PROJECT_ID, VALID_CODE, Instant.now().minusSeconds(1));

      assertThat(shareLink.isExpired()).isTrue();
    }

    @Test
    @DisplayName("만료 시간이 지나지 않았으면 false를 반환한다")
    void validLink_returnsFalse() {
      ShareLink shareLink = ShareLink.create(VALID_PROJECT_ID, VALID_CODE, Instant.now().plusSeconds(3600));

      assertThat(shareLink.isExpired()).isFalse();
    }

    @Test
    @DisplayName("expiresAt이 null이면 false를 반환한다")
    void nullExpiry_returnsFalse() {
      ShareLink shareLink = ShareLink.create(VALID_PROJECT_ID, VALID_CODE, null);

      assertThat(shareLink.isExpired()).isFalse();
    }

  }

  @Nested
  @DisplayName("isActive()")
  class IsActive {

    @Test
    @DisplayName("revoke되지 않고 만료되지 않은 링크는 active다")
    void activeLink_returnsTrue() {
      ShareLink shareLink = ShareLink.create(VALID_PROJECT_ID, VALID_CODE);

      assertThat(shareLink.isActive()).isTrue();
    }

    @Test
    @DisplayName("revoke된 링크는 active가 아니다")
    void revokedLink_returnsFalse() {
      ShareLink shareLink = ShareLink.create(VALID_PROJECT_ID, VALID_CODE);
      shareLink.revoke();

      assertThat(shareLink.isActive()).isFalse();
    }

    @Test
    @DisplayName("만료된 링크는 active가 아니다")
    void expiredLink_returnsFalse() {
      ShareLink shareLink = ShareLink.create(VALID_PROJECT_ID, VALID_CODE, Instant.now().minusSeconds(1));

      assertThat(shareLink.isActive()).isFalse();
    }

    @Test
    @DisplayName("삭제된 링크는 active가 아니다")
    void deletedLink_returnsFalse() {
      ShareLink shareLink = ShareLink.create(VALID_PROJECT_ID, VALID_CODE);
      shareLink.delete();

      assertThat(shareLink.isActive()).isFalse();
    }

  }

  @Nested
  @DisplayName("revoke()")
  class Revoke {

    @Test
    @DisplayName("revoke() 호출 후 isRevoked가 true가 된다")
    void revoke_setsIsRevokedTrue() {
      ShareLink shareLink = ShareLink.create(VALID_PROJECT_ID, VALID_CODE);

      shareLink.revoke();

      assertThat(shareLink.getIsRevoked()).isTrue();
    }

  }

}
