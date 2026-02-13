package com.schemafy.domain.erd.vendor.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.vendor.application.port.out.ListDbVendorSummariesPort;
import com.schemafy.domain.erd.vendor.fixture.DbVendorFixture;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListDbVendorsService")
class ListDbVendorsServiceTest {

  @Mock
  ListDbVendorSummariesPort listDbVendorSummariesPort;

  @InjectMocks
  ListDbVendorsService sut;

  @Nested
  @DisplayName("listDbVendors 메서드는")
  class ListDbVendors {

    @Test
    @DisplayName("벤더 요약 목록을 반환한다")
    void returnsSummaryList() {
      var summary = DbVendorFixture.defaultDbVendorSummary();

      given(listDbVendorSummariesPort.findAllSummaries())
          .willReturn(Flux.just(summary));

      StepVerifier.create(sut.listDbVendors())
          .assertNext(result -> {
            assertThat(result.displayName()).isEqualTo(DbVendorFixture.DEFAULT_DISPLAY_NAME);
            assertThat(result.name()).isEqualTo(DbVendorFixture.DEFAULT_NAME);
          })
          .verifyComplete();

      then(listDbVendorSummariesPort).should().findAllSummaries();
    }

    @Test
    @DisplayName("벤더가 없으면 빈 목록을 반환한다")
    void returnsEmptyWhenNoVendors() {
      given(listDbVendorSummariesPort.findAllSummaries())
          .willReturn(Flux.empty());

      StepVerifier.create(sut.listDbVendors())
          .verifyComplete();

      then(listDbVendorSummariesPort).should().findAllSummaries();
    }

  }

}
