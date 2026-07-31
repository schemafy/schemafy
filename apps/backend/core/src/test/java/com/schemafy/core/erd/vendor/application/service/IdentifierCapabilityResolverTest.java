package com.schemafy.core.erd.vendor.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorUseCase;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;
import com.schemafy.core.project.application.access.GetProjectIdByAccessResourcePort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.TABLE;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdentifierCapabilityResolver")
class IdentifierCapabilityResolverTest {

  @Mock
  GetProjectIdByAccessResourcePort getProjectIdByAccessResourcePort;

  @Mock
  GetProjectDbVendorUseCase getProjectDbVendorUseCase;

  @InjectMocks
  IdentifierCapabilityResolver sut;

  @Test
  @DisplayName("리소스의 projectId로 선택된 벤더 identifier capability를 조회한다")
  void resolvesCapabilitiesByExactProjectId() {
    given(getProjectIdByAccessResourcePort.findProjectId(TABLE, "table-1"))
        .willReturn(Mono.just("project-1"));
    given(getProjectDbVendorUseCase.getProjectDbVendor(
        new GetProjectDbVendorQuery("project-1")))
        .willReturn(Mono.just(DbVendorFixture.defaultDbVendor()));

    StepVerifier.create(sut.resolve(TABLE, "table-1"))
        .expectNext(DbVendorFixture.defaultCapabilities().identifiers())
        .verifyComplete();

    then(getProjectDbVendorUseCase).should()
        .getProjectDbVendor(new GetProjectDbVendorQuery("project-1"));
  }

  @Test
  @DisplayName("리소스의 projectId를 찾지 못하면 벤더를 조회하지 않는다")
  void failsWhenProjectIdIsMissing() {
    given(getProjectIdByAccessResourcePort.findProjectId(TABLE, "missing"))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.resolve(TABLE, "missing"))
        .expectError(DomainException.class)
        .verify();

    then(getProjectDbVendorUseCase).shouldHaveNoInteractions();
  }

}
