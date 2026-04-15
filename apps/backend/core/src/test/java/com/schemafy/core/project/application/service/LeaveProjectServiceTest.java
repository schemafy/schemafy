package com.schemafy.core.project.application.service;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.in.LeaveProjectCommand;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveProjectService")
class LeaveProjectServiceTest {

  private static final String PROJECT_ID = "project-id";
  private static final String WORKSPACE_ID = "workspace-id";
  private static final String USER_ID = "user-id";

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  ProjectMemberPort projectMemberPort;

  @Mock
  ProjectPort projectPort;

  @Mock
  ProjectAccessHelper projectAccessHelper;

  @Mock
  ProjectCascadeHelper projectCascadeHelper;

  @InjectMocks
  LeaveProjectService sut;

  @Test
  @DisplayName("마지막 멤버이고 프로젝트가 존재하면 cascade만 수행하고 fallback 멤버 삭제는 호출하지 않는다")
  void leaveProject_lastMemberSkipsFallbackWhenProjectExists() {
    LeaveProjectCommand command = new LeaveProjectCommand(PROJECT_ID, USER_ID);
    ProjectMember member = ProjectMember.create("member-id", PROJECT_ID,
        USER_ID, ProjectRole.VIEWER);
    Project project = Project.create(PROJECT_ID, WORKSPACE_ID, "Project", "Description");

    given(projectPort.findById(PROJECT_ID))
        .willReturn(Mono.just(project));
    given(projectAccessHelper
        .lockProjectWithinWorkspaceForWrite(WORKSPACE_ID, PROJECT_ID))
        .willReturn(Mono.just(project));
    given(projectAccessHelper.findProjectMember(USER_ID, PROJECT_ID))
        .willReturn(Mono.just(member));
    given(projectAccessHelper.validateWorkspaceAdminGuard(PROJECT_ID, member))
        .willReturn(Mono.empty());
    given(projectMemberPort.countByProjectIdAndNotDeleted(PROJECT_ID))
        .willReturn(Mono.just(1L));
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
  @DisplayName("프로젝트가 이미 삭제되었으면 fallback 멤버 삭제만 수행한다")
  void leaveProject_fallsBackWhenProjectAlreadyDeleted() {
    LeaveProjectCommand command = new LeaveProjectCommand(PROJECT_ID, USER_ID);
    ProjectMember member = ProjectMember.create("member-id", PROJECT_ID,
        USER_ID, ProjectRole.VIEWER);
    Project project = Project.create(PROJECT_ID, WORKSPACE_ID, "Project",
        "Description");
    project.delete();

    given(projectPort.findById(PROJECT_ID))
        .willReturn(Mono.just(project));
    given(projectAccessHelper.findProjectMember(USER_ID, PROJECT_ID))
        .willReturn(Mono.just(member));
    given(projectAccessHelper.softDeleteMember(member))
        .willReturn(Mono.empty());
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    StepVerifier.create(sut.leaveProject(command))
        .verifyComplete();

    then(projectAccessHelper).should()
        .softDeleteMember(member);
    then(projectAccessHelper).should(never())
        .lockProjectWithinWorkspaceForWrite(WORKSPACE_ID, PROJECT_ID);
    then(projectCascadeHelper).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("마지막 멤버이지만 프로젝트가 없으면 fallback 멤버 삭제를 수행한다")
  void leaveProject_lastMemberFallsBackWhenProjectMissing() {
    LeaveProjectCommand command = new LeaveProjectCommand(PROJECT_ID, USER_ID);
    ProjectMember member = ProjectMember.create("member-id", PROJECT_ID,
        USER_ID, ProjectRole.VIEWER);

    given(projectPort.findById(PROJECT_ID))
        .willReturn(Mono.empty());
    given(projectAccessHelper.findProjectMember(USER_ID, PROJECT_ID))
        .willReturn(Mono.just(member));
    given(projectAccessHelper.softDeleteMember(member))
        .willReturn(Mono.empty());
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    StepVerifier.create(sut.leaveProject(command))
        .verifyComplete();

    then(projectAccessHelper).should()
        .softDeleteMember(member);
    then(projectCascadeHelper).shouldHaveNoInteractions();
  }

}
