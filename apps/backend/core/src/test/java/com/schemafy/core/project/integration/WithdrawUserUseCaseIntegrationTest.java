package com.schemafy.core.project.integration;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.project.application.access.ProjectAccessRequesterContext;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;
import com.schemafy.core.ulid.application.service.UlidGenerator;
import com.schemafy.core.user.application.port.in.WithdrawUserCommand;
import com.schemafy.core.user.application.port.in.WithdrawUserUseCase;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.UserStatus;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("계정 탈퇴 유스케이스 통합 테스트")
class WithdrawUserUseCaseIntegrationTest extends ProjectDomainIntegrationSupport {

  @Autowired
  private WithdrawUserUseCase withdrawUserUseCase;

  @Autowired
  private FindUserByIdPort findUserByIdPort;

  @Test
  @DisplayName("공유 리소스는 보존하고 본인 관계·프로젝트 전용 membership·초대·MCP만 정리한다")
  void withdrawUser_preservesSharedResourcesAndCleansUserRelations() {
    User user = signUpUser("withdraw-shared@test.com", "Withdrawn");
    User workspaceAdmin = signUpUser("withdraw-shared-admin@test.com", "Admin");
    User projectOnlyOwner = signUpUser("withdraw-project-only-owner@test.com", "Owner");

    Workspace sharedWorkspace = saveWorkspace("Shared workspace", "description");
    WorkspaceMember userWorkspaceMember = saveWorkspaceMember(sharedWorkspace, user,
        WorkspaceRole.MEMBER);
    WorkspaceMember adminWorkspaceMember = saveWorkspaceMember(sharedWorkspace,
        workspaceAdmin, WorkspaceRole.ADMIN);
    var sharedProject = saveProject(sharedWorkspace, "Shared project");
    ProjectMember userProjectMember = saveProjectMember(sharedProject, user,
        ProjectRole.VIEWER);
    ProjectMember adminProjectMember = saveProjectMember(sharedProject,
        workspaceAdmin, ProjectRole.ADMIN);
    createSchema(sharedProject, "shared-schema", workspaceAdmin.id());

    Workspace projectOnlyWorkspace = saveWorkspace("Project-only parent", "description");
    saveWorkspaceMember(projectOnlyWorkspace, projectOnlyOwner, WorkspaceRole.ADMIN);
    var projectOnlyProject = saveProject(projectOnlyWorkspace, "Project-only project");
    ProjectMember projectOnlyMember = saveProjectMember(projectOnlyProject, user,
        ProjectRole.VIEWER);

    Invitation sentInvitation = saveWorkspaceInvitation(sharedWorkspace,
        "sent-to@example.com", WorkspaceRole.MEMBER, user);
    Invitation receivedInvitation = saveWorkspaceInvitation(sharedWorkspace,
        user.email(), WorkspaceRole.MEMBER, workspaceAdmin);
    insertMcpToken(user.id());

    withdrawUserUseCase.withdrawUser(new WithdrawUserCommand(user.id())).block();

    assertThat(workspaceRepository.findByIdAndNotDeleted(sharedWorkspace.getId()).block())
        .isNotNull();
    assertThat(projectRepository.findByIdAndDeletedAtIsNull(sharedProject.getId()).block())
        .isNotNull();
    assertThat(findSchemasByProjectId(sharedProject.getId())).hasSize(1);
    assertThat(workspaceMemberRepository.findById(userWorkspaceMember.getId()).block()
        .isDeleted()).isTrue();
    assertThat(workspaceMemberRepository.findById(adminWorkspaceMember.getId()).block()
        .isDeleted()).isFalse();
    assertThat(projectMemberRepository.findById(userProjectMember.getId()).block()
        .isDeleted()).isTrue();
    assertThat(projectMemberRepository.findById(adminProjectMember.getId()).block()
        .isDeleted()).isFalse();
    assertThat(projectMemberRepository.findById(projectOnlyMember.getId()).block()
        .isDeleted()).isTrue();
    assertThat(invitationRepository.findById(sentInvitation.getId()).block()
        .getStatusAsEnum()).isEqualTo(InvitationStatus.CANCELLED);
    assertThat(invitationRepository.findById(receivedInvitation.getId()).block()
        .getStatusAsEnum()).isEqualTo(InvitationStatus.CANCELLED);
    assertThat(mcpIsRevoked(user.id())).isTrue();

    User withdrawn = findUserByIdPort.findUserById(user.id()).block();
    assertThat(withdrawn.status()).isEqualTo(UserStatus.INACTIVE);
    assertThat(withdrawn.deletedAt()).isNotNull();
  }

  @Test
  @DisplayName("유일 멤버 workspace는 계정 탈퇴 요청 안에서 기존 cascade로 삭제한다")
  void withdrawUser_deletesSoleMemberWorkspace() {
    User user = signUpUser("withdraw-sole@test.com", "Sole member");
    User outsider = signUpUser("withdraw-sole-outsider@test.com", "Outsider");
    Workspace workspace = saveWorkspace("Sole workspace", "description");
    saveWorkspaceMember(workspace, user, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Sole project");
    saveProjectMember(project, user, ProjectRole.ADMIN);
    saveProjectMember(project, outsider, ProjectRole.VIEWER);
    createSchema(project, "sole-schema");

    withdrawUserUseCase.withdrawUser(new WithdrawUserCommand(user.id())).block();

    assertThat(workspaceRepository.findByIdAndNotDeleted(workspace.getId()).block())
        .isNull();
    assertThat(projectRepository.findByIdAndDeletedAtIsNull(project.getId()).block())
        .isNull();
    assertThat(findSchemasByProjectId(project.getId())).isEmpty();
    assertThat(projectMemberRepository.findByProjectIdAndUserIdAndDeletedAtIsNull(
        project.getId(), outsider.id()).block()).isNull();
  }

  @Test
  @DisplayName("유일 ADMIN workspace가 있으면 계정과 관계 데이터를 변경하지 않는다")
  void withdrawUser_blocksSoleAdminBeforeAnyMutation() {
    User user = signUpUser("withdraw-blocked@test.com", "Blocked admin");
    User member = signUpUser("withdraw-blocked-member@test.com", "Member");
    Workspace workspace = saveWorkspace("Blocked workspace", "description");
    saveWorkspaceMember(workspace, user, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, member, WorkspaceRole.MEMBER);
    Invitation invitation = saveWorkspaceInvitation(workspace,
        "pending@example.com", WorkspaceRole.MEMBER, user);
    insertMcpToken(user.id());

    StepVerifier.create(withdrawUserUseCase.withdrawUser(
        new WithdrawUserCommand(user.id())))
        .expectErrorMatches(DomainException
            .hasErrorCode(WorkspaceErrorCode.LAST_ADMIN_CANNOT_LEAVE))
        .verify();

    User unchanged = findUserByIdPort.findUserById(user.id()).block();
    assertThat(unchanged.status()).isEqualTo(UserStatus.ACTIVE);
    assertThat(unchanged.deletedAt()).isNull();
    assertThat(workspaceMemberRepository.findByWorkspaceIdAndUserIdAndDeletedAtIsNull(
        workspace.getId(), user.id()).block()).isNotNull();
    assertThat(invitationRepository.findById(invitation.getId()).block()
        .getStatusAsEnum()).isEqualTo(InvitationStatus.PENDING);
    assertThat(mcpIsRevoked(user.id())).isFalse();
  }

  @Test
  @DisplayName("여러 workspace 중 유일 ADMIN workspace가 있으면 모든 변경을 차단한다")
  void withdrawUser_blocksMixedWorkspacesBeforeAnyMutation() {
    User user = signUpUser("withdraw-mixed@test.com", "Mixed admin");
    User member = signUpUser("withdraw-mixed-member@test.com", "Member");

    Workspace soleAdminWorkspace = saveWorkspace("Sole admin workspace", "description");
    WorkspaceMember soleAdminMembership = saveWorkspaceMember(
        soleAdminWorkspace, user, WorkspaceRole.ADMIN);
    Project soleAdminProject = saveProject(soleAdminWorkspace, "Sole admin project");
    saveProjectMember(soleAdminProject, user, ProjectRole.ADMIN);
    createSchema(soleAdminProject, "sole-admin-schema", user.id());

    Workspace sharedWorkspace = saveWorkspace("Shared workspace", "description");
    WorkspaceMember sharedUserMembership = saveWorkspaceMember(
        sharedWorkspace, user, WorkspaceRole.ADMIN);
    saveWorkspaceMember(sharedWorkspace, member, WorkspaceRole.MEMBER);
    Project sharedProject = saveProject(sharedWorkspace, "Shared project");
    saveProjectMember(sharedProject, user, ProjectRole.ADMIN);
    saveProjectMember(sharedProject, member, ProjectRole.VIEWER);
    createSchema(sharedProject, "shared-schema", user.id());

    StepVerifier.create(withdrawUserUseCase.withdrawUser(
        new WithdrawUserCommand(user.id())))
        .expectErrorMatches(DomainException
            .hasErrorCode(WorkspaceErrorCode.LAST_ADMIN_CANNOT_LEAVE))
        .verify();

    User unchanged = findUserByIdPort.findUserById(user.id()).block();
    assertThat(unchanged.status()).isEqualTo(UserStatus.ACTIVE);
    assertThat(unchanged.deletedAt()).isNull();
    assertThat(workspaceRepository.findByIdAndNotDeleted(soleAdminWorkspace.getId()).block())
        .isNotNull();
    assertThat(workspaceRepository.findByIdAndNotDeleted(sharedWorkspace.getId()).block())
        .isNotNull();
    assertThat(workspaceMemberRepository.findById(soleAdminMembership.getId()).block()
        .isDeleted()).isFalse();
    assertThat(workspaceMemberRepository.findById(sharedUserMembership.getId()).block()
        .isDeleted()).isFalse();
    assertThat(projectRepository.findByIdAndDeletedAtIsNull(soleAdminProject.getId()).block())
        .isNotNull();
    assertThat(projectRepository.findByIdAndDeletedAtIsNull(sharedProject.getId()).block())
        .isNotNull();
    assertThat(findSchemasByProjectId(soleAdminProject.getId())).hasSize(1);
    assertThat(findSchemasByProjectId(sharedProject.getId())).hasSize(1);
  }

  private void insertMcpToken(String userId) {
    databaseClient.sql("""
        INSERT INTO mcp_tokens (id, user_id, scope, issued_at, expires_at)
        VALUES (:id, :userId, 'mcp', CURRENT_TIMESTAMP, :expiresAt)
        """)
        .bind("id", UlidGenerator.generate())
        .bind("userId", userId)
        .bind("expiresAt", Instant.now().plusSeconds(3600))
        .fetch()
        .rowsUpdated()
        .block();
  }

  private boolean mcpIsRevoked(String userId) {
    return databaseClient.sql("""
        SELECT COUNT(*) FROM mcp_tokens
        WHERE user_id = :userId
          AND revoked_at IS NOT NULL
        """)
        .bind("userId", userId)
        .map((row, metadata) -> row.get(0, Long.class))
        .one()
        .map(count -> count > 0)
        .block();
  }

  private void createSchema(Project project, String schemaName,
      String requesterId) {
    createSchemaUseCase.createSchema(new CreateSchemaCommand(
        project.getId(),
        schemaName,
        "utf8mb4",
        "utf8mb4_general_ci"))
        .contextWrite(ProjectAccessRequesterContext.withRequesterId(requesterId))
        .block();
  }

}
