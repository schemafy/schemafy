package com.schemafy.core.project.integration;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.CreateProjectCommand;
import com.schemafy.core.project.application.port.in.CreateProjectUseCase;
import com.schemafy.core.project.application.port.in.LeaveProjectCommand;
import com.schemafy.core.project.application.port.in.LeaveProjectUseCase;
import com.schemafy.core.project.application.port.in.ProjectDetail;
import com.schemafy.core.project.application.port.in.RemoveProjectMemberCommand;
import com.schemafy.core.project.application.port.in.RemoveProjectMemberUseCase;
import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleUseCase;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.user.domain.User;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Project usecase integration")
class ProjectUseCaseIntegrationTest extends ProjectDomainIntegrationSupport {

  @Autowired
  private CreateProjectUseCase createProjectUseCase;

  @Autowired
  private UpdateProjectMemberRoleUseCase updateProjectMemberRoleUseCase;

  @Autowired
  private RemoveProjectMemberUseCase removeProjectMemberUseCase;

  @Autowired
  private LeaveProjectUseCase leaveProjectUseCase;

  @Test
  @DisplayName("createProject adds active workspace members with derived project roles")
  void createProject_propagatesWorkspaceMembers() {
    User creator = signUpUser("creator-project@test.com", "Creator");
    User member = signUpUser("member-project@test.com", "Member");
    User deletedMember = signUpUser("deleted-project@test.com", "Deleted");
    Workspace workspace = saveWorkspace("Project WS", "Description");
    saveWorkspaceMember(workspace, creator, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, member, WorkspaceRole.MEMBER);
    var removedMembership = saveWorkspaceMember(workspace, deletedMember, WorkspaceRole.MEMBER);
    softDeleteWorkspaceMember(removedMembership.getId());

    ProjectDetail detail = createProjectUseCase.createProject(new CreateProjectCommand(
        workspace.getId(),
        "Project A",
        "Description",
        creator.id()))
        .block();

    ProjectMember creatorMember = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(detail.project().getId(), creator.id())
        .block();
    ProjectMember memberProjectMember = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(detail.project().getId(), member.id())
        .block();
    ProjectMember deletedProjectMember = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(detail.project().getId(), deletedMember.id())
        .block();

    assertThat(detail.currentUserRole()).isEqualTo(ProjectRole.ADMIN.name());
    assertThat(creatorMember.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
    assertThat(memberProjectMember.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
    assertThat(deletedProjectMember).isNull();
  }

  @Test
  @DisplayName("updateProjectMemberRole rejects self-modification")
  void updateProjectMemberRole_rejectsSelfModification() {
    User admin = signUpUser("admin-self@test.com", "Admin");
    Workspace workspace = saveWorkspace("Self WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Self Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);

    StepVerifier.create(updateProjectMemberRoleUseCase.updateProjectMemberRole(
        new UpdateProjectMemberRoleCommand(project.getId(), admin.id(), ProjectRole.VIEWER,
            admin.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            ProjectErrorCode.CANNOT_CHANGE_OWN_ROLE))
        .verify();
  }

  @Test
  @DisplayName("updateProjectMemberRole updates another member role")
  void updateProjectMemberRole_updatesTargetMember() {
    User admin = signUpUser("admin-update@test.com", "Admin");
    User viewer = signUpUser("viewer-update@test.com", "Viewer");
    Workspace workspace = saveWorkspace("Update WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, viewer, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Update Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    ProjectMember viewerMember = saveProjectMember(project, viewer, ProjectRole.VIEWER);

    ProjectMember updatedMember = updateProjectMemberRoleUseCase.updateProjectMemberRole(
        new UpdateProjectMemberRoleCommand(project.getId(), viewer.id(), ProjectRole.EDITOR,
            admin.id()))
        .block();

    assertThat(updatedMember.getId()).isEqualTo(viewerMember.getId());
    assertThat(updatedMember.getRoleAsEnum()).isEqualTo(ProjectRole.EDITOR);
  }

  @Test
  @DisplayName("removeProjectMember soft-deletes the target member")
  void removeProjectMember_deletesTargetMember() {
    User admin = signUpUser("admin-remove@test.com", "Admin");
    User viewer = signUpUser("viewer-remove@test.com", "Viewer");
    Workspace workspace = saveWorkspace("Remove WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, viewer, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Remove Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    ProjectMember viewerMember = saveProjectMember(project, viewer, ProjectRole.VIEWER);

    removeProjectMemberUseCase.removeProjectMember(new RemoveProjectMemberCommand(
        project.getId(),
        viewer.id(),
        admin.id())).block();

    ProjectMember deletedMember = projectMemberRepository.findById(viewerMember.getId()).block();
    assertThat(deletedMember).isNotNull();
    assertThat(deletedMember.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("워크스페이스 관리자이기도 한 프로젝트 관리자는 프로젝트에서 제거할 수 없다")
  void removeProjectMember_rejectsDualAdminTarget() {
    User requester = signUpUser("requester-dual-remove@test.com", "Requester");
    User target = signUpUser("target-dual-remove@test.com", "Target");
    Workspace workspace = saveWorkspace("Dual Remove WS", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, target, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Dual Remove Project");
    saveProjectMember(project, requester, ProjectRole.ADMIN);
    saveProjectMember(project, target, ProjectRole.ADMIN);

    StepVerifier.create(removeProjectMemberUseCase.removeProjectMember(
        new RemoveProjectMemberCommand(project.getId(), target.id(), requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            ProjectErrorCode.WORKSPACE_ADMIN_PROJECT_ADMIN_PROTECTED))
        .verify();
  }

  @Test
  @DisplayName("워크스페이스 관리자이기도 한 프로젝트 관리자는 프로젝트에서 등급을 내릴 수 없다")
  void updateProjectMemberRole_rejectsDualAdminDemotion() {
    User requester = signUpUser("requester-dual-demote@test.com", "Requester");
    User target = signUpUser("target-dual-demote@test.com", "Target");
    Workspace workspace = saveWorkspace("Dual Demote WS", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, target, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Dual Demote Project");
    saveProjectMember(project, requester, ProjectRole.ADMIN);
    saveProjectMember(project, target, ProjectRole.ADMIN);

    StepVerifier.create(updateProjectMemberRoleUseCase.updateProjectMemberRole(
        new UpdateProjectMemberRoleCommand(project.getId(), target.id(), ProjectRole.EDITOR,
            requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            ProjectErrorCode.WORKSPACE_ADMIN_PROJECT_ADMIN_PROTECTED))
        .verify();
  }

  @Test
  @DisplayName("프로젝트 등급 불일치가 있어도 워크스페이스 관리자는 프로젝트에서 제거할 수 없다")
  void removeProjectMember_rejectsWorkspaceAdminEvenWhenStoredProjectRoleIsNotAdmin() {
    User requester = signUpUser("requester-project-remove-allowed@test.com", "Requester");
    User target = signUpUser("target-project-remove-allowed@test.com", "Target");
    Workspace workspace = saveWorkspace("Project Remove Allowed WS", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, target, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Project Remove Allowed Project");
    saveProjectMember(project, requester, ProjectRole.ADMIN);
    ProjectMember targetMember = saveProjectMember(project, target, ProjectRole.EDITOR);

    StepVerifier.create(removeProjectMemberUseCase.removeProjectMember(
        new RemoveProjectMemberCommand(project.getId(), target.id(), requester.id())))
        .expectErrorMatches(DomainException.hasErrorCode(
            ProjectErrorCode.WORKSPACE_ADMIN_PROJECT_ADMIN_PROTECTED))
        .verify();

    ProjectMember persistedMember = projectMemberRepository.findById(targetMember.getId()).block();
    assertThat(persistedMember).isNotNull();
    assertThat(persistedMember.isDeleted()).isFalse();
    assertThat(persistedMember.getRoleAsEnum()).isEqualTo(ProjectRole.EDITOR);
  }

  @Test
  @DisplayName("프로젝트 관리자이지만 워크스페이스 관리자가 아니면 프로젝트에서 등급을 내릴 수 있다")
  void updateProjectMemberRole_allowsProjectAdminWithoutWorkspaceAdmin() {
    User requester = signUpUser("requester-project-demote-allowed@test.com", "Requester");
    User target = signUpUser("target-project-demote-allowed@test.com", "Target");
    Workspace workspace = saveWorkspace("Project Demote Allowed WS", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, target, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Project Demote Allowed Project");
    saveProjectMember(project, requester, ProjectRole.ADMIN);
    ProjectMember targetMember = saveProjectMember(project, target, ProjectRole.ADMIN);

    ProjectMember updatedMember = updateProjectMemberRoleUseCase.updateProjectMemberRole(
        new UpdateProjectMemberRoleCommand(project.getId(), target.id(), ProjectRole.EDITOR,
            requester.id()))
        .block();

    assertThat(updatedMember.getId()).isEqualTo(targetMember.getId());
    assertThat(updatedMember.getRoleAsEnum()).isEqualTo(ProjectRole.EDITOR);
  }

  @Test
  @DisplayName("leaveProject deletes the project when the last member leaves")
  void leaveProject_lastMemberDeletesProject() {
    User admin = signUpUser("admin-last@test.com", "Admin");
    Workspace workspace = saveWorkspace("Last Project WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Last Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);

    leaveProjectUseCase.leaveProject(new LeaveProjectCommand(project.getId(), admin.id())).block();

    assertThat(projectRepository.findByIdAndNotDeleted(project.getId()).block()).isNull();
  }

}
