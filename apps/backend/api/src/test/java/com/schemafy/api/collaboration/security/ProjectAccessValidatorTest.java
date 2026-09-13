package com.schemafy.api.collaboration.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.access.AccessVerifier;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectAccessValidator 단위 테스트")
class ProjectAccessValidatorTest {

  private static final String PROJECT_ID = "project-id";
  private static final String USER_ID = "user-id";

  @Mock
  private WorkspaceMemberPort workspaceMemberPort;

  @Mock
  private ProjectMemberPort projectMemberPort;

  @Mock
  private ProjectPort projectPort;

  @ParameterizedTest
  @EnumSource(ProjectRole.class)
  @DisplayName("활성 VIEWER 이상 멤버는 접근을 허용한다")
  void canAccess_allowsActiveMember(ProjectRole role) {
    ProjectAccessValidator validator = validator();
    givenActiveMember(role);
    givenActiveProject();

    StepVerifier.create(validator.canAccess(PROJECT_ID, USER_ID))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  @DisplayName("비멤버는 접근을 거부한다")
  void canAccess_deniesNonMember() {
    ProjectAccessValidator validator = validator();
    given(projectMemberPort.findByProjectIdAndUserIdAndNotDeleted(
        PROJECT_ID, USER_ID))
        .willReturn(Mono.empty());

    StepVerifier.create(validator.canAccess(PROJECT_ID, USER_ID))
        .expectNext(false)
        .verifyComplete();

    verifyNoInteractions(projectPort);
  }

  @Test
  @DisplayName("삭제된 멤버는 활성 멤버 조회에서 제외되어 접근을 거부한다")
  void canAccess_deniesDeletedMember() {
    ProjectAccessValidator validator = validator();
    given(projectMemberPort.findByProjectIdAndUserIdAndNotDeleted(
        PROJECT_ID, USER_ID))
        .willReturn(Mono.empty());

    StepVerifier.create(validator.canAccess(PROJECT_ID, USER_ID))
        .expectNext(false)
        .verifyComplete();

    verifyNoInteractions(projectPort);
  }

  @Test
  @DisplayName("활성 멤버십이 남아 있어도 존재하지 않는 프로젝트는 접근을 거부한다")
  void canAccess_deniesMissingProject() {
    ProjectAccessValidator validator = validator();
    givenActiveMember(ProjectRole.VIEWER);
    given(projectPort.findByIdAndNotDeleted(PROJECT_ID))
        .willReturn(Mono.empty());

    StepVerifier.create(validator.canAccess(PROJECT_ID, USER_ID))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  @DisplayName("프로젝트 조회 오류는 접근 허용으로 바꾸지 않는다")
  void canAccess_propagatesProjectLookupError() {
    ProjectAccessValidator validator = validator();
    IllegalStateException error = new IllegalStateException("database unavailable");
    givenActiveMember(ProjectRole.VIEWER);
    given(projectPort.findByIdAndNotDeleted(PROJECT_ID))
        .willReturn(Mono.error(error));

    StepVerifier.create(validator.canAccess(PROJECT_ID, USER_ID))
        .expectErrorMatches(actual -> actual == error)
        .verify();
  }

  @Test
  @DisplayName("멤버십 조회 오류는 접근 허용으로 바꾸지 않는다")
  void canAccess_propagatesMemberLookupError() {
    ProjectAccessValidator validator = validator();
    IllegalStateException error = new IllegalStateException("database unavailable");
    given(projectMemberPort.findByProjectIdAndUserIdAndNotDeleted(
        PROJECT_ID, USER_ID))
        .willReturn(Mono.error(error));

    StepVerifier.create(validator.canAccess(PROJECT_ID, USER_ID))
        .expectErrorMatches(actual -> actual == error)
        .verify();

    verifyNoInteractions(projectPort);
  }

  private ProjectAccessValidator validator() {
    return new ProjectAccessValidator(projectPort,
        new AccessVerifier(workspaceMemberPort, projectMemberPort));
  }

  private void givenActiveProject() {
    given(projectPort.findByIdAndNotDeleted(PROJECT_ID))
        .willReturn(Mono.just(Project.create(PROJECT_ID, "workspace-id", 1,
            "Project", "Description")));
  }

  private void givenActiveMember(ProjectRole role) {
    given(projectMemberPort.findByProjectIdAndUserIdAndNotDeleted(
        PROJECT_ID, USER_ID))
        .willReturn(Mono.just(ProjectMember.create("member-id", PROJECT_ID,
            USER_ID, role)));
  }

}
