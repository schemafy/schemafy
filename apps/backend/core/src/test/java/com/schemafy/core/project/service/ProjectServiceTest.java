package com.schemafy.core.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.project.controller.dto.request.UpdateProjectMemberRoleRequest;
import com.schemafy.core.project.exception.ProjectErrorCode;
import com.schemafy.core.project.repository.*;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.project.service.dto.ProjectMemberDetail;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;
import com.schemafy.domain.common.exception.DomainException;

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

  private User primaryAdminUser;
  private User adminUser;
  private User viewerUser;
  private Workspace testWorkspace;
  private Project testProject;
  private ProjectMember primaryAdminMember;
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

    primaryAdminUser = User.signUp(
        new UserInfo("primary-admin@test.com", "Primary Admin User", "password"),
        encoder).flatMap(userRepository::save).block();

    adminUser = User.signUp(
        new UserInfo("admin@test.com", "Admin User", "password"),
        encoder).flatMap(userRepository::save).block();

    viewerUser = User.signUp(
        new UserInfo("viewer@test.com", "Viewer User", "password"),
        encoder).flatMap(userRepository::save).block();

    testWorkspace = Workspace.create(
        "Test Workspace",
        "Test Description");
    testWorkspace = workspaceRepository.save(testWorkspace).block();

    workspaceMemberRepository.save(
        WorkspaceMember.create(testWorkspace.getId(), primaryAdminUser.getId(),
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

    testProject = Project.create(testWorkspace.getId(), "Test Project", "Test Description");
    testProject = projectRepository.save(testProject).block();

    primaryAdminMember = ProjectMember.create(testProject.getId(),
        primaryAdminUser.getId(), ProjectRole.ADMIN);
    primaryAdminMember = projectMemberRepository.save(primaryAdminMember).block();

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
              testProject.getId(),
              adminUser.getId(),
              request.role(),
              adminUser.getId()))
          .expectErrorMatches(e -> e instanceof DomainException &&
              ((DomainException) e)
                  .getErrorCode() == ProjectErrorCode.CANNOT_CHANGE_OWN_ROLE)
          .verify();
    }

    @Test
    @DisplayName("마지막 ADMIN의 권한 변경은 방지된다")
    void lastAdminDowngrade_prevented() {
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
              testProject.getId(),
              primaryAdminUser.getId(),
              request.role(),
              primaryAdminUser.getId()))
          .expectErrorMatches(e -> e instanceof DomainException &&
              ((DomainException) e)
                  .getErrorCode() == ProjectErrorCode.CANNOT_CHANGE_OWN_ROLE)
          .verify();
    }

    @Test
    @DisplayName("유효한 권한 변경은 성공한다")
    void validRoleChange_success() {
      UpdateProjectMemberRoleRequest request = new UpdateProjectMemberRoleRequest(
          ProjectRole.EDITOR);

      Mono<ProjectMemberDetail> result = projectService
          .updateMemberRole(
              testProject.getId(),
              viewerUser.getId(),
              request.role(),
              adminUser.getId());

      StepVerifier.create(result)
          .assertNext(response -> {
            assertThat(response.member().getUserId())
                .isEqualTo(viewerUser.getId());
            assertThat(response.member().getRole())
                .isEqualTo(ProjectRole.EDITOR.name());
          })
          .verifyComplete();

      ProjectMember updated = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(testProject.getId(), viewerUser.getId())
          .block();
      assertThat(updated.getRoleAsEnum()).isEqualTo(ProjectRole.EDITOR);
    }

    @Test
    @DisplayName("관리자 권한이 없으면 권한 변경이 거부된다")
    void nonAdmin_rejected() {
      UpdateProjectMemberRoleRequest request = new UpdateProjectMemberRoleRequest(
          ProjectRole.VIEWER);

      StepVerifier.create(
          projectService.updateMemberRole(
              testProject.getId(),
              adminUser.getId(),
              request.role(),
              viewerUser.getId() // VIEWER has no admin rights
          ))
          .expectErrorMatches(e -> e instanceof DomainException &&
              ((DomainException) e)
                  .getErrorCode() == ProjectErrorCode.ADMIN_REQUIRED)
          .verify();
    }

    @Test
    @DisplayName("마지막 ADMIN을 다른 역할로 강등 시도 시 방지된다")
    void demoteLastAdmin_prevented() {
      // adminMember와 viewerMember 제거하여 primaryAdminUser만 ADMIN으로 남김
      projectMemberRepository.deleteById(adminMember.getId()).block();
      projectMemberRepository.deleteById(viewerMember.getId()).block();

      // 새 ADMIN 추가 (요청자 역할)
      User secondAdmin = User.signUp(
          new UserInfo("second@test.com", "Second Admin", "password"),
          new BCryptPasswordEncoder()).flatMap(userRepository::save).block();
      workspaceMemberRepository.save(
          WorkspaceMember.create(testWorkspace.getId(), secondAdmin.getId(), WorkspaceRole.ADMIN)).block();
      ProjectMember secondAdminMember = ProjectMember.create(
          testProject.getId(), secondAdmin.getId(), ProjectRole.ADMIN);
      projectMemberRepository.save(secondAdminMember).block();

      // 현재: primaryAdminUser(ADMIN) + secondAdmin(ADMIN) = 2명
      // secondAdmin이 primaryAdminUser를 VIEWER로 강등 → 성공 (ADMIN 1명 남음)
      UpdateProjectMemberRoleRequest request = new UpdateProjectMemberRoleRequest(ProjectRole.VIEWER);
      StepVerifier.create(
          projectService.updateMemberRole(
              testProject.getId(), primaryAdminUser.getId(), request.role(), secondAdmin.getId()))
          .assertNext(response -> assertThat(response.member().getRole()).isEqualTo(ProjectRole.VIEWER.name()))
          .verifyComplete();

      // 현재: primaryAdminUser(VIEWER) + secondAdmin(ADMIN) = ADMIN 1명
      // primaryAdminUser는 이제 VIEWER이므로 secondAdmin을 강등할 수 없음 (PROJECT_ADMIN_REQUIRED)
      StepVerifier.create(
          projectService.updateMemberRole(
              testProject.getId(), secondAdmin.getId(), request.role(), primaryAdminUser.getId()))
          .expectErrorMatches(e -> e instanceof DomainException &&
              ((DomainException) e).getErrorCode() == ProjectErrorCode.ADMIN_REQUIRED)
          .verify();
    }

  }

  @Nested
  @DisplayName("프로젝트 멤버 제거")
  class RemoveMember {

    @Test
    @DisplayName("마지막 ADMIN도 제거할 수 있다")
    void lastAdminRemoval_allowed() {
      projectMemberRepository.deleteById(adminMember.getId()).block();
      projectMemberRepository.deleteById(viewerMember.getId()).block();

      StepVerifier.create(
          projectService.removeMember(
              testProject.getId(),
              primaryAdminUser.getId(),
              primaryAdminUser.getId()))
          .verifyComplete();

      ProjectMember deleted = projectMemberRepository
          .findById(primaryAdminMember.getId()).block();
      assertThat(deleted).isNotNull();
      assertThat(deleted.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("유효한 멤버 제거는 성공한다")
    void validMemberRemoval_success() {
      Long beforeCount = projectMemberRepository
          .countByProjectIdAndNotDeleted(testProject.getId()).block();

      Mono<Void> result = projectService.removeMember(
          testProject.getId(),
          viewerUser.getId(),
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
              testProject.getId(),
              adminUser.getId(),
              viewerUser.getId() // VIEWER has no admin rights
          ))
          .expectErrorMatches(e -> e instanceof DomainException &&
              ((DomainException) e)
                  .getErrorCode() == ProjectErrorCode.ADMIN_REQUIRED)
          .verify();
    }

  }

  @Nested
  @DisplayName("프로젝트 자발적 탈퇴")
  class LeaveProject {

    @Test
    @DisplayName("마지막 ADMIN도 탈퇴할 수 있다")
    void lastAdminLeave_allowed() {
      projectMemberRepository.deleteById(adminMember.getId()).block();
      projectMemberRepository.deleteById(viewerMember.getId()).block();

      StepVerifier.create(
          projectService.leaveProject(
              testProject.getId(),
              primaryAdminUser.getId()))
          .verifyComplete();

      ProjectMember deletedMember = projectMemberRepository
          .findById(primaryAdminMember.getId()).block();
      assertThat(deletedMember).isNotNull();
      assertThat(deletedMember.isDeleted()).isTrue();

      Project deletedProject = projectRepository
          .findById(testProject.getId()).block();
      assertThat(deletedProject).isNotNull();
      assertThat(deletedProject.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("마지막 멤버 탈퇴 시 프로젝트도 삭제된다")
    void lastMemberLeave_deletesProject() {
      projectMemberRepository.deleteById(primaryAdminMember.getId()).block();
      projectMemberRepository.deleteById(adminMember.getId()).block();

      Long memberCount = projectMemberRepository
          .countByProjectIdAndNotDeleted(testProject.getId()).block();
      assertThat(memberCount).isEqualTo(1L);

      Mono<Void> result = projectService.leaveProject(
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
      assertThat(beforeCount).isEqualTo(3L); // primaryAdmin, admin, viewer

      Mono<Void> result = projectService.leaveProject(
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

  @Nested
  @DisplayName("프로젝트 생성 시 워크스페이스 멤버 자동 추가")
  class CreateProjectMemberPropagation {

    @Test
    @DisplayName("프로젝트 생성 시 워크스페이스 MEMBER는 프로젝트 VIEWER로 추가된다")
    void createProject_WorkspaceMember_AddedAsProjectViewer() {
      // primaryAdminUser는 workspace ADMIN, adminUser와 viewerUser는 workspace MEMBER
      // createProject는 workspace ADMIN만 호출 가능
      projectService.createProject(testWorkspace.getId(), "New Project", "Description", primaryAdminUser.getId())
          .block();

      // 새로 생성된 프로젝트 찾기
      var projects = projectRepository.findByWorkspaceIdAndNotDeleted(testWorkspace.getId())
          .filter(p -> p.getName().equals("New Project"))
          .collectList().block();
      assertThat(projects).hasSize(1);
      String newProjectId = projects.get(0).getId();

      // primaryAdminUser는 ADMIN으로 추가 (생성자)
      ProjectMember primaryAdminPm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(newProjectId, primaryAdminUser.getId()).block();
      assertThat(primaryAdminPm).isNotNull();
      assertThat(primaryAdminPm.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);

      // adminUser는 workspace MEMBER이므로 VIEWER로 추가
      ProjectMember adminPm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(newProjectId, adminUser.getId()).block();
      assertThat(adminPm).isNotNull();
      assertThat(adminPm.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);

      // viewerUser는 workspace MEMBER이므로 VIEWER로 추가
      ProjectMember viewerPm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(newProjectId, viewerUser.getId()).block();
      assertThat(viewerPm).isNotNull();
      assertThat(viewerPm.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
    }

    @Test
    @DisplayName("삭제된 워크스페이스 멤버는 프로젝트에 추가되지 않는다")
    void createProject_DeletedWorkspaceMember_Skipped() {
      // viewerUser의 워크스페이스 멤버십을 soft-delete
      WorkspaceMember viewerWsMember = workspaceMemberRepository
          .findByWorkspaceIdAndUserIdAndNotDeleted(testWorkspace.getId(), viewerUser.getId()).block();
      viewerWsMember.delete();
      workspaceMemberRepository.save(viewerWsMember).block();

      projectService.createProject(testWorkspace.getId(), "New Project", "Description", primaryAdminUser.getId())
          .block();

      var projects = projectRepository.findByWorkspaceIdAndNotDeleted(testWorkspace.getId())
          .filter(p -> p.getName().equals("New Project"))
          .collectList().block();
      String newProjectId = projects.get(0).getId();

      // viewerUser는 soft-deleted이므로 프로젝트에 추가되지 않아야 함
      ProjectMember viewerPm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(newProjectId, viewerUser.getId()).block();
      assertThat(viewerPm).isNull();

      // adminUser는 활성 멤버이므로 추가되어야 함
      ProjectMember adminPm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(newProjectId, adminUser.getId()).block();
      assertThat(adminPm).isNotNull();
      assertThat(adminPm.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
    }

    @Test
    @DisplayName("프로젝트 생성자는 중복 추가되지 않는다")
    void createProject_CreatorNotDuplicated() {
      projectService.createProject(testWorkspace.getId(), "New Project", "Description", primaryAdminUser.getId())
          .block();

      var projects = projectRepository.findByWorkspaceIdAndNotDeleted(testWorkspace.getId())
          .filter(p -> p.getName().equals("New Project"))
          .collectList().block();
      String newProjectId = projects.get(0).getId();

      // 생성자를 포함한 전체 프로젝트 멤버 수를 검증
      Long memberCount = projectMemberRepository
          .countByProjectIdAndNotDeleted(newProjectId).block();
      // 3명: primaryAdminUser(ADMIN) + adminUser(VIEWER) + viewerUser(VIEWER)
      assertThat(memberCount).isEqualTo(3L);
    }

  }

}
