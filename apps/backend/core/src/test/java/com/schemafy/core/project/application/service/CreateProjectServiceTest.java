package com.schemafy.core.project.application.service;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.application.port.out.GetDbVendorByIdPort;
import com.schemafy.core.erd.vendor.domain.exception.VendorErrorCode;
import com.schemafy.core.erd.vendor.fixture.DbVendorFixture;
import com.schemafy.core.project.application.port.in.CreateProjectCommand;
import com.schemafy.core.project.application.port.in.ProjectDetail;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProjectService")
class CreateProjectServiceTest {

  private static final Integer REQUESTED_DB_VENDOR_ID = 999;
  private static final String WORKSPACE_ID = "workspace-id";
  private static final String REQUESTER_ID = "requester-id";

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  GetDbVendorByIdPort getDbVendorByIdPort;

  @Mock
  ProjectPort projectPort;

  @Mock
  ProjectMemberPort projectMemberPort;

  @Mock
  ProjectAccessHelper projectAccessHelper;

  @Mock
  ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @InjectMocks
  CreateProjectService sut;

  @Test
  @DisplayName("조회된 벤더의 canonical ID를 프로젝트에 저장한다")
  void storesCanonicalDbVendorIdReturnedByLookup() {
    var command = new CreateProjectCommand(
        WORKSPACE_ID,
        REQUESTED_DB_VENDOR_ID,
        "Project",
        "Description",
        REQUESTER_ID);
    var dbVendor = DbVendorFixture.defaultDbVendor();

    given(getDbVendorByIdPort.findActiveById(REQUESTED_DB_VENDOR_ID))
        .willReturn(Mono.just(dbVendor));
    given(ulidGeneratorPort.generate())
        .willReturn("project-id", "member-id");
    given(projectPort.save(any(Project.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    given(projectMemberPort.save(any(ProjectMember.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    given(projectMembershipPropagationHelper.propagateWorkspaceMembersToProject(
        "project-id", WORKSPACE_ID, REQUESTER_ID))
        .willReturn(Mono.empty());
    given(projectAccessHelper.buildProjectDetail(any(Project.class), eq(REQUESTER_ID)))
        .willAnswer(invocation -> Mono.just(new ProjectDetail(
            invocation.getArgument(0), ProjectRole.ADMIN.name())));
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    StepVerifier.create(sut.createProject(command))
        .assertNext(detail -> assertThat(detail.project().getDbVendorId())
            .isEqualTo(DbVendorFixture.DEFAULT_ID))
        .verifyComplete();

    then(getDbVendorByIdPort).should().findActiveById(REQUESTED_DB_VENDOR_ID);

    ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
    then(projectPort).should().save(projectCaptor.capture());
    assertThat(projectCaptor.getValue().getDbVendorId()).isEqualTo(DbVendorFixture.DEFAULT_ID);
  }

  @Test
  @DisplayName("활성 벤더가 없으면 프로젝트를 저장하지 않는다")
  void rejectsMissingActiveDbVendor() {
    var command = new CreateProjectCommand(
        WORKSPACE_ID,
        REQUESTED_DB_VENDOR_ID,
        "Project",
        "Description",
        REQUESTER_ID);

    given(getDbVendorByIdPort.findActiveById(REQUESTED_DB_VENDOR_ID))
        .willReturn(Mono.empty());
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    StepVerifier.create(sut.createProject(command))
        .expectErrorMatches(DomainException.hasErrorCode(VendorErrorCode.NOT_FOUND))
        .verify();

    then(ulidGeneratorPort).shouldHaveNoInteractions();
    then(projectPort).shouldHaveNoInteractions();
    then(projectMemberPort).shouldHaveNoInteractions();
  }

}
