package com.schemafy.core.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.project.controller.dto.request.UpdateProjectMemberRoleRequest;
import com.schemafy.core.project.controller.dto.response.ProjectMemberResponse;
import com.schemafy.core.project.repository.*;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.project.repository.vo.ProjectSettings;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("ProjectService 프로젝트 멤버 관리 테스트")
class ProjectServiceTest {

  @Autowired
  private ProjectService projectService;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private ProjectMemberRepository projectMemberRepository;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Autowired
  private WorkspaceMemberRepository workspaceMemberRepository;

  @Autowired
  private UserRepository userRepository;

  private User ownerUser;
  private User adminUser;
  private User viewerUser;
  private User outsiderUser;
  private Workspace testWorkspace;
  private Project testProject;
  private ProjectMember ownerMember;
  private ProjectMember adminMember;
  private ProjectMember viewerMember;

  @BeforeEach
  void setUp() {
    Mono.when(
        projectMemberRepository.deleteAll(),
        projectRepository.deleteAll(),
        workspaceMemberRepository.deleteAll(),
        workspaceRepository.deleteAll(),
        userRepository.deleteAll()).block();

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    ownerUser = User.signUp(
        new UserInfo("owner@test.com", "Owner User", "password"),
        encoder).flatMap(userRepository::save).block();

    adminUser = User.signUp(
        new UserInfo("admin@test.com", "Admin User", "password"),
        encoder).flatMap(userRepository::save).block();

    viewerUser = User.signUp(
        new UserInfo("viewer@test.com", "Viewer User", "password"),
        encoder).flatMap(userRepository::save).block();

    outsiderUser = User.signUp(
        new UserInfo("outsider@test.com", "Outsider User", "password"),
        encoder).flatMap(userRepository::save).block();

    testWorkspace = Workspace.create(
        "Test Workspace",
        "Test Description");
    testWorkspace = workspaceRepository.save(testWorkspace).block();

    workspaceMemberRepository.save(
        WorkspaceMember.create(testWorkspace.getId(), ownerUser.getId(),
            WorkspaceRole.ADMIN))
        .block();
    workspaceMemberRepository.save(
        WorkspaceMember.create(testWorkspace.getId(), adminUser.getId(),
            WorkspaceRole.MEMBER))
        .block();
    workspaceMemberRepository.save(
        WorkspaceMember.create(testWorkspace.getId(),
            viewerUser.getId(), WorkspaceRole.MEMBER))
        .block();

    testProject = Project.create(
        testWorkspace.getId(),
        "Test Project",
        "Test Description",
        ProjectSettings.defaultSettings());
    testProject = projectRepository.save(testProject).block();

    ownerMember = ProjectMember.create(testProject.getId(),
        ownerUser.getId(), ProjectRole.OWNER);
    ownerMember = projectMemberRepository.save(ownerMember).block();

    adminMember = ProjectMember.create(testProject.getId(),
        adminUser.getId(), ProjectRole.ADMIN);
    adminMember = projectMemberRepository.save(adminMember).block();

    viewerMember = ProjectMember.create(testProject.getId(),
        viewerUser.getId(), ProjectRole.VIEWER);
    viewerMember = projectMemberRepository.save(viewerMember).block();
  }

  @Nested
  @DisplayName("프로젝트 멤버 역할 변경")
  class UpdateMemberRole {

    @Test
    @DisplayName("자신의 권한은 변경할 수 없다")
    void selfModification_rejected() {
      UpdateProjectMemberRoleRequest request = new UpdateProjectMemberRoleRequest(
          ProjectRole.VIEWER);

      StepVerifier.create(
          projectService.updateMemberRole(
              testWorkspace.getId(),
              testProject.getId(),
              adminMember.getId(),
              request,
              adminUser.getId()))
          .expectErrorMatches(e -> e instanceof BusinessException &&
              ((BusinessException) e)
                  .getErrorCode() == ErrorCode.CANNOT_CHANGE_OWN_ROLE)
          .verify();
    }

    @Test
    @DisplayName("마지막 OWNER/ADMIN의 권한 변경은 방지된다")
    void lastOwnerOrAdminDowngrade_prevented() {
      User secondAdmin = User.signUp(
          new UserInfo("admin2@test.com", "Second Admin", "password"),
          new BCryptPasswordEncoder()).flatMap(userRepository::save)
          .block();

      workspaceMemberRepository.save(
          WorkspaceMember.create(testWorkspace.getId(),
              secondAdmin.getId(), WorkspaceRole.MEMBER))
          .block();

      ProjectMember secondAdminMember = ProjectMember.create(
          testProject.getId(), secondAdmin.getId(),
          ProjectRole.ADMIN);
      secondAdminMember = projectMemberRepository.save(secondAdminMember)
          .block();

      projectMemberRepository.deleteById(secondAdminMember.getId())
          .block();
      projectMemberRepository.deleteById(adminMember.getId()).block();
      projectMemberRepository.deleteById(viewerMember.getId()).block();

      UpdateProjectMemberRoleRequest request = new UpdateProjectMemberRoleRequest(
          ProjectRole.VIEWER);

      StepVerifier.create(
          projectService.updateMemberRole(
              testWorkspace.getId(),
              testProject.getId(),
              ownerMember.getId(),
              request,
              ownerUser.getId()))
          .expectErrorMatches(e -> e instanceof BusinessException &&
              ((BusinessException) e)
                  .getErrorCode() == ErrorCode.CANNOT_CHANGE_OWN_ROLE)
          .verify();
    }

    @Test
    @DisplayName("유효한 권한 변경은 성공한다")
    void validRoleChange_success() {
      UpdateProjectMemberRoleRequest request = new UpdateProjectMemberRoleRequest(
          ProjectRole.EDITOR);

      Mono<ProjectMemberResponse> result = projectService
          .updateMemberRole(
              testWorkspace.getId(),
              testProject.getId(),
              viewerMember.getId(),
              request,
              adminUser.getId());

      StepVerifier.create(result)
          .assertNext(response -> {
            assertThat(response.userId())
                .isEqualTo(viewerUser.getId());
            assertThat(response.role())
                .isEqualTo(ProjectRole.EDITOR.getValue());
          })
          .verifyComplete();

      ProjectMember updated = projectMemberRepository
          .findByIdAndNotDeleted(viewerMember.getId())
          .block();
      assertThat(updated.getRoleAsEnum()).isEqualTo(ProjectRole.EDITOR);
    }

    @Test
    @DisplayName("관리자 권한이 없으면 거부된다")
    void nonAdmin_rejected() {
      UpdateProjectMemberRoleRequest request = new UpdateProjectMemberRoleRequest(
          ProjectRole.COMMENTER);

      StepVerifier.create(
          projectService.updateMemberRole(
              testWorkspace.getId(),
              testProject.getId(),
              adminMember.getId(),
              request,
              viewerUser.getId() // VIEWER has no admin rights
          ))
          .expectErrorMatches(e -> e instanceof BusinessException &&
              ((BusinessException) e)
                  .getErrorCode() == ErrorCode.PROJECT_ADMIN_REQUIRED)
          .verify();
    }

  }

  @Nested
  @DisplayName("프로젝트 멤버 제거")
  class RemoveMember {

    @Test
    @DisplayName("마지막 OWNER/ADMIN 제거는 방지된다")
    void lastOwnerOrAdminRemoval_prevented() {
      projectMemberRepository.deleteById(adminMember.getId()).block();
      projectMemberRepository.deleteById(viewerMember.getId()).block();

      StepVerifier.create(
          projectService.removeMember(
              testWorkspace.getId(),
              testProject.getId(),
              ownerMember.getId(),
              ownerUser.getId()))
          .expectErrorMatches(e -> e instanceof BusinessException &&
              ((BusinessException) e)
                  .getErrorCode() == ErrorCode.LAST_OWNER_CANNOT_BE_REMOVED)
          .verify();
    }

    @Test
    @DisplayName("유효한 멤버 제거는 성공한다")
    void validMemberRemoval_success() {
      Long beforeCount = projectMemberRepository
          .countByProjectIdAndNotDeleted(testProject.getId()).block();

      Mono<Void> result = projectService.removeMember(
          testWorkspace.getId(),
          testProject.getId(),
          viewerMember.getId(),
          adminUser.getId());

      StepVerifier.create(result).verifyComplete();

      ProjectMember deleted = projectMemberRepository
          .findById(viewerMember.getId()).block();
      assertThat(deleted.isDeleted()).isTrue();

      Long afterCount = projectMemberRepository
          .countByProjectIdAndNotDeleted(testProject.getId()).block();
      assertThat(afterCount).isEqualTo(beforeCount - 1);
    }

    @Test
    @DisplayName("관리자 권한이 없으면 거부된다")
    void nonAdmin_rejected() {
      StepVerifier.create(
          projectService.removeMember(
              testWorkspace.getId(),
              testProject.getId(),
              adminMember.getId(),
              viewerUser.getId() // VIEWER has no admin rights
          ))
          .expectErrorMatches(e -> e instanceof BusinessException &&
              ((BusinessException) e)
                  .getErrorCode() == ErrorCode.PROJECT_ADMIN_REQUIRED)
          .verify();
    }

  }

  @Nested
  @DisplayName("프로젝트 자발적 탈퇴")
  class LeaveProject {

    @Test
    @DisplayName("마지막 OWNER/ADMIN은 탈퇴할 수 없다")
    void lastOwnerOrAdminLeave_prevented() {
      projectMemberRepository.deleteById(adminMember.getId()).block();
      projectMemberRepository.deleteById(viewerMember.getId()).block();

      StepVerifier.create(
          projectService.leaveProject(
              testWorkspace.getId(),
              testProject.getId(),
              ownerUser.getId()))
          .expectErrorMatches(e -> e instanceof BusinessException &&
              ((BusinessException) e)
                  .getErrorCode() == ErrorCode.LAST_OWNER_CANNOT_BE_REMOVED)
          .verify();
    }

    @Test
    @DisplayName("마지막 멤버 탈퇴 시 프로젝트도 삭제된다")
    void lastMemberLeave_deletesProject() {
      projectMemberRepository.deleteById(ownerMember.getId()).block();
      projectMemberRepository.deleteById(adminMember.getId()).block();

      Long memberCount = projectMemberRepository
          .countByProjectIdAndNotDeleted(testProject.getId()).block();
      assertThat(memberCount).isEqualTo(1L);

      Mono<Void> result = projectService.leaveProject(
          testWorkspace.getId(),
          testProject.getId(),
          viewerUser.getId());

      StepVerifier.create(result).verifyComplete();

      ProjectMember deletedMember = projectMemberRepository
          .findById(viewerMember.getId()).block();
      assertThat(deletedMember.isDeleted()).isTrue();

      Project deletedProject = projectRepository
          .findById(testProject.getId()).block();
      assertThat(deletedProject.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("일반 멤버 탈퇴는 성공한다")
    void normalMemberLeave_success() {
      Long beforeCount = projectMemberRepository
          .countByProjectIdAndNotDeleted(testProject.getId()).block();
      assertThat(beforeCount).isEqualTo(3L); // owner, admin, viewer

      Mono<Void> result = projectService.leaveProject(
          testWorkspace.getId(),
          testProject.getId(),
          viewerUser.getId());

      StepVerifier.create(result).verifyComplete();

      ProjectMember deleted = projectMemberRepository
          .findById(viewerMember.getId()).block();
      assertThat(deleted.isDeleted()).isTrue();

      Project project = projectRepository
          .findByIdAndNotDeleted(testProject.getId()).block();
      assertThat(project).isNotNull();
      assertThat(project.isDeleted()).isFalse();

      Long afterCount = projectMemberRepository
          .countByProjectIdAndNotDeleted(testProject.getId()).block();
      assertThat(afterCount).isEqualTo(beforeCount - 1);
    }

  }

}
