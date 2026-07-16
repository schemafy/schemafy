package com.schemafy.core.erd.vendor.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.out.GetActiveProjectDbVendorIdPort;
import com.schemafy.core.erd.vendor.application.port.out.GetDbVendorByIdPort;
import com.schemafy.core.erd.vendor.domain.exception.VendorErrorCode;
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
  GetActiveProjectDbVendorIdPort getActiveProjectDbVendorIdPort;

  @Mock
  GetDbVendorByIdPort getDbVendorByIdPort;

  @InjectMocks
  GetProjectDbVendorService sut;

  @Test
  @DisplayName("활성 프로젝트가 선택한 벤더를 반환한다")
  void returnsProjectVendor() {
    var query = new GetProjectDbVendorQuery("project-id");
    var vendor = DbVendorFixture.defaultDbVendor();
    given(getActiveProjectDbVendorIdPort.findDbVendorIdByProjectId(query.projectId()))
        .willReturn(Mono.just(DbVendorFixture.DEFAULT_ID));
    given(getDbVendorByIdPort.findActiveById(DbVendorFixture.DEFAULT_ID))
        .willReturn(Mono.just(vendor));

    StepVerifier.create(sut.getProjectDbVendor(query))
        .expectNext(vendor)
        .verifyComplete();

    then(getActiveProjectDbVendorIdPort).should()
        .findDbVendorIdByProjectId(query.projectId());
    then(getDbVendorByIdPort).should()
        .findActiveById(DbVendorFixture.DEFAULT_ID);
  }

  @Test
  @DisplayName("활성 프로젝트가 없으면 프로젝트 없음 오류를 반환한다")
  void failsWhenActiveProjectDoesNotExist() {
    var query = new GetProjectDbVendorQuery("missing-project-id");
    given(getActiveProjectDbVendorIdPort.findDbVendorIdByProjectId(query.projectId()))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.getProjectDbVendor(query))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.NOT_FOUND))
        .verify();

    then(getDbVendorByIdPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("프로젝트가 선택한 활성 벤더가 없으면 벤더 없음 오류를 반환한다")
  void failsWhenActiveVendorDoesNotExist() {
    var query = new GetProjectDbVendorQuery("project-id");
    given(getActiveProjectDbVendorIdPort.findDbVendorIdByProjectId(query.projectId()))
        .willReturn(Mono.just(DbVendorFixture.DEFAULT_ID));
    given(getDbVendorByIdPort.findActiveById(DbVendorFixture.DEFAULT_ID))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.getProjectDbVendor(query))
        .expectErrorMatches(DomainException.hasErrorCode(VendorErrorCode.NOT_FOUND))
        .verify();

    then(getDbVendorByIdPort).should()
        .findActiveById(DbVendorFixture.DEFAULT_ID);
  }

}
