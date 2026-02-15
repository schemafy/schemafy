package com.schemafy.domain.erd.vendor.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.config.R2dbcTestConfiguration;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({ DbVendorPersistenceAdapter.class, DbVendorMapper.class, R2dbcTestConfiguration.class })
@DisplayName("DbVendorPersistenceAdapter")
class DbVendorPersistenceAdapterTest {

  @Autowired
  DbVendorPersistenceAdapter sut;

  @Nested
  @DisplayName("findAllSummaries 메서드는")
  class FindAllSummaries {

    @Test
    @DisplayName("시드 데이터의 벤더 요약을 반환한다")
    void returnsSeedVendorSummaries() {
      StepVerifier.create(sut.findAllSummaries())
          .assertNext(summary -> {
            assertThat(summary.displayName()).isEqualTo("MySQL 8.0");
            assertThat(summary.name()).isEqualTo("mysql");
            assertThat(summary.version()).isEqualTo("8.0");
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findByDisplayName 메서드는")
  class FindByDisplayName {

    @Test
    @DisplayName("존재하는 벤더를 반환한다")
    void returnsExistingVendor() {
      StepVerifier.create(sut.findByDisplayName("MySQL 8.0"))
          .assertNext(vendor -> {
            assertThat(vendor.displayName()).isEqualTo("MySQL 8.0");
            assertThat(vendor.name()).isEqualTo("mysql");
            assertThat(vendor.version()).isEqualTo("8.0");
            assertThat(vendor.datatypeMappings()).contains("schemaVersion");
            assertThat(vendor.datatypeMappings()).contains("TINYINT");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 empty를 반환한다")
    void returnsEmptyWhenNotExists() {
      StepVerifier.create(sut.findByDisplayName("NonExistent 1.0"))
          .verifyComplete();
    }

  }

}
