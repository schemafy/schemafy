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
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.COLUMN;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectDbVendorResolver")
class ProjectDbVendorResolverTest {

  @Mock
  GetProjectIdByAccessResourcePort getProjectIdByAccessResourcePort;

  @Mock
  GetProjectDbVendorUseCase getProjectDbVendorUseCase;

  @InjectMocks
  ProjectDbVendorResolver sut;

  @Test
  @DisplayName("리소스의 정확한 projectId로 선택된 DB vendor를 조회한다")
  void resolvesVendorByExactProjectId() {
    given(getProjectIdByAccessResourcePort.findProjectId(COLUMN, "column-1"))
        .willReturn(Mono.just("project-1"));
    given(getProjectDbVendorUseCase.getProjectDbVendor(
        new GetProjectDbVendorQuery("project-1")))
        .willReturn(Mono.just(DbVendorFixture.defaultDbVendor()));

    StepVerifier.create(sut.resolve(COLUMN, "column-1"))
        .expectNext(DbVendorFixture.defaultDbVendor())
        .verifyComplete();

    then(getProjectDbVendorUseCase).should()
        .getProjectDbVendor(new GetProjectDbVendorQuery("project-1"));
  }

  @Test
  @DisplayName("projectId를 찾지 못하면 기존 NOT_FOUND 오류로 실패한다")
  void failsWhenProjectIdIsMissing() {
    given(getProjectIdByAccessResourcePort.findProjectId(COLUMN, "missing"))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.resolve(COLUMN, "missing"))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.NOT_FOUND))
        .verify();

    then(getProjectDbVendorUseCase).shouldHaveNoInteractions();
  }

}
