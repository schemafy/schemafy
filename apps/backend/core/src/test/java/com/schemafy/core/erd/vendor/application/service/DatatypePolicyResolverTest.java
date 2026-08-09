package com.schemafy.core.erd.vendor.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.COLUMN;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatatypePolicyResolver")
class DatatypePolicyResolverTest {

  @Mock
  ProjectDbVendorResolver projectDbVendorResolver;

  @InjectMocks
  DatatypePolicyResolver sut;

  @Test
  @DisplayName("공통 resolver가 반환한 vendor의 datatype policy를 반환한다")
  void resolvesPolicyFromProjectVendor() {
    given(projectDbVendorResolver.resolve(COLUMN, "column-1"))
        .willReturn(Mono.just(DbVendorFixture.defaultDbVendor()));

    StepVerifier.create(sut.resolve(COLUMN, "column-1"))
        .expectNext(DbVendorFixture.defaultDatatypePolicy())
        .verifyComplete();

    then(projectDbVendorResolver).should().resolve(COLUMN, "column-1");
  }

}
