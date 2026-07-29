package com.schemafy.core.project.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.ProjectDetail;
import com.schemafy.core.project.application.port.in.UpdateProjectCommand;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("프로젝트 수정 서비스 테스트")
class UpdateProjectServiceTest {

  @Mock
  ProjectPort projectPort;

  @Mock
  ProjectAccessHelper projectAccessHelper;

  @InjectMocks
  UpdateProjectService sut;

  @Test
  @DisplayName("활성 프로젝트 행만 수정한다")
  void updateProjectUpdatesOnlyActiveProject() {
    Project project = Project.create("project-id", "workspace-id", "기존 이름",
        "기존 설명");
    ProjectDetail detail = new ProjectDetail(project, "ADMIN");
    UpdateProjectCommand command = new UpdateProjectCommand(
        project.getId(), "변경 이름", "변경 설명", "requester-id");
    given(projectPort.updateIfActive(project.getId(), "변경 이름", "변경 설명"))
        .willReturn(Mono.just(1L));
    given(projectAccessHelper.findProjectById(project.getId()))
        .willReturn(Mono.just(project));
    given(projectAccessHelper.buildProjectDetail(project, "requester-id"))
        .willReturn(Mono.just(detail));

    StepVerifier.create(sut.updateProject(command))
        .expectNext(detail)
        .verifyComplete();

    then(projectPort).should()
        .updateIfActive(project.getId(), "변경 이름", "변경 설명");
    then(projectPort).should(never()).save(project);
  }

  @Test
  @DisplayName("수정 후 삭제 경합으로 멤버십이 사라지면 프로젝트 없음으로 응답한다")
  void updateProjectMapsDeletedMembershipToProjectNotFound() {
    Project project = Project.create("project-id", "workspace-id", "기존 이름",
        "기존 설명");
    UpdateProjectCommand command = new UpdateProjectCommand(
        project.getId(), "변경 이름", "변경 설명", "requester-id");
    given(projectPort.updateIfActive(project.getId(), "변경 이름", "변경 설명"))
        .willReturn(Mono.just(1L));
    given(projectAccessHelper.findProjectById(project.getId()))
        .willReturn(Mono.just(project));
    given(projectAccessHelper.buildProjectDetail(project, "requester-id"))
        .willReturn(Mono.error(new DomainException(ProjectErrorCode.MEMBER_NOT_FOUND)));

    StepVerifier.create(sut.updateProject(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ProjectErrorCode.NOT_FOUND);
        })
        .verify();
  }

}
