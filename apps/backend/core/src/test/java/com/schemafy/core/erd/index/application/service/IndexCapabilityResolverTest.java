package com.schemafy.core.erd.index.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.application.service.ProjectDbVendorResolver;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.INDEX;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndexCapabilityResolver")
class IndexCapabilityResolverTest {

  @Mock
  ProjectDbVendorResolver projectDbVendorResolver;

  @InjectMocks
  IndexCapabilityResolver sut;

  @Test
  @DisplayName("리소스의 projectId로 선택된 벤더 capability를 조회한다")
  void resolvesCapabilitiesByExactProjectId() {
    given(projectDbVendorResolver.resolve(INDEX, "index-1"))
        .willReturn(Mono.just(DbVendorFixture.defaultDbVendor()));

    StepVerifier.create(sut.resolve(INDEX, "index-1"))
        .expectNext(DbVendorFixture.defaultCapabilities().indexes())
        .verifyComplete();

    then(projectDbVendorResolver).should().resolve(INDEX, "index-1");
  }

  @Test
  @DisplayName("리소스의 projectId를 찾지 못하면 벤더를 조회하지 않는다")
  void failsWhenProjectIdIsMissing() {
    given(projectDbVendorResolver.resolve(INDEX, "missing"))
        .willReturn(Mono.error(new DomainException(ProjectErrorCode.NOT_FOUND)));

    StepVerifier.create(sut.resolve(INDEX, "missing"))
        .expectError(DomainException.class)
        .verify();

    then(projectDbVendorResolver).should().resolve(INDEX, "missing");
  }

}
