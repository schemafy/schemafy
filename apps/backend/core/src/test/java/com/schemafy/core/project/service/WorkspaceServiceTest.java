package com.schemafy.core.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.project.controller.dto.request.AddWorkspaceMemberRequest;
import com.schemafy.core.project.controller.dto.request.CreateWorkspaceRequest;
import com.schemafy.core.project.controller.dto.request.UpdateMemberRoleRequest;
import com.schemafy.core.project.repository.ProjectMemberRepository;
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.project.repository.vo.ProjectSettings;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.project.service.dto.WorkspaceDetail;
import com.schemafy.core.project.service.dto.WorkspaceMemberDetail;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;
import com.schemafy.core.user.repository.vo.UserInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("WorkspaceService 테스트")
class WorkspaceServiceTest {

  @Autowired
  private WorkspaceService workspaceService;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Autowired
  private WorkspaceMemberRepository workspaceMemberRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private ProjectMemberRepository projectMemberRepository;

  private User adminUser;
  private User memberUser;
  private User outsiderUser;
  private Workspace testWorkspace;
  private WorkspaceMember adminMember;
  private WorkspaceMember normalMember;

  @BeforeEach
  void setUp() {
    Mono.when(
        projectMemberRepository.deleteAll(),
        projectRepository.deleteAll(),
        workspaceMemberRepository.deleteAll(),
        workspaceRepository.deleteAll(),
        userRepository.deleteAll()).block();

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    adminUser = User.signUp(
        new UserInfo("admin@test.com", "Admin User", "password"),
        encoder).flatMap(userRepository::save).block();

    memberUser = User.signUp(
        new UserInfo("member@test.com", "Member User", "password"),
        encoder).flatMap(userRepository::save).block();

    outsiderUser = User.signUp(
        new UserInfo("outsider@test.com", "Outsider User", "password"),
        encoder).flatMap(userRepository::save).block();

    testWorkspace = Workspace.create(
        "Test Workspace",
        "Test Description");
    testWorkspace = workspaceRepository.save(testWorkspace).block();

    adminMember = WorkspaceMember.create(
        testWorkspace.getId(), adminUser.getId(), WorkspaceRole.ADMIN);
    adminMember = workspaceMemberRepository.save(adminMember).block();

    normalMember = WorkspaceMember.create(
        testWorkspace.getId(), memberUser.getId(),
        WorkspaceRole.MEMBER);
    normalMember = workspaceMemberRepository.save(normalMember).block();
  }

  @Nested
  @DisplayName("워크스페이스 생성")
  class CreateWorkspace {

    @Test
    @DisplayName("워크스페이스 생성 시 멤버도 함께 생성된다")
    void createWorkspace_CreatesOwnerMember() {
      CreateWorkspaceRequest request = new CreateWorkspaceRequest(
          "New Workspace",
          "New Description");

      Mono<WorkspaceDetail> result = workspaceService.createWorkspace(
          request.name(), request.description(), outsiderUser.getId());

      StepVerifier.create(result)
          .assertNext(detail -> {
            assertThat(detail.workspace().getName())
                .isEqualTo("New Workspace");
            assertThat(detail.workspace().getDescription())
                .isEqualTo("New Description");
          })
          .verifyComplete();

      StepVerifier.create(workspaceMemberRepository
          .findByUserIdAndNotDeleted(outsiderUser.getId())
          .collectList())
          .assertNext(members -> {
            assertThat(members).hasSize(1);
            assertThat(members.get(0).isAdmin()).isTrue();
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("워크스페이스 삭제")
  class DeleteWorkspace {

    @Test
    @DisplayName("워크스페이스 삭제 시 모든 멤버도 soft-delete 된다")
    void deleteWorkspace_SoftDeletesAllMembers() {
      // 추가 워크스페이스 생성 (기본 워크스페이스는 삭제 불가하므로)
      Workspace newWorkspace = Workspace.create(
          "Deletable Workspace",
          "Description");
      newWorkspace = workspaceRepository.save(newWorkspace).block();

      WorkspaceMember member1 = WorkspaceMember.create(
          newWorkspace.getId(), adminUser.getId(),
          WorkspaceRole.ADMIN);
      member1 = workspaceMemberRepository.save(member1).block();

      WorkspaceMember member2 = WorkspaceMember.create(
          newWorkspace.getId(), memberUser.getId(),
          WorkspaceRole.MEMBER);
      member2 = workspaceMemberRepository.save(member2).block();

      String workspaceId = newWorkspace.getId();

      Mono<Void> result = workspaceService.deleteWorkspace(
          workspaceId, adminUser.getId());

      StepVerifier.create(result).verifyComplete();

      Workspace deleted = workspaceRepository.findById(workspaceId)
          .block();
      assertThat(deleted.isDeleted()).isTrue();

      StepVerifier.create(workspaceMemberRepository
          .findByWorkspaceIdAndNotDeleted(workspaceId, 100, 0)
          .collectList())
          .assertNext(members -> assertThat(members).isEmpty())
          .verifyComplete();
    }

    @Test
    @DisplayName("이미 삭제된 워크스페이스 삭제 시 에러 발생")
    void deleteWorkspace_AlreadyDeleted_Fails() {
      Workspace newWorkspace = Workspace.create(
          "Will Delete",
          "Description");
      newWorkspace = workspaceRepository.save(newWorkspace).block();

      WorkspaceMember member = WorkspaceMember.create(
          newWorkspace.getId(), adminUser.getId(),
          WorkspaceRole.ADMIN);
      workspaceMemberRepository.save(member).block();

      workspaceService.deleteWorkspace(newWorkspace.getId(),
          adminUser.getId()).block();

      // 현재는 검증 로직에 의해 WORKSPACE_MEMBER_NOT_FOUND 발생
      // 추후 WORKSPACE_NOT_FOUND로 변경 검토
      StepVerifier.create(workspaceService.deleteWorkspace(
          newWorkspace.getId(), adminUser.getId()))
          .expectErrorMatches(e -> e instanceof BusinessException &&
              ((BusinessException) e)
                  .getErrorCode() == ErrorCode.WORKSPACE_MEMBER_NOT_FOUND)
          .verify();
    }

    @Test
    @DisplayName("ADMIN이 아닌 멤버는 삭제 불가")
    void deleteWorkspace_NonAdmin_Rejected() {
      StepVerifier.create(workspaceService.deleteWorkspace(
          testWorkspace.getId(), memberUser.getId()))
          .expectErrorMatches(e -> e instanceof BusinessException &&
              ((BusinessException) e)
                  .getErrorCode() == ErrorCode.WORKSPACE_ADMIN_REQUIRED)
          .verify();
    }

  }

  @Nested
  @DisplayName("멤버 관리")
  class MemberManagement {

    @Test
    @DisplayName("마지막 ADMIN 제거 시 에러 발생")
    void removeMember_LastAdmin_Prevented() {
      // 일반 멤버만 제거하고 ADMIN 1명만 남김
      workspaceMemberRepository.deleteById(normalMember.getId()).block();

      StepVerifier.create(workspaceService.removeMember(
          testWorkspace.getId(), adminUser.getId(),
          adminUser.getId()))
          .expectErrorMatches(e -> e instanceof BusinessException &&
              ((BusinessException) e)
                  .getErrorCode() == ErrorCode.LAST_ADMIN_CANNOT_LEAVE)
          .verify();
    }

    @Test
    @DisplayName("마지막 ADMIN 역할 변경 시 에러 발생")
    void updateMemberRole_LastAdminDowngrade_Prevented() {
      UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(
          WorkspaceRole.MEMBER);

      StepVerifier.create(workspaceService.updateMemberRole(
          testWorkspace.getId(), adminUser.getId(), request.role(),
          adminUser.getId()))
          .expectErrorMatches(e -> e instanceof BusinessException &&
              ((BusinessException) e)
                  .getErrorCode() == ErrorCode.LAST_ADMIN_CANNOT_LEAVE)
          .verify();
    }

    @Test
    @DisplayName("일반 멤버 제거 성공")
    void removeMember_NormalMember_Success() {
      Mono<Void> result = workspaceService.removeMember(
          testWorkspace.getId(), memberUser.getId(),
          adminUser.getId());

      StepVerifier.create(result).verifyComplete();

      WorkspaceMember deleted = workspaceMemberRepository
          .findById(normalMember.getId()).block();
      assertThat(deleted.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("역할 변경 성공 (멀티 ADMIN 상황)")
    void updateMemberRole_MultipleAdmins_Success() {
      // 기존 normalMember를 ADMIN으로 승격
      normalMember.updateRole(WorkspaceRole.ADMIN);
      normalMember = workspaceMemberRepository.save(normalMember).block();

      // 이제 ADMIN이 2명이므로 역할 변경 가능
      UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(
          WorkspaceRole.MEMBER);

      Mono<WorkspaceMemberDetail> result = workspaceService
          .updateMemberRole(testWorkspace.getId(),
              memberUser.getId(), request.role(), adminUser.getId());

      StepVerifier.create(result)
          .assertNext(response -> assertThat(response.member().getRole())
              .isEqualTo(WorkspaceRole.MEMBER.getValue()))
          .verifyComplete();
    }

    @Test
    @DisplayName("동시에 같은 유저 추가 시 하나만 성공 (Race Condition 방지)")
    void addMember_ConcurrentDuplicateUser_OnlyOneSucceeds() {
      BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
      User newUser = User.signUp(
          new UserInfo("concurrent@test.com", "Concurrent User",
              "password"),
          encoder).flatMap(userRepository::save).block();

      AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest(
          newUser.getEmail(), WorkspaceRole.MEMBER);

      Mono<Long> resultMono = Flux.range(0, 30)
          .flatMap(i -> workspaceService
              .addMember(testWorkspace.getId(), request.email(), request.role(),
                  adminUser.getId())
              .subscribeOn(Schedulers.parallel())
              .map(response -> 1) // 성공 시 1 반환
              .onErrorResume(e -> {
                // R2DBC에서 Unique Key 위반 시 DataIntegrityViolationException 발생
                if (e instanceof DataIntegrityViolationException) {
                  return Mono.just(0); // 중복 에러는 '성공 횟수 0'으로 처리 (정상적인 방어)
                }

                if (e instanceof BusinessException &&
                    ((BusinessException) e)
                        .getErrorCode() == ErrorCode.WORKSPACE_MEMBER_ALREADY_EXISTS) {
                  return Mono.just(0);
                }

                // 그 외 에러(NPE, Connection 등)는 테스트 실패로 간주해야 함
                System.err.println(
                    "Unexpected error: " + e.getMessage());
                return Mono.error(e);
              }),
              30 // flatMap의 동시성 레벨 명시
          )
          .reduce(0, Integer::sum)
          .map(Long::valueOf);

      StepVerifier.create(resultMono)
          .expectNext(1L) // 정확히 1건만 성공했는지 검증
          .verifyComplete();

      // DB에도 1개만 저장되어 있어야 함
      Long dbCount = workspaceMemberRepository
          .findByUserIdAndNotDeleted(newUser.getId()).count()
          .block();
      assertThat(dbCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("삭제된 멤버 재초대 시 재활성화")
    void addMember_DeletedMember_Reactivates() {
      // 멤버를 추가하고 삭제
      BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
      User newUser = User.signUp(
          new UserInfo("reactivate@test.com", "Reactivate User",
              "password"),
          encoder).flatMap(userRepository::save).block();

      AddWorkspaceMemberRequest addRequest = new AddWorkspaceMemberRequest(
          newUser.getEmail(), WorkspaceRole.MEMBER);

      // 멤버 추가
      WorkspaceMemberDetail added = workspaceService.addMember(
          testWorkspace.getId(), addRequest.email(), addRequest.role(), adminUser.getId())
          .block();
      assertThat(added).isNotNull();

      // 멤버 삭제
      workspaceService.removeMember(
          testWorkspace.getId(), added.member().getUserId(), adminUser.getId())
          .block();

      // 삭제 확인
      Boolean existsAfterDelete = workspaceMemberRepository
          .existsByWorkspaceIdAndUserIdAndNotDeleted(
              testWorkspace.getId(), newUser.getId())
          .block();
      assertThat(existsAfterDelete).isFalse();

      // 동일 멤버 재초대
      AddWorkspaceMemberRequest readdRequest = new AddWorkspaceMemberRequest(
          newUser.getEmail(), WorkspaceRole.ADMIN); // 다른 role로 재초대

      WorkspaceMemberDetail readded = workspaceService.addMember(
          testWorkspace.getId(), readdRequest.email(), readdRequest.role(), adminUser.getId())
          .block();

      // 재활성화 성공
      assertThat(readded).isNotNull();
      assertThat(readded.member().getUserId()).isEqualTo(newUser.getId());
      assertThat(readded.member().getRole())
          .isEqualTo(WorkspaceRole.ADMIN.getValue());

      // 활성 멤버로 존재 확인
      Boolean existsAfterReadd = workspaceMemberRepository
          .existsByWorkspaceIdAndUserIdAndNotDeleted(
              testWorkspace.getId(), newUser.getId())
          .block();
      assertThat(existsAfterReadd).isTrue();
    }

    @Test
    @DisplayName("멤버 직접 추가 시 기존 프로젝트에 VIEWER로 자동 추가된다")
    void addMember_PropagatesAsViewerToExistingProjects() {
      Project project = Project.create(testWorkspace.getId(), "Test Project", "Desc", ProjectSettings
          .defaultSettings());
      project = projectRepository.save(project).block();

      BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
      User newUser = User.signUp(
          new UserInfo("newmember@test.com", "New Member", "password"),
          encoder).flatMap(userRepository::save).block();

      AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest(
          newUser.getEmail(), WorkspaceRole.MEMBER);

      workspaceService.addMember(testWorkspace.getId(), request.email(), request.role(), adminUser.getId()).block();

      // 프로젝트에 VIEWER로 추가되었는지 확인
      ProjectMember pm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(project.getId(), newUser.getId()).block();
      assertThat(pm).isNotNull();
      assertThat(pm.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
    }

    @Test
    @DisplayName("멤버 제거 시 해당 워크스페이스의 프로젝트 멤버십도 cascade soft-delete 된다")
    void removeMember_cascadesProjectMemberships() {
      Project project1 = projectRepository.save(
          Project.create(testWorkspace.getId(), "Project 1", "Desc", ProjectSettings.defaultSettings())).block();
      Project project2 = projectRepository.save(
          Project.create(testWorkspace.getId(), "Project 2", "Desc", ProjectSettings.defaultSettings())).block();

      projectMemberRepository.save(
          ProjectMember.create(project1.getId(), memberUser.getId(), ProjectRole.VIEWER)).block();
      projectMemberRepository.save(
          ProjectMember.create(project2.getId(), memberUser.getId(), ProjectRole.VIEWER)).block();

      workspaceService.removeMember(testWorkspace.getId(), memberUser.getId(), adminUser.getId()).block();

      ProjectMember pm1 = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(project1.getId(), memberUser.getId()).block();
      ProjectMember pm2 = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(project2.getId(), memberUser.getId()).block();

      assertThat(pm1).isNull();
      assertThat(pm2).isNull();
    }

    @Test
    @DisplayName("멤버 제거 시 다른 워크스페이스의 프로젝트 멤버십은 영향 없다")
    void removeMember_cascadeDoesNotAffectOtherWorkspace() {
      Workspace otherWorkspace = workspaceRepository.save(
          Workspace.create("Other Workspace", "Desc")).block();
      workspaceMemberRepository.save(
          WorkspaceMember.create(otherWorkspace.getId(), adminUser.getId(), WorkspaceRole.ADMIN)).block();
      workspaceMemberRepository.save(
          WorkspaceMember.create(otherWorkspace.getId(), memberUser.getId(), WorkspaceRole.MEMBER)).block();

      Project projectInWs = projectRepository.save(
          Project.create(testWorkspace.getId(), "WS Project", "Desc", ProjectSettings.defaultSettings())).block();
      Project projectInOther = projectRepository.save(
          Project.create(otherWorkspace.getId(), "Other Project", "Desc", ProjectSettings.defaultSettings())).block();

      projectMemberRepository.save(
          ProjectMember.create(projectInWs.getId(), memberUser.getId(), ProjectRole.VIEWER)).block();
      projectMemberRepository.save(
          ProjectMember.create(projectInOther.getId(), memberUser.getId(), ProjectRole.VIEWER)).block();

      workspaceService.removeMember(testWorkspace.getId(), memberUser.getId(), adminUser.getId()).block();

      ProjectMember pmWs = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(projectInWs.getId(), memberUser.getId()).block();
      ProjectMember pmOther = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(projectInOther.getId(), memberUser.getId()).block();

      assertThat(pmWs).isNull();
      assertThat(pmOther).isNotNull();
    }

    @Test
    @DisplayName("프로젝트 멤버십이 없는 멤버 제거 시 정상 성공")
    void removeMember_noProjectMemberships_succeeds() {
      StepVerifier.create(workspaceService.removeMember(
          testWorkspace.getId(), memberUser.getId(), adminUser.getId()))
          .verifyComplete();

      WorkspaceMember deleted = workspaceMemberRepository.findById(normalMember.getId()).block();
      assertThat(deleted.isDeleted()).isTrue();
    }

    @Nested
    @DisplayName("역할 변경 전파 (UpdateMemberRole)")
    class UpdateMemberRole {

      @Test
      @DisplayName("MEMBER→ADMIN 승격 시 해당 워크스페이스의 프로젝트 역할도 ADMIN으로 변경된다")
      void updateMemberRole_propagatesUpgradeToProjects() {
        Project project1 = projectRepository.save(
            Project.create(testWorkspace.getId(), "Project 1", "Desc", ProjectSettings.defaultSettings())).block();
        Project project2 = projectRepository.save(
            Project.create(testWorkspace.getId(), "Project 2", "Desc", ProjectSettings.defaultSettings())).block();

        projectMemberRepository.save(
            ProjectMember.create(project1.getId(), memberUser.getId(), ProjectRole.VIEWER)).block();
        projectMemberRepository.save(
            ProjectMember.create(project2.getId(), memberUser.getId(), ProjectRole.VIEWER)).block();

        // memberUser는 setUp에서 이미 WorkspaceRole.MEMBER → ADMIN으로 직접 승격 (중간 단계 없음)
        workspaceService.updateMemberRole(
            testWorkspace.getId(), memberUser.getId(), WorkspaceRole.ADMIN, adminUser.getId()).block();

        ProjectMember pm1 = projectMemberRepository
            .findByProjectIdAndUserIdAndNotDeleted(project1.getId(), memberUser.getId()).block();
        ProjectMember pm2 = projectMemberRepository
            .findByProjectIdAndUserIdAndNotDeleted(project2.getId(), memberUser.getId()).block();

        assertThat(pm1).isNotNull();
        assertThat(pm1.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
        assertThat(pm2).isNotNull();
        assertThat(pm2.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
      }

      @Test
      @DisplayName("ADMIN→MEMBER 강등 시 해당 워크스페이스의 프로젝트 역할도 VIEWER로 변경된다")
      void updateMemberRole_propagatesDowngradeToProjects() {
        Project project = projectRepository.save(
            Project.create(testWorkspace.getId(), "Downgrade Project", "Desc", ProjectSettings.defaultSettings()))
            .block();

        projectMemberRepository.save(
            ProjectMember.create(project.getId(), memberUser.getId(), ProjectRole.ADMIN)).block();

        // normalMember를 ADMIN으로 먼저 승격
        normalMember.updateRole(WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(normalMember).block();

        workspaceService.updateMemberRole(
            testWorkspace.getId(), memberUser.getId(), WorkspaceRole.MEMBER, adminUser.getId()).block();

        ProjectMember pm = projectMemberRepository
            .findByProjectIdAndUserIdAndNotDeleted(project.getId(), memberUser.getId()).block();

        assertThat(pm).isNotNull();
        assertThat(pm.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
      }

      @Test
      @DisplayName("프로젝트 멤버십이 없는 경우 에러 없이 WS 역할만 변경된다")
      void updateMemberRole_noProjects_succeeds() {
        normalMember.updateRole(WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(normalMember).block();

        StepVerifier.create(workspaceService.updateMemberRole(
            testWorkspace.getId(), memberUser.getId(), WorkspaceRole.MEMBER, adminUser.getId()))
            .assertNext(detail -> assertThat(detail.member().getRole()).isEqualTo(WorkspaceRole.MEMBER.getValue()))
            .verifyComplete();
      }

      @Test
      @DisplayName("Workspace-A 역할 변경 시 Workspace-B 프로젝트 역할은 영향 없다")
      void updateMemberRole_cascadeDoesNotAffectOtherWorkspace() {
        Workspace otherWorkspace = workspaceRepository.save(
            Workspace.create("Other Workspace", "Desc")).block();
        workspaceMemberRepository.save(
            WorkspaceMember.create(otherWorkspace.getId(), adminUser.getId(), WorkspaceRole.ADMIN)).block();
        workspaceMemberRepository.save(
            WorkspaceMember.create(otherWorkspace.getId(), memberUser.getId(), WorkspaceRole.MEMBER)).block();

        Project projectInWs = projectRepository.save(
            Project.create(testWorkspace.getId(), "WS Project", "Desc", ProjectSettings.defaultSettings())).block();
        Project projectInOther = projectRepository.save(
            Project.create(otherWorkspace.getId(), "Other Project", "Desc", ProjectSettings.defaultSettings())).block();

        projectMemberRepository.save(
            ProjectMember.create(projectInWs.getId(), memberUser.getId(), ProjectRole.VIEWER)).block();
        projectMemberRepository.save(
            ProjectMember.create(projectInOther.getId(), memberUser.getId(), ProjectRole.VIEWER)).block();

        // normalMember를 ADMIN으로 먼저 승격
        normalMember.updateRole(WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(normalMember).block();

        workspaceService.updateMemberRole(
            testWorkspace.getId(), memberUser.getId(), WorkspaceRole.ADMIN, adminUser.getId()).block();

        ProjectMember pmWs = projectMemberRepository
            .findByProjectIdAndUserIdAndNotDeleted(projectInWs.getId(), memberUser.getId()).block();
        ProjectMember pmOther = projectMemberRepository
            .findByProjectIdAndUserIdAndNotDeleted(projectInOther.getId(), memberUser.getId()).block();

        assertThat(pmWs).isNotNull();
        assertThat(pmWs.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
        assertThat(pmOther).isNotNull();
        assertThat(pmOther.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
      }

      @Test
      @DisplayName("ADMIN→MEMBER 강등 시 프로젝트 EDITOR 역할은 보호된다 (VIEWER로 내려가지 않음)")
      void updateMemberRole_downgrade_preservesEditorRole() {
        Project project = projectRepository.save(
            Project.create(testWorkspace.getId(), "Editor Project", "Desc", ProjectSettings.defaultSettings()))
            .block();

        projectMemberRepository.save(
            ProjectMember.create(project.getId(), memberUser.getId(), ProjectRole.EDITOR)).block();

        // normalMember를 ADMIN으로 먼저 승격
        normalMember.updateRole(WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(normalMember).block();

        workspaceService.updateMemberRole(
            testWorkspace.getId(), memberUser.getId(), WorkspaceRole.MEMBER, adminUser.getId()).block();

        ProjectMember pm = projectMemberRepository
            .findByProjectIdAndUserIdAndNotDeleted(project.getId(), memberUser.getId()).block();

        assertThat(pm).isNotNull();
        assertThat(pm.getRoleAsEnum()).isEqualTo(ProjectRole.EDITOR);
      }

      @Test
      @DisplayName("MEMBER→ADMIN 승격 시 프로젝트 EDITOR 역할은 ADMIN으로 덮어씌워진다")
      void updateMemberRole_upgrade_overridesEditorRole() {
        Project project = projectRepository.save(
            Project.create(testWorkspace.getId(), "Editor Upgrade Project", "Desc", ProjectSettings.defaultSettings()))
            .block();

        projectMemberRepository.save(
            ProjectMember.create(project.getId(), memberUser.getId(), ProjectRole.EDITOR)).block();

        // normalMember를 ADMIN으로 먼저 승격 (이후 테스트 대상 멤버를 ADMIN으로 변경)
        normalMember.updateRole(WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(normalMember).block();

        workspaceService.updateMemberRole(
            testWorkspace.getId(), memberUser.getId(), WorkspaceRole.ADMIN, adminUser.getId()).block();

        ProjectMember pm = projectMemberRepository
            .findByProjectIdAndUserIdAndNotDeleted(project.getId(), memberUser.getId()).block();

        assertThat(pm).isNotNull();
        assertThat(pm.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
      }

      @Test
      @DisplayName("같은 역할로 변경 시 에러 없이 정상 완료된다")
      void updateMemberRole_sameRole_noOp() {
        Project project = projectRepository.save(
            Project.create(testWorkspace.getId(), "Same Role Project", "Desc", ProjectSettings.defaultSettings()))
            .block();

        projectMemberRepository.save(
            ProjectMember.create(project.getId(), memberUser.getId(), ProjectRole.VIEWER)).block();

        StepVerifier.create(workspaceService.updateMemberRole(
            testWorkspace.getId(), memberUser.getId(), WorkspaceRole.MEMBER, adminUser.getId()))
            .assertNext(detail -> assertThat(detail.member().getRole()).isEqualTo(WorkspaceRole.MEMBER.getValue()))
            .verifyComplete();

        ProjectMember pm = projectMemberRepository
            .findByProjectIdAndUserIdAndNotDeleted(project.getId(), memberUser.getId()).block();
        assertThat(pm).isNotNull();
        assertThat(pm.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
      }

      @Test
      @DisplayName("유일 Project ADMIN 강등 시 VIEWER로 강등된다")
      void updateMemberRole_soleProjectAdmin_downgradeProceeds() {
        Project project = projectRepository.save(
            Project.create(testWorkspace.getId(), "Sole Admin Project", "Desc", ProjectSettings.defaultSettings()))
            .block();

        projectMemberRepository.save(
            ProjectMember.create(project.getId(), memberUser.getId(), ProjectRole.ADMIN)).block();

        // normalMember를 ADMIN으로 승격 후 다시 MEMBER로 강등 (admin guard 없이 통과해야 함)
        normalMember.updateRole(WorkspaceRole.ADMIN);
        workspaceMemberRepository.save(normalMember).block();

        StepVerifier.create(workspaceService.updateMemberRole(
            testWorkspace.getId(), memberUser.getId(), WorkspaceRole.MEMBER, adminUser.getId()))
            .assertNext(detail -> assertThat(detail.member().getRole()).isEqualTo(WorkspaceRole.MEMBER.getValue()))
            .verifyComplete();

        ProjectMember pm = projectMemberRepository
            .findByProjectIdAndUserIdAndNotDeleted(project.getId(), memberUser.getId()).block();
        assertThat(pm).isNotNull();
        assertThat(pm.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
      }

    }

    @Test
    @DisplayName("ADMIN으로 직접 추가 시 기존 프로젝트에 ADMIN으로 자동 추가된다")
    void addMember_Admin_PropagatesAsAdminToExistingProjects() {
      Project project = Project.create(testWorkspace.getId(), "Test Project", "Desc", ProjectSettings
          .defaultSettings());
      project = projectRepository.save(project).block();

      BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
      User newUser = User.signUp(
          new UserInfo("newadmin@test.com", "New Admin", "password"),
          encoder).flatMap(userRepository::save).block();

      AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest(
          newUser.getEmail(), WorkspaceRole.ADMIN);

      workspaceService.addMember(testWorkspace.getId(), request.email(), request.role(), adminUser.getId()).block();

      ProjectMember pm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(project.getId(), newUser.getId()).block();
      assertThat(pm).isNotNull();
      assertThat(pm.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
    }

  }

  @Nested
  @DisplayName("멤버 탈퇴")
  class LeaveMember {

    @Test
    @DisplayName("마지막 멤버 탈퇴 시 워크스페이스 삭제")
    void leaveMember_LastMember_DeletesWorkspace() {
      // 새 워크스페이스 생성 (테스트 격리)
      Workspace singleMemberWorkspace = Workspace.create(
          "Single Member Workspace",
          "Description");
      singleMemberWorkspace = workspaceRepository.save(
          singleMemberWorkspace).block();

      WorkspaceMember singleMember = WorkspaceMember.create(
          singleMemberWorkspace.getId(), memberUser.getId(),
          WorkspaceRole.ADMIN);
      workspaceMemberRepository.save(singleMember).block();

      String workspaceId = singleMemberWorkspace.getId();

      Mono<Void> result = workspaceService.leaveMember(
          workspaceId, memberUser.getId());

      StepVerifier.create(result).verifyComplete();

      Workspace deleted = workspaceRepository.findById(workspaceId)
          .block();
      assertThat(deleted.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("마지막 ADMIN 탈퇴 시 에러 발생 (다른 멤버 있음)")
    void leaveMember_LastAdmin_Prevented() {
      StepVerifier.create(workspaceService.leaveMember(
          testWorkspace.getId(), adminUser.getId()))
          .expectErrorMatches(e -> e instanceof BusinessException &&
              ((BusinessException) e)
                  .getErrorCode() == ErrorCode.LAST_ADMIN_CANNOT_LEAVE)
          .verify();
    }

    @Test
    @DisplayName("일반 멤버 탈퇴 성공")
    void leaveMember_NormalMember_Success() {
      Mono<Void> result = workspaceService.leaveMember(
          testWorkspace.getId(), memberUser.getId());

      StepVerifier.create(result).verifyComplete();

      WorkspaceMember deleted = workspaceMemberRepository
          .findById(normalMember.getId()).block();
      assertThat(deleted.isDeleted()).isTrue();

      Workspace workspace = workspaceRepository
          .findByIdAndNotDeleted(testWorkspace.getId()).block();
      assertThat(workspace).isNotNull();
      assertThat(workspace.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("멤버 탈퇴 시 해당 워크스페이스의 프로젝트 멤버십도 cascade soft-delete 된다")
    void leaveMember_cascadesProjectMemberships() {
      Project project = projectRepository.save(
          Project.create(testWorkspace.getId(), "Cascade Project", "Desc", ProjectSettings.defaultSettings())).block();
      projectMemberRepository.save(
          ProjectMember.create(project.getId(), memberUser.getId(), ProjectRole.VIEWER)).block();

      workspaceService.leaveMember(testWorkspace.getId(), memberUser.getId()).block();

      ProjectMember pm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(project.getId(), memberUser.getId()).block();
      assertThat(pm).isNull();
    }

    @Test
    @DisplayName("마지막 멤버 탈퇴 시 워크스페이스의 프로젝트와 프로젝트 멤버십도 cascade soft-delete 된다")
    void leaveMember_lastMember_cascadesProjectsAndMembers() {
      Workspace singleMemberWorkspace = workspaceRepository.save(
          Workspace.create("Single Member WS", "Desc")).block();
      WorkspaceMember singleMember = workspaceMemberRepository.save(
          WorkspaceMember.create(singleMemberWorkspace.getId(), memberUser.getId(), WorkspaceRole.ADMIN)).block();

      Project project = projectRepository.save(
          Project.create(singleMemberWorkspace.getId(), "Solo Project", "Desc", ProjectSettings.defaultSettings()))
          .block();
      projectMemberRepository.save(
          ProjectMember.create(project.getId(), memberUser.getId(), ProjectRole.ADMIN)).block();

      String workspaceId = singleMemberWorkspace.getId();
      String projectId = project.getId();

      workspaceService.leaveMember(workspaceId, memberUser.getId()).block();

      Workspace deletedWs = workspaceRepository.findById(workspaceId).block();
      assertThat(deletedWs.isDeleted()).isTrue();

      Project deletedProject = projectRepository.findById(projectId).block();
      assertThat(deletedProject.isDeleted()).isTrue();

      ProjectMember pm = projectMemberRepository
          .findByProjectIdAndUserIdAndNotDeleted(projectId, memberUser.getId()).block();
      assertThat(pm).isNull();
    }

  }

  @Nested
  @DisplayName("워크스페이스 삭제 cascade")
  class DeleteWorkspaceCascade {

    @Test
    @DisplayName("워크스페이스 삭제 시 프로젝트와 프로젝트 멤버십도 cascade soft-delete 된다")
    void deleteWorkspace_cascadesProjectsAndMembers() {
      Workspace newWorkspace = workspaceRepository.save(
          Workspace.create("Cascade Workspace", "Desc")).block();
      workspaceMemberRepository.save(
          WorkspaceMember.create(newWorkspace.getId(), adminUser.getId(), WorkspaceRole.ADMIN)).block();

      Project project1 = projectRepository.save(
          Project.create(newWorkspace.getId(), "Project A", "Desc", ProjectSettings.defaultSettings())).block();
      Project project2 = projectRepository.save(
          Project.create(newWorkspace.getId(), "Project B", "Desc", ProjectSettings.defaultSettings())).block();

      projectMemberRepository.save(
          ProjectMember.create(project1.getId(), adminUser.getId(), ProjectRole.ADMIN)).block();
      projectMemberRepository.save(
          ProjectMember.create(project1.getId(), memberUser.getId(), ProjectRole.VIEWER)).block();
      projectMemberRepository.save(
          ProjectMember.create(project2.getId(), adminUser.getId(), ProjectRole.ADMIN)).block();
      projectMemberRepository.save(
          ProjectMember.create(project2.getId(), memberUser.getId(), ProjectRole.VIEWER)).block();

      String workspaceId = newWorkspace.getId();
      String project1Id = project1.getId();
      String project2Id = project2.getId();

      workspaceService.deleteWorkspace(workspaceId, adminUser.getId()).block();

      Project deletedProject1 = projectRepository.findById(project1Id).block();
      Project deletedProject2 = projectRepository.findById(project2Id).block();
      assertThat(deletedProject1.isDeleted()).isTrue();
      assertThat(deletedProject2.isDeleted()).isTrue();

      StepVerifier.create(projectMemberRepository
          .findByProjectIdAndNotDeleted(project1Id, 100, 0).collectList())
          .assertNext(members -> assertThat(members).isEmpty())
          .verifyComplete();

      StepVerifier.create(projectMemberRepository
          .findByProjectIdAndNotDeleted(project2Id, 100, 0).collectList())
          .assertNext(members -> assertThat(members).isEmpty())
          .verifyComplete();
    }

  }

}
