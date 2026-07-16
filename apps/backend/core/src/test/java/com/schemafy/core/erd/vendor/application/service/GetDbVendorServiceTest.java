package com.schemafy.core.erd.vendor.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.application.port.in.GetDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.out.GetDbVendorByIdPort;
import com.schemafy.core.erd.vendor.domain.exception.VendorErrorCode;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;

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
  GetDbVendorByIdPort getDbVendorByIdPort;

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

        given(getDbVendorByIdPort.findById(any()))
            .willReturn(Mono.just(vendor));

        StepVerifier.create(sut.getDbVendor(query))
            .assertNext(result -> {
              assertThat(result.id()).isEqualTo(query.id());
              assertThat(result.name()).isEqualTo(DbVendorFixture.DEFAULT_NAME);
            })
            .verifyComplete();

        then(getDbVendorByIdPort).should().findById(query.id());
      }

    }

    @Nested
    @DisplayName("벤더가 존재하지 않으면")
    class WhenVendorNotExists {

      @Test
      @DisplayName("DbVendorNotExistException을 발생시킨다")
      void throwsDbVendorNotExistException() {
        var query = new GetDbVendorQuery(999);

        given(getDbVendorByIdPort.findById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.getDbVendor(query))
            .expectErrorMatches(DomainException.hasErrorCode(VendorErrorCode.NOT_FOUND))
            .verify();

        then(getDbVendorByIdPort).should().findById(query.id());
      }

    }

  }

}
