package com.schemafy.core.project.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.in.LeaveProjectCommand;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("프로젝트 탈퇴 서비스 테스트")
class LeaveProjectServiceTest {

  private static final String PROJECT_ID = "project-id";
  private static final String WORKSPACE_ID = "workspace-id";
  private static final Integer DB_VENDOR_ID = 1;
  private static final String USER_ID = "user-id";

  @Mock
  ProjectAccessHelper projectAccessHelper;

  @InjectMocks
  LeaveProjectService sut;

  @Test
  @DisplayName("마지막 멤버이고 프로젝트가 존재하면 cascade만 수행하고 fallback 멤버 삭제는 호출하지 않는다")
  void leaveProject_lastMemberSkipsFallbackWhenProjectExists() {
    LeaveProjectCommand command = new LeaveProjectCommand(PROJECT_ID, USER_ID);
    ProjectMember member = ProjectMember.create("member-id", PROJECT_ID, USER_ID, ProjectRole.VIEWER);
    Project project = Project.create(PROJECT_ID, WORKSPACE_ID, DB_VENDOR_ID, "Project",
        "Description");

    given(projectAccessHelper.findProjectMember(USER_ID, PROJECT_ID))
        .willReturn(Mono.just(member));
    given(projectAccessHelper.validateWorkspaceAdminGuard(PROJECT_ID, member))
        .willReturn(Mono.empty());
    given(projectMemberPort.countByProjectIdAndNotDeleted(PROJECT_ID))
        .willReturn(Mono.just(1L));
    given(projectPort.findById(PROJECT_ID))
        .willReturn(Mono.just(project));
    given(projectCascadeHelper.softDeleteProjectCascade(project))
        .willReturn(Mono.empty());
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    StepVerifier.create(sut.leaveProject(command))
        .verifyComplete();

    then(projectCascadeHelper).should()
        .softDeleteProjectCascade(project);
    then(projectAccessHelper).should(never())
        .softDeleteMember(member);
  }

  @Test
  @DisplayName("마지막 멤버이지만 프로젝트가 없으면 fallback 멤버 삭제를 수행한다")
  void leaveProject_lastMemberFallsBackWhenProjectMissing() {
    LeaveProjectCommand command = new LeaveProjectCommand(PROJECT_ID, USER_ID);
    ProjectMember member = ProjectMember.create("member-id", PROJECT_ID, USER_ID, ProjectRole.VIEWER);

    given(projectAccessHelper.findProjectMember(USER_ID, PROJECT_ID))
        .willReturn(Mono.just(member));
    given(projectAccessHelper.validateWorkspaceAdminGuard(PROJECT_ID, member))
        .willReturn(Mono.empty());
    given(projectAccessHelper.softDeleteMember(member))
        .willReturn(Mono.empty());
    StepVerifier.create(sut.leaveProject(command))
        .verifyComplete();

    then(projectAccessHelper).should()
        .softDeleteMember(member);
  }

}
