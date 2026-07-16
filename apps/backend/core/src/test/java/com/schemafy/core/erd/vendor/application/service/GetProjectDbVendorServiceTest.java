package com.schemafy.core.erd.vendor.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.out.GetDbVendorByProjectIdPort;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetProjectDbVendorService")
class GetProjectDbVendorServiceTest {

  @Mock
  GetDbVendorByProjectIdPort getDbVendorByProjectIdPort;

  @InjectMocks
  GetProjectDbVendorService sut;

  @Test
  @DisplayName("활성 프로젝트가 선택한 벤더를 반환한다")
  void returnsProjectVendor() {
    var query = new GetProjectDbVendorQuery("project-id");
    var vendor = DbVendorFixture.defaultDbVendor();
    given(getDbVendorByProjectIdPort.findByProjectId(query.projectId()))
        .willReturn(Mono.just(vendor));

    StepVerifier.create(sut.getProjectDbVendor(query))
        .expectNext(vendor)
        .verifyComplete();

    then(getDbVendorByProjectIdPort).should().findByProjectId(query.projectId());
  }

  @Test
  @DisplayName("활성 프로젝트가 없으면 프로젝트 없음 오류를 반환한다")
  void failsWhenActiveProjectDoesNotExist() {
    var query = new GetProjectDbVendorQuery("missing-project-id");
    given(getDbVendorByProjectIdPort.findByProjectId(query.projectId()))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.getProjectDbVendor(query))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.NOT_FOUND))
        .verify();
  }

}
