package com.schemafy.core.project.integration;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberUseCase;
import com.schemafy.core.project.application.port.in.CreateProjectCommand;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationUseCase;
import com.schemafy.core.project.application.port.in.CreateProjectUseCase;
import com.schemafy.core.project.application.port.in.CreateWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.in.DeleteProjectCommand;
import com.schemafy.core.project.application.port.in.DeleteProjectUseCase;
import com.schemafy.core.project.application.port.in.LeaveProjectCommand;
import com.schemafy.core.project.application.port.in.LeaveProjectUseCase;
import com.schemafy.core.project.application.port.in.RemoveWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.in.RemoveWorkspaceMemberUseCase;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleCommand;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleUseCase;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.InvitationType;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.user.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@DisplayName("워크스페이스/프로젝트 race condition 통합 테스트")
class WorkspaceProjectRaceConditionIntegrationTest
    extends ProjectDomainIntegrationSupport {

  @Autowired
  private CreateWorkspaceInvitationUseCase createWorkspaceInvitationUseCase;

  @Autowired
  private CreateProjectInvitationUseCase createProjectInvitationUseCase;

  @Autowired
  private CreateProjectUseCase createProjectUseCase;

  @Autowired
  private AddWorkspaceMemberUseCase addWorkspaceMemberUseCase;

  @Autowired
  private AcceptWorkspaceInvitationUseCase acceptWorkspaceInvitationUseCase;

  @Autowired
  private UpdateWorkspaceMemberRoleUseCase updateWorkspaceMemberRoleUseCase;

  @Autowired
  private RemoveWorkspaceMemberUseCase removeWorkspaceMemberUseCase;

  @Autowired
  private LeaveProjectUseCase leaveProjectUseCase;

  @Autowired
  private DeleteProjectUseCase deleteProjectUseCase;

  @Test
  @DisplayName("만료된 워크스페이스 pending 초대는 생성 전에 정리되어 새 초대를 만들 수 있다")
  void createWorkspaceInvitation_cleansExpiredPendingInvitation() {
    User admin = signUpUser("admin-wi-expired@test.com", "Admin");
    User invitee = signUpUser("invitee-wi-expired@test.com", "Invitee");
    Workspace workspace = saveWorkspace("Workspace Expired Invitation", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Invitation expiredInvitation = saveWorkspaceInvitation(workspace, invitee.email(),
        WorkspaceRole.MEMBER, admin);
    updateInvitationExpiration(expiredInvitation.getId(), Instant.now().minusSeconds(60));

    Invitation createdInvitation = createWorkspaceInvitationUseCase.createWorkspaceInvitation(
        new CreateWorkspaceInvitationCommand(workspace.getId(), invitee.email(),
            WorkspaceRole.MEMBER, admin.id()))
        .block();

    assertThat(createdInvitation).isNotNull();
    assertThat(invitationRepository.findById(expiredInvitation.getId()).block()
        .getStatusAsEnum()).isEqualTo(InvitationStatus.CANCELLED);
    assertThat(countInvitations(InvitationType.WORKSPACE, workspace.getId(),
        invitee.email(), InvitationStatus.PENDING)).isEqualTo(1);
  }

  @Test
  @DisplayName("만료된 프로젝트 pending 초대는 생성 전에 정리되어 새 초대를 만들 수 있다")
  void createProjectInvitation_cleansExpiredPendingInvitation() {
    User admin = signUpUser("admin-pi-expired@test.com", "Admin");
    User invitee = signUpUser("invitee-pi-expired@test.com", "Invitee");
    Workspace workspace = saveWorkspace("Project Expired Invitation", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Project project = saveProject(workspace, "Project Expired Invitation");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    Invitation expiredInvitation = saveProjectInvitation(project, workspace, invitee.email(),
        ProjectRole.EDITOR, admin);
    updateInvitationExpiration(expiredInvitation.getId(), Instant.now().minusSeconds(60));

    Invitation createdInvitation = createProjectInvitationUseCase.createProjectInvitation(
        new CreateProjectInvitationCommand(project.getId(), invitee.email(),
            ProjectRole.EDITOR, admin.id()))
        .block();

    assertThat(createdInvitation).isNotNull();
    assertThat(invitationRepository.findById(expiredInvitation.getId()).block()
        .getStatusAsEnum()).isEqualTo(InvitationStatus.CANCELLED);
    assertThat(countInvitations(InvitationType.PROJECT, project.getId(),
        invitee.email(), InvitationStatus.PENDING)).isEqualTo(1);
  }

  @Test
  @DisplayName("프로젝트 생성과 워크스페이스 멤버 추가가 동시에 일어나도 프로젝트 멤버십은 한 번만 materialize 된다")
  void concurrentProjectCreateAndWorkspaceMemberAdd_materializesMembershipOnce()
      throws InterruptedException {
    User admin = signUpUser("admin-project-add-race@test.com", "Admin");
    User target = signUpUser("target-project-add-race@test.com", "Target");
    Workspace workspace = saveWorkspace("Project Add Race", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);

    ConcurrentLinkedQueue<Throwable> unexpectedErrors = runConcurrently(
        () -> createProjectUseCase.createProject(new CreateProjectCommand(
            workspace.getId(), "Project Add Race Created", "Description", admin.id()))
            .block(),
        () -> addWorkspaceMemberUseCase.addWorkspaceMember(
            new AddWorkspaceMemberCommand(workspace.getId(), target.email(),
                WorkspaceRole.MEMBER, admin.id()))
            .block());

    if (!unexpectedErrors.isEmpty()) {
      fail("Unexpected errors: " + unexpectedErrors);
    }

    Project project = findProjectByName(workspace.getId(), "Project Add Race Created");
    ProjectMember member = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(), target.id())
        .block();

    assertThat(member).isNotNull();
    assertThat(member.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
    assertThat(countProjectMemberRows(project.getId(), target.id())).isEqualTo(1);
  }

  @Test
  @DisplayName("프로젝트 생성과 워크스페이스 초대 수락이 동시에 일어나도 프로젝트 멤버십은 한 번만 materialize 된다")
  void concurrentProjectCreateAndWorkspaceInvitationAccept_materializesMembershipOnce()
      throws InterruptedException {
    User admin = signUpUser("admin-project-accept-race@test.com", "Admin");
    User invitee = signUpUser("invitee-project-accept-race@test.com", "Invitee");
    Workspace workspace = saveWorkspace("Project Accept Race", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Invitation invitation = saveWorkspaceInvitation(workspace, invitee.email(),
        WorkspaceRole.MEMBER, admin);

    ConcurrentLinkedQueue<Throwable> unexpectedErrors = runConcurrently(
        () -> createProjectUseCase.createProject(new CreateProjectCommand(
            workspace.getId(), "Project Accept Race Created", "Description", admin.id()))
            .block(),
        () -> acceptWorkspaceInvitationUseCase.acceptWorkspaceInvitation(
            new AcceptWorkspaceInvitationCommand(invitation.getId(), invitee.id()))
            .block());

    if (!unexpectedErrors.isEmpty()) {
      fail("Unexpected errors: " + unexpectedErrors);
    }

    Project project = findProjectByName(workspace.getId(), "Project Accept Race Created");
    ProjectMember member = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(), invitee.id())
        .block();

    assertThat(member).isNotNull();
    assertThat(member.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
    assertThat(countProjectMemberRows(project.getId(), invitee.id())).isEqualTo(1);
  }

  @Test
  @DisplayName("프로젝트 생성과 워크스페이스 ADMIN 승격이 동시에 일어나도 새 프로젝트 멤버십은 ADMIN으로 수렴한다")
  void concurrentProjectCreateAndWorkspaceRolePromotion_materializesAdminMembership()
      throws InterruptedException {
    User admin = signUpUser("admin-project-role-race@test.com", "Admin");
    User target = signUpUser("target-project-role-race@test.com", "Target");
    Workspace workspace = saveWorkspace("Project Role Race", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, target, WorkspaceRole.MEMBER);

    ConcurrentLinkedQueue<Throwable> unexpectedErrors = runConcurrently(
        () -> createProjectUseCase.createProject(new CreateProjectCommand(
            workspace.getId(), "Project Role Race Created", "Description",
            admin.id()))
            .block(),
        () -> updateWorkspaceMemberRoleUseCase.updateWorkspaceMemberRole(
            new UpdateWorkspaceMemberRoleCommand(workspace.getId(), target.id(),
                WorkspaceRole.ADMIN, admin.id()))
            .block());

    if (!unexpectedErrors.isEmpty()) {
      fail("Unexpected errors: " + unexpectedErrors);
    }

    Project project = findProjectByName(workspace.getId(), "Project Role Race Created");
    ProjectMember member = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(), target.id())
        .block();

    assertThat(member).isNotNull();
    assertThat(member.getRoleAsEnum()).isEqualTo(ProjectRole.ADMIN);
    assertThat(countProjectMemberRows(project.getId(), target.id())).isEqualTo(1);
  }

  @Test
  @DisplayName("프로젝트 생성과 워크스페이스 멤버 제거가 동시에 일어나도 새 프로젝트에 활성 멤버십이 남지 않는다")
  void concurrentProjectCreateAndWorkspaceMemberRemove_leavesNoActiveMembership()
      throws InterruptedException {
    User admin = signUpUser("admin-project-remove-race@test.com", "Admin");
    User target = signUpUser("target-project-remove-race@test.com", "Target");
    Workspace workspace = saveWorkspace("Project Remove Race", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, target, WorkspaceRole.MEMBER);

    ConcurrentLinkedQueue<Throwable> unexpectedErrors = runConcurrently(
        () -> createProjectUseCase.createProject(new CreateProjectCommand(
            workspace.getId(), "Project Remove Race Created", "Description",
            admin.id()))
            .block(),
        () -> removeWorkspaceMemberUseCase.removeWorkspaceMember(
            new RemoveWorkspaceMemberCommand(workspace.getId(), target.id(),
                admin.id()))
            .block());

    if (!unexpectedErrors.isEmpty()) {
      fail("Unexpected errors: " + unexpectedErrors);
    }

    Project project = findProjectByName(workspace.getId(), "Project Remove Race Created");
    ProjectMember activeMember = projectMemberRepository
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(), target.id())
        .block();

    assertThat(activeMember).isNull();
    assertThat(countProjectMemberRows(project.getId(), target.id()))
        .isLessThanOrEqualTo(1);
  }

  @Test
  @DisplayName("두 프로젝트 멤버가 동시에 나가도 마지막 leave가 프로젝트 삭제로 수렴한다")
  void concurrentProjectMembersLeave_deletesProjectWhenLastLeaveWins()
      throws InterruptedException {
    User workspaceAdmin = signUpUser("admin-project-leave-race@test.com",
        "Admin");
    User firstMember = signUpUser("first-project-leave-race@test.com",
        "First");
    User secondMember = signUpUser("second-project-leave-race@test.com",
        "Second");
    Workspace workspace = saveWorkspace("Project Leave Race", "Description");
    saveWorkspaceMember(workspace, workspaceAdmin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, firstMember, WorkspaceRole.MEMBER);
    saveWorkspaceMember(workspace, secondMember, WorkspaceRole.MEMBER);
    Project project = saveProject(workspace, "Project Leave Race Created");
    ProjectMember firstProjectMember = saveProjectMember(project, firstMember,
        ProjectRole.VIEWER);
    ProjectMember secondProjectMember = saveProjectMember(project, secondMember,
        ProjectRole.VIEWER);

    ConcurrentLinkedQueue<Throwable> unexpectedErrors = runConcurrently(
        () -> leaveProjectUseCase.leaveProject(new LeaveProjectCommand(
            project.getId(), firstMember.id())).block(),
        () -> leaveProjectUseCase.leaveProject(new LeaveProjectCommand(
            project.getId(), secondMember.id())).block());

    if (!unexpectedErrors.isEmpty()) {
      fail("Unexpected errors: " + unexpectedErrors);
    }

    assertThat(projectRepository.findByIdAndNotDeleted(project.getId()).block())
        .isNull();
    assertThat(countActiveProjectMemberRows(project.getId())).isZero();
    assertThat(projectMemberRepository.findById(firstProjectMember.getId())
        .block().isDeleted()).isTrue();
    assertThat(projectMemberRepository.findById(secondProjectMember.getId())
        .block().isDeleted()).isTrue();
  }

  @Test
  @DisplayName("프로젝트 초대 생성과 워크스페이스 멤버 추가가 동시에 일어나도 pending 프로젝트 초대가 남지 않는다")
  void concurrentProjectInvitationCreateAndWorkspaceMemberAdd_leavesNoPendingInvitation()
      throws InterruptedException {
    User admin = signUpUser("admin-project-invite-add-race@test.com", "Admin");
    User target = signUpUser("target-project-invite-add-race@test.com", "Target");
    Workspace workspace = saveWorkspace("Project Invite Add Race", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Project project = saveProject(workspace, "Project Invite Add Race Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);

    ConcurrentLinkedQueue<Throwable> unexpectedErrors = runConcurrently(
        () -> createProjectInvitationIgnoringExpectedMembershipRace(
            project.getId(), target.email(), admin.id()),
        () -> addWorkspaceMemberUseCase.addWorkspaceMember(
            new AddWorkspaceMemberCommand(workspace.getId(), target.email(),
                WorkspaceRole.MEMBER, admin.id()))
            .block());

    if (!unexpectedErrors.isEmpty()) {
      fail("Unexpected errors: " + unexpectedErrors);
    }

    assertThat(countActiveProjectMemberRows(project.getId(), target.id()))
        .isEqualTo(1);
    assertThat(countUnexpiredPendingProjectInvitations(project.getId(),
        target.email())).isZero();
  }

  @Test
  @DisplayName("프로젝트 초대 생성과 워크스페이스 초대 수락이 동시에 일어나도 pending 프로젝트 초대가 남지 않는다")
  void concurrentProjectInvitationCreateAndWorkspaceInvitationAccept_leavesNoPendingInvitation()
      throws InterruptedException {
    User admin = signUpUser("admin-project-invite-accept-race@test.com",
        "Admin");
    User invitee = signUpUser("invitee-project-invite-accept-race@test.com",
        "Invitee");
    Workspace workspace = saveWorkspace("Project Invite Accept Race",
        "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Project project = saveProject(workspace,
        "Project Invite Accept Race Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    Invitation workspaceInvitation = saveWorkspaceInvitation(workspace,
        invitee.email(), WorkspaceRole.MEMBER, admin);

    ConcurrentLinkedQueue<Throwable> unexpectedErrors = runConcurrently(
        () -> createProjectInvitationIgnoringExpectedMembershipRace(
            project.getId(), invitee.email(), admin.id()),
        () -> acceptWorkspaceInvitationUseCase.acceptWorkspaceInvitation(
            new AcceptWorkspaceInvitationCommand(workspaceInvitation.getId(),
                invitee.id()))
            .block());

    if (!unexpectedErrors.isEmpty()) {
      fail("Unexpected errors: " + unexpectedErrors);
    }

    assertThat(countActiveProjectMemberRows(project.getId(), invitee.id()))
        .isEqualTo(1);
    assertThat(countUnexpiredPendingProjectInvitations(project.getId(),
        invitee.email())).isZero();
  }

  @Test
  @DisplayName("프로젝트 초대 생성과 프로젝트 삭제가 동시에 일어나도 삭제된 프로젝트에 pending 초대가 남지 않는다")
  void concurrentProjectInvitationCreateAndProjectDelete_leavesNoPendingInvitation()
      throws InterruptedException {
    User admin = signUpUser("admin-project-invite-delete-race@test.com",
        "Admin");
    User invitee = signUpUser("invitee-project-invite-delete-race@test.com",
        "Invitee");
    Workspace workspace = saveWorkspace("Project Invite Delete Race",
        "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    Project project = saveProject(workspace,
        "Project Invite Delete Race Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);

    ConcurrentLinkedQueue<Throwable> unexpectedErrors = runConcurrently(
        () -> createProjectInvitationIgnoringExpectedProjectDeleteRace(
            project.getId(), invitee.email(), admin.id()),
        () -> deleteProjectUseCase.deleteProject(new DeleteProjectCommand(
            project.getId(), admin.id()))
            .block());

    if (!unexpectedErrors.isEmpty()) {
      fail("Unexpected errors: " + unexpectedErrors);
    }

    Project deletedProject = projectRepository.findById(project.getId()).block();
    assertThat(deletedProject).isNotNull();
    assertThat(deletedProject.isDeleted()).isTrue();
    assertThat(countLivePendingProjectInvitations(project.getId(),
        invitee.email())).isZero();
  }

  private long countInvitations(
      InvitationType targetType,
      String targetId,
      String email,
      InvitationStatus status) {
    return invitationRepository.countByTargetAndEmailAndStatus(
        targetType.name(), targetId, email, status.name())
        .block();
  }

  private ConcurrentLinkedQueue<Throwable> runConcurrently(
      int taskCount,
      CheckedTask task) throws InterruptedException {
    CheckedTask[] tasks = new CheckedTask[taskCount];
    for (int i = 0; i < taskCount; i++) {
      tasks[i] = task;
    }
    return runConcurrently(tasks);
  }

  private ConcurrentLinkedQueue<Throwable> runConcurrently(CheckedTask... tasks)
      throws InterruptedException {
    CountDownLatch readyLatch = new CountDownLatch(tasks.length);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(tasks.length);
    ConcurrentLinkedQueue<Throwable> unexpectedErrors = new ConcurrentLinkedQueue<>();

    try (ExecutorService executor = Executors.newFixedThreadPool(tasks.length)) {
      for (CheckedTask task : tasks) {
        executor.submit(() -> {
          readyLatch.countDown();
          await(startLatch);
          try {
            task.run();
          } catch (Throwable error) {
            unexpectedErrors.add(error);
          } finally {
            doneLatch.countDown();
          }
        });
      }

      readyLatch.await();
      startLatch.countDown();
      doneLatch.await();
    }

    return unexpectedErrors;
  }

  private Project findProjectByName(String workspaceId, String name) {
    return projectRepository.findByWorkspaceIdAndNotDeleted(workspaceId)
        .filter(project -> project.getName().equals(name))
        .blockFirst();
  }

  private long countProjectMemberRows(String projectId, String userId) {
    return databaseClient.sql("""
        SELECT COUNT(*) FROM project_members
        WHERE project_id = :projectId AND user_id = :userId
        """)
        .bind("projectId", projectId)
        .bind("userId", userId)
        .map(row -> row.get(0, Long.class))
        .one()
        .block();
  }

  private long countActiveProjectMemberRows(String projectId, String userId) {
    return databaseClient.sql("""
        SELECT COUNT(*) FROM project_members
        WHERE project_id = :projectId
          AND user_id = :userId
          AND deleted_at IS NULL
        """)
        .bind("projectId", projectId)
        .bind("userId", userId)
        .map(row -> row.get(0, Long.class))
        .one()
        .block();
  }

  private long countActiveProjectMemberRows(String projectId) {
    return databaseClient.sql("""
        SELECT COUNT(*) FROM project_members
        WHERE project_id = :projectId
          AND deleted_at IS NULL
        """)
        .bind("projectId", projectId)
        .map(row -> row.get(0, Long.class))
        .one()
        .block();
  }

  private long countUnexpiredPendingProjectInvitations(
      String projectId,
      String email) {
    return databaseClient.sql("""
        SELECT COUNT(*) FROM invitations
        WHERE target_type = 'PROJECT'
          AND target_id = :projectId
          AND invited_email = :email
          AND status = 'PENDING'
          AND expires_at > CURRENT_TIMESTAMP
          AND deleted_at IS NULL
        """)
        .bind("projectId", projectId)
        .bind("email", email)
        .map(row -> row.get(0, Long.class))
        .one()
        .block();
  }

  private long countLivePendingProjectInvitations(
      String projectId,
      String email) {
    return databaseClient.sql("""
        SELECT COUNT(*) FROM invitations
        WHERE target_type = 'PROJECT'
          AND target_id = :projectId
          AND invited_email = :email
          AND status = 'PENDING'
          AND deleted_at IS NULL
        """)
        .bind("projectId", projectId)
        .bind("email", email)
        .map(row -> row.get(0, Long.class))
        .one()
        .block();
  }

  private void createProjectInvitationIgnoringExpectedMembershipRace(
      String projectId,
      String email,
      String requesterId) {
    try {
      createProjectInvitationUseCase.createProjectInvitation(
          new CreateProjectInvitationCommand(projectId, email,
              ProjectRole.EDITOR, requesterId))
          .block();
    } catch (DomainException error) {
      if (error.getErrorCode() != ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT) {
        throw error;
      }
    }
  }

  private void createProjectInvitationIgnoringExpectedProjectDeleteRace(
      String projectId,
      String email,
      String requesterId) {
    try {
      createProjectInvitationUseCase.createProjectInvitation(
          new CreateProjectInvitationCommand(projectId, email,
              ProjectRole.EDITOR, requesterId))
          .block();
    } catch (DomainException error) {
      if (error.getErrorCode() != ProjectErrorCode.NOT_FOUND
          && error.getErrorCode() != ProjectErrorCode.ACCESS_DENIED) {
        throw error;
      }
    }
  }

  private void updateInvitationExpiration(String invitationId, Instant expiresAt) {
    databaseClient.sql("""
        UPDATE invitations
        SET expires_at = :expiresAt
        WHERE id = :id
        """)
        .bind("expiresAt", expiresAt)
        .bind("id", invitationId)
        .fetch()
        .rowsUpdated()
        .block();
  }

  private void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  @FunctionalInterface
  private interface CheckedTask {

    void run() throws Exception;

  }

}
