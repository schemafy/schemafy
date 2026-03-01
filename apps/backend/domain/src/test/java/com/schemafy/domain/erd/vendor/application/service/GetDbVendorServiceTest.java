package com.schemafy.domain.erd.vendor.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.vendor.application.port.in.GetDbVendorQuery;
import com.schemafy.domain.erd.vendor.application.port.out.GetDbVendorByDisplayNamePort;
import com.schemafy.domain.erd.vendor.domain.exception.VendorErrorCode;
import com.schemafy.domain.erd.vendor.fixture.DbVendorFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetDbVendorService")
class GetDbVendorServiceTest {

  @Mock
  GetDbVendorByDisplayNamePort getDbVendorByDisplayNamePort;

  @InjectMocks
  GetDbVendorService sut;

  @Nested
  @DisplayName("getDbVendor 메서드는")
  class GetDbVendor {

    @Nested
    @DisplayName("벤더가 존재하면")
    class WhenVendorExists {

      @Test
      @DisplayName("벤더를 반환한다")
      void returnsVendor() {
        var query = DbVendorFixture.getQuery();
        var vendor = DbVendorFixture.defaultDbVendor();

        given(getDbVendorByDisplayNamePort.findByDisplayName(any()))
            .willReturn(Mono.just(vendor));

        StepVerifier.create(sut.getDbVendor(query))
            .assertNext(result -> {
              assertThat(result.displayName()).isEqualTo(query.displayName());
              assertThat(result.name()).isEqualTo(DbVendorFixture.DEFAULT_NAME);
            })
            .verifyComplete();

        then(getDbVendorByDisplayNamePort).should().findByDisplayName(query.displayName());
      }

    }

    @Nested
    @DisplayName("벤더가 존재하지 않으면")
    class WhenVendorNotExists {

      @Test
      @DisplayName("DbVendorNotExistException을 발생시킨다")
      void throwsDbVendorNotExistException() {
        var query = new GetDbVendorQuery("NonExistent 1.0");

        given(getDbVendorByDisplayNamePort.findByDisplayName(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.getDbVendor(query))
            .expectErrorMatches(DomainException.hasErrorCode(VendorErrorCode.NOT_FOUND))
            .verify();

        then(getDbVendorByDisplayNamePort).should().findByDisplayName(query.displayName());
      }

    }

  }

}
