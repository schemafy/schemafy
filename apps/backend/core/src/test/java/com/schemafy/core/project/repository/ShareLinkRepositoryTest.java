package com.schemafy.core.project.repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.config.R2dbcConfig;
import com.schemafy.core.project.repository.entity.ShareLink;
import com.schemafy.core.project.repository.vo.ShareLinkRole;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(R2dbcConfig.class)
@DisplayName("ShareLinkRepository 테스트")
class ShareLinkRepositoryTest {

  @Autowired
  private ShareLinkRepository shareLinkRepository;

  @Autowired
  private DatabaseClient databaseClient;

  private ShareLink testShareLink;
  private String testProjectId = "project123";
  private byte[] testTokenHash;

  @BeforeEach
  void setUp() {
    shareLinkRepository.deleteAll().block();

    testTokenHash = new byte[32];
    for (int i = 0; i < 32; i++) {
      testTokenHash[i] = (byte) i;
    }

    testShareLink = ShareLink.create(testProjectId, testTokenHash,
        ShareLinkRole.EDITOR,
        Instant.now().plus(7, ChronoUnit.DAYS));
  }

  @Test
  @DisplayName("ShareLink 생성 및 저장에 성공한다")
  void createShareLink() {
    StepVerifier.create(shareLinkRepository.save(testShareLink))
        .assertNext(shareLink -> {
          assertThat(shareLink.getId()).isNotNull();
          assertThat(shareLink.getProjectId())
              .isEqualTo(testProjectId);
          assertThat(shareLink.getTokenHash())
              .isEqualTo(testTokenHash);
          assertThat(shareLink.getRoleAsEnum())
              .isEqualTo(ShareLinkRole.EDITOR);
          assertThat(shareLink.getIsRevoked()).isFalse();
        }).verifyComplete();
  }

  @Test
  @DisplayName("유효한 토큰 해시로 조회에 성공한다")
  void findValidByTokenHash_Success() {
    shareLinkRepository.save(testShareLink).block();

    StepVerifier
        .create(shareLinkRepository.findValidByTokenHash(testTokenHash))
        .assertNext(shareLink -> {
          assertThat(shareLink.getId())
              .isEqualTo(testShareLink.getId());
        }).verifyComplete();
  }

  @Test
  @DisplayName("만료일이 없는(무제한) 토큰 조회 성공한다")
  void findValidByTokenHash_NoExpiration_Success() {
    ShareLink noExpireLink = ShareLink.create(testProjectId, testTokenHash,
        ShareLinkRole.VIEWER, null);
    shareLinkRepository.save(noExpireLink).block();

    StepVerifier
        .create(shareLinkRepository.findValidByTokenHash(testTokenHash))
        .assertNext(shareLink -> {
          assertThat(shareLink.getId())
              .isEqualTo(noExpireLink.getId());
        }).verifyComplete();
  }

  @Test
  @DisplayName("Revoked된 토큰은 조회되지 않는다")
  void findValidByTokenHash_Revoked_Empty() {
    ShareLink saved = shareLinkRepository.save(testShareLink).block();

    saved.revoke();
    shareLinkRepository.save(saved).block();

    StepVerifier
        .create(shareLinkRepository.findValidByTokenHash(testTokenHash))
        .verifyComplete();
  }

  @Test
  @DisplayName("만료된 토큰은 조회되지 않는다")
  void findValidByTokenHash_Expired_Empty() {
    ShareLink saved = shareLinkRepository.save(testShareLink).block();

    // 강제로 만료 시간을 과거로 업데이트
    databaseClient.sql(
        "UPDATE share_links SET expires_at = :expiresAt WHERE id = :id")
        .bind("expiresAt", Instant.now().minusSeconds(3600))
        .bind("id", saved.getId())
        .fetch().rowsUpdated().block();

    StepVerifier
        .create(shareLinkRepository.findValidByTokenHash(testTokenHash))
        .verifyComplete();
  }

  @Test
  @DisplayName("삭제된(Soft Deleted) 토큰은 조회되지 않는다")
  void findValidByTokenHash_Deleted_Empty() {
    ShareLink saved = shareLinkRepository.save(testShareLink).block();

    saved.delete();
    shareLinkRepository.save(saved).block();

    StepVerifier
        .create(shareLinkRepository.findValidByTokenHash(testTokenHash))
        .verifyComplete();
  }

  @Test
  @DisplayName("프로젝트 ID로 목록 조회에 성공한다 (최신순)")
  void findByProjectIdAndNotDeleted() {
    shareLinkRepository.save(testShareLink).block();

    byte[] hash2 = new byte[32];
    hash2[0] = 1;
    ShareLink link2 = ShareLink.create(testProjectId, hash2,
        ShareLinkRole.VIEWER, null);
    shareLinkRepository.save(link2).block();

    StepVerifier
        .create(shareLinkRepository
            .findByProjectIdAndNotDeleted(testProjectId))
        .assertNext(link -> assertThat(link.getTokenHash())
            .isEqualTo(hash2))
        .assertNext(link -> assertThat(link.getTokenHash())
            .isEqualTo(testTokenHash))
        .verifyComplete();
  }

  @Test
  @DisplayName("프로젝트 ID로 Soft Delete에 성공한다")
  void softDeleteByProjectId() {
    shareLinkRepository.save(testShareLink).block();

    StepVerifier
        .create(shareLinkRepository
            .softDeleteByProjectId(testProjectId))
        .verifyComplete();

    StepVerifier
        .create(shareLinkRepository
            .findByIdAndNotDeleted(testShareLink.getId()))
        .verifyComplete();
  }

}
