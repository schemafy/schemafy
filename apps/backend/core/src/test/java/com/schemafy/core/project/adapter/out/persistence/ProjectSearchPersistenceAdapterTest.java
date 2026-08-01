package com.schemafy.core.project.adapter.out.persistence;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.config.R2dbcTestConfiguration;
import com.schemafy.core.project.application.port.in.MemberSearchResult;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.ulid.application.service.UlidGenerator;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({ ProjectSearchPersistenceAdapter.class, R2dbcTestConfiguration.class })
@DisplayName("ProjectSearchPersistenceAdapter")
class ProjectSearchPersistenceAdapterTest {

  private static final Integer DB_VENDOR_ID = 1;

  @Autowired
  private ProjectSearchPersistenceAdapter sut;

  @Autowired
  private DatabaseClient databaseClient;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private ProjectMemberRepository projectMemberRepository;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Autowired
  private WorkspaceMemberRepository workspaceMemberRepository;

  @BeforeEach
  void setUp() {
    projectMemberRepository.deleteAll()
        .then(projectRepository.deleteAll())
        .then(workspaceMemberRepository.deleteAll())
        .then(workspaceRepository.deleteAll())
        .then(databaseClient.sql("DELETE FROM users").fetch().rowsUpdated())
        .block();
  }

  @Test
  @DisplayName("워크스페이스 멤버 검색은 사용자 정보를 포함한 read projection을 반환한다")
  void searchWorkspaceMembers_returnsUserProjection() {
    Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
    Workspace workspace = saveWorkspace("Search Workspace");
    UserFixture user = saveUser("alice@example.com", "Alice Engineer");
    WorkspaceMember member = saveWorkspaceMember(workspace, user.id(),
        WorkspaceRole.ADMIN);
    setWorkspaceMemberCreatedAt(member.getId(), createdAt);

    StepVerifier.create(sut.searchWorkspaceMembers(
        workspace.getId(), "ALICE", 0, 5))
        .assertNext(page -> {
          assertThat(page.totalElements()).isEqualTo(1);
          assertThat(page.content()).containsExactly(new MemberSearchResult(
              user.id(), user.name(), user.email(), member.getRole(),
              createdAt));
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("워크스페이스 멤버 검색은 생성 시각이 같으면 멤버 ID 순으로 페이지를 반환한다")
  void searchWorkspaceMembers_ordersEqualCreatedAtByMemberId() {
    Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
    Workspace workspace = saveWorkspace("Stable Workspace Member Search");
    UserFixture firstUser = saveUser(
        "00000000000000000000000011", "first@example.com", "Stable First");
    UserFixture secondUser = saveUser(
        "00000000000000000000000012", "second@example.com", "Stable Second");
    WorkspaceMember higherIdMember = saveWorkspaceMember(
        "00000000000000000000000002", workspace, firstUser.id(),
        WorkspaceRole.MEMBER);
    WorkspaceMember lowerIdMember = saveWorkspaceMember(
        "00000000000000000000000001", workspace, secondUser.id(),
        WorkspaceRole.MEMBER);
    setWorkspaceMemberCreatedAt(higherIdMember.getId(), createdAt);
    setWorkspaceMemberCreatedAt(lowerIdMember.getId(), createdAt);

    StepVerifier.create(sut.searchWorkspaceMembers(
        workspace.getId(), "stable", 0, 1)
        .zipWith(sut.searchWorkspaceMembers(
            workspace.getId(), "stable", 1, 1)))
        .assertNext(pages -> {
          assertThat(pages.getT1().content())
              .extracting(MemberSearchResult::userId)
              .containsExactly(secondUser.id());
          assertThat(pages.getT2().content())
              .extracting(MemberSearchResult::userId)
              .containsExactly(firstUser.id());
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("프로젝트 멤버 검색은 사용자 이메일과 가입 시각을 포함한 read projection을 반환한다")
  void searchProjectMembers_returnsUserProjection() {
    Instant joinedAt = Instant.parse("2025-01-01T00:00:00Z");
    Workspace workspace = saveWorkspace("Project Search Workspace");
    Project project = saveProject(workspace, "Project Search");
    UserFixture user = saveUser("bob@example.com", "Bob Designer");
    ProjectMember member = saveProjectMember(project, user.id(),
        ProjectRole.EDITOR);
    setProjectMemberJoinedAt(member.getId(), joinedAt);

    StepVerifier.create(sut.searchProjectMembers(
        project.getId(), "BOB@EXAMPLE.COM", 0, 5))
        .assertNext(page -> {
          assertThat(page.totalElements()).isEqualTo(1);
          assertThat(page.content()).containsExactly(new MemberSearchResult(
              user.id(), user.name(), user.email(), member.getRole(),
              joinedAt));
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("프로젝트 멤버 검색은 가입 시각이 같으면 멤버 ID 순으로 페이지를 반환한다")
  void searchProjectMembers_ordersEqualJoinedAtByMemberId() {
    Instant joinedAt = Instant.parse("2025-01-01T00:00:00Z");
    Workspace workspace = saveWorkspace("Stable Project Member Workspace");
    Project project = saveProject(workspace, "Stable Project Member Search");
    UserFixture firstUser = saveUser(
        "00000000000000000000000011", "first@example.com", "Stable First");
    UserFixture secondUser = saveUser(
        "00000000000000000000000012", "second@example.com", "Stable Second");
    ProjectMember higherIdMember = saveProjectMember(
        "00000000000000000000000002", project, firstUser.id(),
        ProjectRole.VIEWER);
    ProjectMember lowerIdMember = saveProjectMember(
        "00000000000000000000000001", project, secondUser.id(),
        ProjectRole.EDITOR);
    setProjectMemberJoinedAt(higherIdMember.getId(), joinedAt);
    setProjectMemberJoinedAt(lowerIdMember.getId(), joinedAt);

    StepVerifier.create(sut.searchProjectMembers(
        project.getId(), "stable", 0, 1)
        .zipWith(sut.searchProjectMembers(
            project.getId(), "stable", 1, 1)))
        .assertNext(pages -> {
          assertThat(pages.getT1().content())
              .extracting(MemberSearchResult::userId)
              .containsExactly(secondUser.id());
          assertThat(pages.getT2().content())
              .extracting(MemberSearchResult::userId)
              .containsExactly(firstUser.id());
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("워크스페이스의 프로젝트 검색은 역할과 전체 개수를 같은 페이지 결과로 반환한다")
  void searchWorkspaceProjects_returnsProjectsWithRolesAndCount() {
    Workspace workspace = saveWorkspace("Workspace Project Search");
    String requesterId = UlidGenerator.generate();
    Project olderMatch = saveProject(workspace, "Alpha Search Project");
    saveProjectMember(olderMatch, requesterId, ProjectRole.VIEWER);
    Project nonMatch = saveProject(workspace, "Unrelated Project");
    saveProjectMember(nonMatch, requesterId, ProjectRole.ADMIN);
    Project newerMatch = saveProject(workspace, "SEARCH Omega");
    saveProjectMember(newerMatch, requesterId, ProjectRole.EDITOR);

    StepVerifier.create(sut.searchWorkspaceProjects(
        workspace.getId(), requesterId, "sEaRcH", 0, 1))
        .assertNext(page -> {
          assertThat(page.totalElements()).isEqualTo(2);
          assertThat(page.totalPages()).isEqualTo(2);
          assertThat(page.content()).singleElement().satisfies(result -> {
            assertThat(result.id()).isEqualTo(newerMatch.getId());
            assertThat(result.requesterRole()).isEqualTo(ProjectRole.EDITOR.name());
          });
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("공유 프로젝트 검색은 워크스페이스 멤버십으로 상속된 프로젝트를 제외한다")
  void searchSharedProjects_excludesWorkspaceInheritedProjects() {
    String requesterId = UlidGenerator.generate();
    Workspace sharedWorkspace = saveWorkspace("Shared Workspace");
    Project sharedProject = saveProject(sharedWorkspace, "Shared Search Project");
    saveProjectMember(sharedProject, requesterId, ProjectRole.EDITOR);

    Workspace joinedWorkspace = saveWorkspace("Joined Workspace");
    saveWorkspaceMember(joinedWorkspace, requesterId, WorkspaceRole.MEMBER);
    Project inheritedProject = saveProject(joinedWorkspace,
        "Shared Search Inherited");
    saveProjectMember(inheritedProject, requesterId, ProjectRole.VIEWER);

    StepVerifier.create(sut.searchSharedProjects(
        requesterId, "shared search", 0, 5))
        .assertNext(page -> {
          assertThat(page.totalElements()).isEqualTo(1);
          assertThat(page.content()).singleElement().satisfies(result -> {
            assertThat(result.id()).isEqualTo(sharedProject.getId());
            assertThat(result.requesterRole()).isEqualTo(ProjectRole.EDITOR.name());
          });
        })
        .verifyComplete();
  }

  private Workspace saveWorkspace(String name) {
    return workspaceRepository.save(Workspace.create(
        UlidGenerator.generate(), name, "Description")).block();
  }

  private WorkspaceMember saveWorkspaceMember(Workspace workspace,
      String userId, WorkspaceRole role) {
    return saveWorkspaceMember(UlidGenerator.generate(), workspace, userId, role);
  }

  private WorkspaceMember saveWorkspaceMember(String id, Workspace workspace,
      String userId, WorkspaceRole role) {
    return workspaceMemberRepository.save(
        WorkspaceMember.create(id, workspace.getId(), userId, role)).block();
  }

  private Project saveProject(Workspace workspace, String name) {
    return projectRepository.save(Project.create(UlidGenerator.generate(),
        workspace.getId(), DB_VENDOR_ID, name, "Description")).block();
  }

  private ProjectMember saveProjectMember(Project project, String userId,
      ProjectRole role) {
    return saveProjectMember(UlidGenerator.generate(), project, userId, role);
  }

  private ProjectMember saveProjectMember(String id, Project project,
      String userId, ProjectRole role) {
    return projectMemberRepository.save(ProjectMember.create(
        id, project.getId(), userId, role)).block();
  }

  private void setWorkspaceMemberCreatedAt(String memberId, Instant createdAt) {
    databaseClient.sql("UPDATE workspace_members SET created_at = :createdAt WHERE id = :memberId")
        .bind("createdAt", createdAt)
        .bind("memberId", memberId)
        .fetch()
        .rowsUpdated()
        .block();
  }

  private void setProjectMemberJoinedAt(String memberId, Instant joinedAt) {
    databaseClient.sql("UPDATE project_members SET joined_at = :joinedAt WHERE id = :memberId")
        .bind("joinedAt", joinedAt)
        .bind("memberId", memberId)
        .fetch()
        .rowsUpdated()
        .block();
  }

  private UserFixture saveUser(String email, String name) {
    return saveUser(UlidGenerator.generate(), email, name);
  }

  private UserFixture saveUser(String id, String email, String name) {
    databaseClient.sql("""
        INSERT INTO users (
          id, email, name, password, status, created_at, updated_at
        )
        VALUES (
          :id, :email, :name, NULL, 'ACTIVE',
          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
        .bind("id", id)
        .bind("email", email)
        .bind("name", name)
        .fetch()
        .rowsUpdated()
        .block();
    return new UserFixture(id, email, name);
  }

  private record UserFixture(String id, String email, String name) {
  }

}
