package com.schemafy.core.project.application.service;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.in.ProjectDetail;
import com.schemafy.core.project.application.port.in.UpdateProjectCommand;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.domain.Project;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

  @Mock
  ProjectMutationGuard projectMutationGuard;

  @InjectMocks
  UpdateProjectService sut;

  @BeforeEach
  void setUp() {
    given(projectMutationGuard.<ProjectDetail>protectProjectMutation(
        any(String.class), any()))
        .willAnswer(invocation -> invocation
            .<Supplier<Mono<ProjectDetail>>>getArgument(1).get());
  }

  @Test
  @DisplayName("프로젝트 행을 수정할 때 배타 변경 보호를 사용한다")
  void updateProjectUsesExclusiveMutationProtection() {
    Project project = Project.create("project-id", "workspace-id", "기존 이름",
        "기존 설명");
    ProjectDetail detail = new ProjectDetail(project, "ADMIN");
    UpdateProjectCommand command = new UpdateProjectCommand(
        project.getId(), "변경 이름", "변경 설명", "requester-id");
    given(projectAccessHelper.findProjectById(project.getId()))
        .willReturn(Mono.just(project));
    given(projectPort.save(project)).willReturn(Mono.just(project));
    given(projectAccessHelper.buildProjectDetail(project, "requester-id"))
        .willReturn(Mono.just(detail));

    StepVerifier.create(sut.updateProject(command))
        .expectNext(detail)
        .verifyComplete();

    then(projectMutationGuard).should()
        .protectProjectMutation(eq(project.getId()), any());
    then(projectMutationGuard).should(never())
        .protectChildCreation(any(), any());
    then(projectMutationGuard).should(never())
        .protectWorkspaceAndProjectMutation(any(), any());
  }

}
