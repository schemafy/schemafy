package com.schemafy.core.project.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectMembershipPropagationHelper")
class ProjectMembershipPropagationHelperTest {

  private static final String WORKSPACE_ID = "workspace-id";
  private static final String USER_ID = "user-id";

  @Mock
  ProjectPort projectPort;

  @Mock
  ProjectMemberPort projectMemberPort;

  @Mock
  WorkspaceMemberPort workspaceMemberPort;

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Test
  @DisplayName("워크스페이스 ADMIN 합류 시 기존 활성 프로젝트 멤버십도 ADMIN으로 올리고 누락된 프로젝트 멤버십을 생성한다")
  void syncProjectMembershipsForWorkspaceRole_upgradesExistingActiveMembershipForWorkspaceAdmin() {
    Project joinedProject = Project.create("project-1", WORKSPACE_ID, "Joined Project",
        "Description");
    Project newProject = Project.create("project-2", WORKSPACE_ID, "New Project", "Description");
    ProjectMember existingMember = ProjectMember.create("member-1", joinedProject.getId(), USER_ID,
        ProjectRole.VIEWER);
    ProjectMembershipPropagationHelper sut = new ProjectMembershipPropagationHelper(
        projectPort,
        projectMemberPort,
        workspaceMemberPort,
        ulidGeneratorPort);

    given(projectPort.findByWorkspaceIdAndNotDeleted(WORKSPACE_ID))
        .willReturn(Flux.just(joinedProject, newProject));
    given(projectMemberPort.findLatestByProjectIdAndUserId(joinedProject.getId(), USER_ID))
        .willReturn(Mono.just(existingMember));
    given(projectMemberPort.findLatestByProjectIdAndUserId(newProject.getId(), USER_ID))
        .willReturn(Mono.empty());
    given(ulidGeneratorPort.generate())
        .willReturn("member-2");
    given(projectMemberPort.save(any(ProjectMember.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(sut.syncProjectMembershipsForWorkspaceRole(WORKSPACE_ID, USER_ID,
        WorkspaceRole.ADMIN))
        .verifyComplete();

    assertThat(existingMember.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);

    ArgumentCaptor<ProjectMember> savedMemberCaptor = ArgumentCaptor.forClass(ProjectMember.class);
    then(projectMemberPort).should(times(2)).save(savedMemberCaptor.capture());
    assertThat(savedMemberCaptor.getAllValues())
        .extracting(ProjectMember::getProjectId, ProjectMember::getUserId, ProjectMember::getRole)
        .containsExactlyInAnyOrder(
            org.assertj.core.groups.Tuple.tuple(joinedProject.getId(), USER_ID,
                ProjectRole.ADMIN.name()),
            org.assertj.core.groups.Tuple.tuple(newProject.getId(), USER_ID,
                ProjectRole.ADMIN.name()));
  }

  @Test
  @DisplayName("워크스페이스 MEMBER 합류 시 기존 활성 프로젝트 역할은 유지한다")
  void syncProjectMembershipsForWorkspaceRole_preservesExistingActiveMembershipForWorkspaceMember() {
    Project project = Project.create("project-1", WORKSPACE_ID, "Project", "Description");
    ProjectMember existingMember = ProjectMember.create("member-1", project.getId(), USER_ID,
        ProjectRole.EDITOR);
    ProjectMembershipPropagationHelper sut = new ProjectMembershipPropagationHelper(
        projectPort,
        projectMemberPort,
        workspaceMemberPort,
        ulidGeneratorPort);

    given(projectPort.findByWorkspaceIdAndNotDeleted(WORKSPACE_ID))
        .willReturn(Flux.just(project));
    given(projectMemberPort.findLatestByProjectIdAndUserId(project.getId(), USER_ID))
        .willReturn(Mono.just(existingMember));

    StepVerifier.create(sut.syncProjectMembershipsForWorkspaceRole(WORKSPACE_ID, USER_ID,
        WorkspaceRole.MEMBER))
        .verifyComplete();

    assertThat(existingMember.getRoleAsEnum()).isEqualTo(ProjectRole.EDITOR);
    then(projectMemberPort).should().findLatestByProjectIdAndUserId(project.getId(), USER_ID);
    then(projectMemberPort).should(never()).save(any(ProjectMember.class));
    then(ulidGeneratorPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("워크스페이스 ADMIN 승격 시 삭제된 프로젝트 멤버십은 복원하고 ADMIN으로 보정한다")
  void syncProjectMembershipsForWorkspaceRole_restoresDeletedMembershipForWorkspaceAdmin() {
    Project project = Project.create("project-1", WORKSPACE_ID, "Project", "Description");
    ProjectMember deletedMember = ProjectMember.create("member-1", project.getId(), USER_ID,
        ProjectRole.VIEWER);
    deletedMember.delete();
    ProjectMembershipPropagationHelper sut = new ProjectMembershipPropagationHelper(
        projectPort,
        projectMemberPort,
        workspaceMemberPort,
        ulidGeneratorPort);

    given(projectPort.findByWorkspaceIdAndNotDeleted(WORKSPACE_ID))
        .willReturn(Flux.just(project));
    given(projectMemberPort.findLatestByProjectIdAndUserId(project.getId(), USER_ID))
        .willReturn(Mono.just(deletedMember));
    given(projectMemberPort.save(deletedMember))
        .willReturn(Mono.just(deletedMember));

    StepVerifier.create(sut.syncProjectMembershipsForWorkspaceRole(WORKSPACE_ID, USER_ID,
        WorkspaceRole.ADMIN))
        .verifyComplete();

    assertThat(deletedMember.isDeleted()).isFalse();
    assertThat(deletedMember.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
    then(projectMemberPort).should().save(deletedMember);
    then(ulidGeneratorPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("워크스페이스 ADMIN을 MEMBER로 강등하면 활성 프로젝트 ADMIN은 VIEWER로 내려간다")
  void updateActiveProjectMembershipRoles_demotesProjectAdminToViewer() {
    ProjectMember projectAdminMember = ProjectMember.create("member-1", "project-1", USER_ID,
        ProjectRole.ADMIN);
    ProjectMembershipPropagationHelper sut = new ProjectMembershipPropagationHelper(
        projectPort,
        projectMemberPort,
        workspaceMemberPort,
        ulidGeneratorPort);

    given(projectMemberPort.findByWorkspaceIdAndUserId(WORKSPACE_ID, USER_ID))
        .willReturn(Flux.just(projectAdminMember));
    given(projectMemberPort.save(projectAdminMember))
        .willReturn(Mono.just(projectAdminMember));

    StepVerifier.create(sut.updateActiveProjectMembershipRoles(WORKSPACE_ID, USER_ID,
        WorkspaceRole.MEMBER))
        .verifyComplete();

    assertThat(projectAdminMember.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
    then(projectMemberPort).should().save(projectAdminMember);
  }

  @Test
  @DisplayName("워크스페이스 ADMIN을 MEMBER로 강등해도 프로젝트 EDITOR 역할은 유지된다")
  void updateActiveProjectMembershipRoles_preservesEditorRoleOnDemotion() {
    ProjectMember projectEditorMember = ProjectMember.create("member-1", "project-1", USER_ID,
        ProjectRole.EDITOR);
    ProjectMembershipPropagationHelper sut = new ProjectMembershipPropagationHelper(
        projectPort,
        projectMemberPort,
        workspaceMemberPort,
        ulidGeneratorPort);

    given(projectMemberPort.findByWorkspaceIdAndUserId(WORKSPACE_ID, USER_ID))
        .willReturn(Flux.just(projectEditorMember));

    StepVerifier.create(sut.updateActiveProjectMembershipRoles(WORKSPACE_ID, USER_ID,
        WorkspaceRole.MEMBER))
        .verifyComplete();

    assertThat(projectEditorMember.getRoleAsEnum()).isEqualTo(ProjectRole.EDITOR);
    then(projectMemberPort).should(never()).save(any(ProjectMember.class));
  }

}
