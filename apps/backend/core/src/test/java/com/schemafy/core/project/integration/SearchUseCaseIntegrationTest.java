package com.schemafy.core.project.integration;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.MemberSearchResult;
import com.schemafy.core.project.application.port.in.ProjectSearchResult;
import com.schemafy.core.project.application.port.in.SearchProjectMembersQuery;
import com.schemafy.core.project.application.port.in.SearchProjectMembersUseCase;
import com.schemafy.core.project.application.port.in.SearchSharedProjectsQuery;
import com.schemafy.core.project.application.port.in.SearchSharedProjectsUseCase;
import com.schemafy.core.project.application.port.in.SearchWorkspaceMembersQuery;
import com.schemafy.core.project.application.port.in.SearchWorkspaceMembersUseCase;
import com.schemafy.core.project.application.port.in.SearchWorkspaceProjectsQuery;
import com.schemafy.core.project.application.port.in.SearchWorkspaceProjectsUseCase;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;
import com.schemafy.core.user.domain.User;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("프로젝트 검색 유스케이스 통합 테스트")
class SearchUseCaseIntegrationTest extends ProjectDomainIntegrationSupport {

  @Autowired
  private SearchWorkspaceProjectsUseCase searchWorkspaceProjectsUseCase;

  @Autowired
  private SearchSharedProjectsUseCase searchSharedProjectsUseCase;

  @Autowired
  private SearchWorkspaceMembersUseCase searchWorkspaceMembersUseCase;

  @Autowired
  private SearchProjectMembersUseCase searchProjectMembersUseCase;

  @Test
  @DisplayName("워크스페이스 검색은 요청자의 활성 프로젝트 멤버십에서 이름을 필터링한 뒤 페이지와 역할을 반환한다")
  void searchWorkspaceProjects_filtersBeforePagingAndReturnsRequesterRoles() {
    User requester = signUpUser("workspace-search-requester@test.com", "Requester");
    User otherUser = signUpUser("workspace-search-other@test.com", "Other");
    Workspace workspace = saveWorkspace("Workspace Search", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.MEMBER);

    Project olderMatch = saveProject(workspace, "Alpha Search Project");
    saveProjectMember(olderMatch, requester, ProjectRole.VIEWER);
    Project nonMatch = saveProject(workspace, "Unrelated Project");
    saveProjectMember(nonMatch, requester, ProjectRole.ADMIN);
    Project hiddenMatch = saveProject(workspace, "Search Hidden Project");
    saveProjectMember(hiddenMatch, otherUser, ProjectRole.ADMIN);
    Project newerMatch = saveProject(workspace, "SEARCH Omega");
    saveProjectMember(newerMatch, requester, ProjectRole.EDITOR);

    PageResult<ProjectSearchResult> firstPage = searchWorkspaceProjectsUseCase
        .searchWorkspaceProjects(new SearchWorkspaceProjectsQuery(
            workspace.getId(), requester.id(), "sEaRcH", 0, 1))
        .block();
    PageResult<ProjectSearchResult> secondPage = searchWorkspaceProjectsUseCase
        .searchWorkspaceProjects(new SearchWorkspaceProjectsQuery(
            workspace.getId(), requester.id(), "sEaRcH", 1, 1))
        .block();

    assertThat(firstPage).isNotNull();
    assertThat(firstPage.content()).singleElement().satisfies(result -> {
      assertThat(result.name()).isEqualTo(newerMatch.getName());
      assertThat(result.requesterRole()).isEqualTo(ProjectRole.EDITOR.name());
    });
    assertThat(firstPage.totalElements()).isEqualTo(2);
    assertThat(firstPage.totalPages()).isEqualTo(2);

    assertThat(secondPage).isNotNull();
    assertThat(secondPage.content()).singleElement().satisfies(result -> {
      assertThat(result.name()).isEqualTo(olderMatch.getName());
      assertThat(result.requesterRole()).isEqualTo(ProjectRole.VIEWER.name());
    });
    assertThat(secondPage.totalElements()).isEqualTo(2);
    assertThat(secondPage.totalPages()).isEqualTo(2);
  }

  @Test
  @DisplayName("워크스페이스 비회원의 프로젝트 검색은 거부된다")
  void searchWorkspaceProjects_deniesRequesterWithoutWorkspaceMembership() {
    User requester = signUpUser("workspace-search-denied@test.com", "Requester");
    Workspace workspace = saveWorkspace("Workspace Search Denied", "Description");
    Project project = saveProject(workspace, "Search Project");
    saveProjectMember(project, requester, ProjectRole.EDITOR);

    StepVerifier.create(searchWorkspaceProjectsUseCase.searchWorkspaceProjects(
        new SearchWorkspaceProjectsQuery(
            workspace.getId(), requester.id(), "search", 0, 10)))
        .expectErrorMatches(DomainException.hasErrorCode(WorkspaceErrorCode.ACCESS_DENIED))
        .verify();
  }

  @Test
  @DisplayName("공유 검색은 워크스페이스 비회원의 직접 프로젝트 멤버십만 필터링한 뒤 페이지와 역할을 반환한다")
  void searchSharedProjects_filtersDirectMembershipsBeforePaging() {
    User requester = signUpUser("shared-search-requester@test.com", "Requester");
    User owner = signUpUser("shared-search-owner@test.com", "Owner");

    Workspace sharedWorkspace = saveWorkspace("Shared Search", "Description");
    saveWorkspaceMember(sharedWorkspace, owner, WorkspaceRole.ADMIN);
    Project olderMatch = saveProject(sharedWorkspace, "Alpha Shared Search");
    saveProjectMember(olderMatch, requester, ProjectRole.VIEWER);
    Project nonMatch = saveProject(sharedWorkspace, "Unrelated Shared Project");
    saveProjectMember(nonMatch, requester, ProjectRole.ADMIN);
    Project newerMatch = saveProject(sharedWorkspace, "SHARED SEARCH Omega");
    saveProjectMember(newerMatch, requester, ProjectRole.EDITOR);

    Workspace joinedWorkspace = saveWorkspace("Joined Search", "Description");
    saveWorkspaceMember(joinedWorkspace, requester, WorkspaceRole.MEMBER);
    Project joinedMatch = saveProject(joinedWorkspace, "Shared Search Excluded");
    saveProjectMember(joinedMatch, requester, ProjectRole.VIEWER);

    PageResult<ProjectSearchResult> firstPage = searchSharedProjectsUseCase
        .searchSharedProjects(new SearchSharedProjectsQuery(
            requester.id(), "sHaReD sEaRcH", 0, 1))
        .block();
    PageResult<ProjectSearchResult> secondPage = searchSharedProjectsUseCase
        .searchSharedProjects(new SearchSharedProjectsQuery(
            requester.id(), "sHaReD sEaRcH", 1, 1))
        .block();

    assertThat(firstPage).isNotNull();
    assertThat(firstPage.content()).singleElement().satisfies(result -> {
      assertThat(result.name()).isEqualTo(newerMatch.getName());
      assertThat(result.requesterRole()).isEqualTo(ProjectRole.EDITOR.name());
    });
    assertThat(firstPage.totalElements()).isEqualTo(2);
    assertThat(firstPage.totalPages()).isEqualTo(2);

    assertThat(secondPage).isNotNull();
    assertThat(secondPage.content()).singleElement().satisfies(result -> {
      assertThat(result.name()).isEqualTo(olderMatch.getName());
      assertThat(result.requesterRole()).isEqualTo(ProjectRole.VIEWER.name());
    });
    assertThat(secondPage.totalElements()).isEqualTo(2);
    assertThat(secondPage.totalPages()).isEqualTo(2);
  }

  @Test
  @DisplayName("삭제된 프로젝트와 요청자의 삭제된 프로젝트 멤버십은 워크스페이스 및 공유 검색 결과와 개수에서 제외된다")
  void searchProjects_excludesDeletedProjectsAndRequesterMemberships() {
    User requester = signUpUser("deleted-search-requester@test.com", "Requester");

    Workspace workspace = saveWorkspace("Deleted Workspace Search", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.MEMBER);
    Project activeWorkspaceProject = saveProject(workspace, "Search Active Workspace");
    saveProjectMember(activeWorkspaceProject, requester, ProjectRole.ADMIN);
    Project deletedWorkspaceProject = saveProject(workspace, "Search Deleted Workspace Project");
    saveProjectMember(deletedWorkspaceProject, requester, ProjectRole.EDITOR);
    softDeleteProject(deletedWorkspaceProject.getId());
    Project deletedWorkspaceMembershipProject = saveProject(
        workspace, "Search Deleted Workspace Membership");
    ProjectMember deletedWorkspaceMembership = saveProjectMember(
        deletedWorkspaceMembershipProject, requester, ProjectRole.VIEWER);
    softDeleteProjectMember(deletedWorkspaceMembership.getId());

    Workspace sharedWorkspace = saveWorkspace("Deleted Shared Search", "Description");
    Project activeSharedProject = saveProject(sharedWorkspace, "Search Active Shared");
    saveProjectMember(activeSharedProject, requester, ProjectRole.VIEWER);
    Project deletedSharedProject = saveProject(sharedWorkspace, "Search Deleted Shared Project");
    saveProjectMember(deletedSharedProject, requester, ProjectRole.EDITOR);
    softDeleteProject(deletedSharedProject.getId());
    Project deletedSharedMembershipProject = saveProject(
        sharedWorkspace, "Search Deleted Shared Membership");
    ProjectMember deletedSharedMembership = saveProjectMember(
        deletedSharedMembershipProject, requester, ProjectRole.ADMIN);
    softDeleteProjectMember(deletedSharedMembership.getId());

    PageResult<ProjectSearchResult> workspaceResult = searchWorkspaceProjectsUseCase
        .searchWorkspaceProjects(new SearchWorkspaceProjectsQuery(
            workspace.getId(), requester.id(), "search", 0, 10))
        .block();
    PageResult<ProjectSearchResult> sharedResult = searchSharedProjectsUseCase
        .searchSharedProjects(new SearchSharedProjectsQuery(
            requester.id(), "search", 0, 10))
        .block();

    assertThat(workspaceResult).isNotNull();
    assertThat(workspaceResult.content()).singleElement().satisfies(result -> {
      assertThat(result.id()).isEqualTo(activeWorkspaceProject.getId());
      assertThat(result.requesterRole()).isEqualTo(ProjectRole.ADMIN.name());
    });
    assertThat(workspaceResult.totalElements()).isEqualTo(1);

    assertThat(sharedResult).isNotNull();
    assertThat(sharedResult.content()).singleElement().satisfies(result -> {
      assertThat(result.id()).isEqualTo(activeSharedProject.getId());
      assertThat(result.requesterRole()).isEqualTo(ProjectRole.VIEWER.name());
    });
    assertThat(sharedResult.totalElements()).isEqualTo(1);
  }

  @Test
  @DisplayName("워크스페이스 멤버 검색은 이름과 이메일을 필터링한 뒤 페이지와 전체 개수를 반환한다")
  void searchWorkspaceMembers_matchesNameAndEmailBeforePaging() {
    User requester = signUpUser(
        "workspace-member-requester@test.com", "Workspace Requester");
    User alice = signUpUser(
        "alice@workspace-member.test", "Alice Engineer");
    User bob = signUpUser(
        "bob@workspace-member.test", "Bob Designer");
    Workspace workspace = saveWorkspace("Workspace Member Search", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.MEMBER);
    saveWorkspaceMember(workspace, alice, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, bob, WorkspaceRole.MEMBER);

    PageResult<MemberSearchResult> byName = searchWorkspaceMembersUseCase
        .searchWorkspaceMembers(new SearchWorkspaceMembersQuery(
            workspace.getId(), requester.id(), "aLi", 0, 5))
        .block();
    PageResult<MemberSearchResult> byEmail = searchWorkspaceMembersUseCase
        .searchWorkspaceMembers(new SearchWorkspaceMembersQuery(
            workspace.getId(), requester.id(), "BOB@WORKSPACE-MEMBER", 0, 5))
        .block();
    PageResult<MemberSearchResult> secondPage = searchWorkspaceMembersUseCase
        .searchWorkspaceMembers(new SearchWorkspaceMembersQuery(
            workspace.getId(), requester.id(), "@workspace-member.test", 1, 1))
        .block();

    assertThat(byName).isNotNull();
    assertThat(byName.content())
        .extracting(MemberSearchResult::userId)
        .containsExactly(alice.id());
    assertThat(byName.totalElements()).isEqualTo(1);

    assertThat(byEmail).isNotNull();
    assertThat(byEmail.content())
        .extracting(MemberSearchResult::userId)
        .containsExactly(bob.id());
    assertThat(byEmail.totalElements()).isEqualTo(1);

    assertThat(secondPage).isNotNull();
    assertThat(secondPage.content())
        .extracting(MemberSearchResult::userId)
        .containsExactly(bob.id());
    assertThat(secondPage.totalElements()).isEqualTo(2);
    assertThat(secondPage.totalPages()).isEqualTo(2);
  }

  @Test
  @DisplayName("프로젝트 멤버 검색은 이름과 이메일을 필터링한 뒤 페이지와 전체 개수를 반환한다")
  void searchProjectMembers_matchesNameAndEmailBeforePaging() {
    User requester = signUpUser(
        "project-member-requester@test.com", "Project Requester");
    User alice = signUpUser(
        "alice@project-member.test", "Alice Engineer");
    User bob = signUpUser(
        "bob@project-member.test", "Bob Designer");
    Workspace workspace = saveWorkspace("Project Member Workspace", "Description");
    Project project = saveProject(workspace, "Project Member Search");
    saveProjectMember(project, requester, ProjectRole.VIEWER);
    saveProjectMember(project, alice, ProjectRole.ADMIN);
    saveProjectMember(project, bob, ProjectRole.EDITOR);

    PageResult<MemberSearchResult> byName = searchProjectMembersUseCase
        .searchProjectMembers(new SearchProjectMembersQuery(
            project.getId(), requester.id(), "aLi", 0, 5))
        .block();
    PageResult<MemberSearchResult> byEmail = searchProjectMembersUseCase
        .searchProjectMembers(new SearchProjectMembersQuery(
            project.getId(), requester.id(), "BOB@PROJECT-MEMBER", 0, 5))
        .block();
    PageResult<MemberSearchResult> secondPage = searchProjectMembersUseCase
        .searchProjectMembers(new SearchProjectMembersQuery(
            project.getId(), requester.id(), "@project-member.test", 1, 1))
        .block();

    assertThat(byName).isNotNull();
    assertThat(byName.content())
        .extracting(MemberSearchResult::userId)
        .containsExactly(alice.id());
    assertThat(byName.totalElements()).isEqualTo(1);

    assertThat(byEmail).isNotNull();
    assertThat(byEmail.content())
        .extracting(MemberSearchResult::userId)
        .containsExactly(bob.id());
    assertThat(byEmail.totalElements()).isEqualTo(1);

    assertThat(secondPage).isNotNull();
    assertThat(secondPage.content())
        .extracting(MemberSearchResult::userId)
        .containsExactly(bob.id());
    assertThat(secondPage.totalElements()).isEqualTo(2);
    assertThat(secondPage.totalPages()).isEqualTo(2);
  }

  @Test
  @DisplayName("삭제된 워크스페이스 및 프로젝트 멤버십은 멤버 검색 결과와 개수에서 제외된다")
  void searchMembers_excludesSoftDeletedMemberships() {
    User requester = signUpUser(
        "deleted-member-requester@test.com", "Deleted Member Requester");
    User active = signUpUser(
        "active@deleted-member.test", "Search Active");
    User deleted = signUpUser(
        "deleted@deleted-member.test", "Search Deleted");
    Workspace workspace = saveWorkspace("Deleted Member Workspace", "Description");
    saveWorkspaceMember(workspace, requester, WorkspaceRole.MEMBER);
    saveWorkspaceMember(workspace, active, WorkspaceRole.MEMBER);
    WorkspaceMember deletedWorkspaceMember = saveWorkspaceMember(
        workspace, deleted, WorkspaceRole.MEMBER);
    softDeleteWorkspaceMember(deletedWorkspaceMember.getId());

    Project project = saveProject(workspace, "Deleted Member Project");
    saveProjectMember(project, requester, ProjectRole.VIEWER);
    saveProjectMember(project, active, ProjectRole.EDITOR);
    ProjectMember deletedProjectMember = saveProjectMember(
        project, deleted, ProjectRole.VIEWER);
    softDeleteProjectMember(deletedProjectMember.getId());

    PageResult<MemberSearchResult> workspaceResult = searchWorkspaceMembersUseCase
        .searchWorkspaceMembers(new SearchWorkspaceMembersQuery(
            workspace.getId(), requester.id(), "search", 0, 10))
        .block();
    PageResult<MemberSearchResult> projectResult = searchProjectMembersUseCase
        .searchProjectMembers(new SearchProjectMembersQuery(
            project.getId(), requester.id(), "search", 0, 10))
        .block();

    assertThat(workspaceResult).isNotNull();
    assertThat(workspaceResult.content())
        .extracting(MemberSearchResult::userId)
        .containsExactly(active.id());
    assertThat(workspaceResult.totalElements()).isEqualTo(1);

    assertThat(projectResult).isNotNull();
    assertThat(projectResult.content())
        .extracting(MemberSearchResult::userId)
        .containsExactly(active.id());
    assertThat(projectResult.totalElements()).isEqualTo(1);
  }

  @Test
  @DisplayName("멤버가 아닌 요청자의 워크스페이스 및 프로젝트 멤버 검색은 거부된다")
  void searchMembers_deniesRequestersWithoutRequiredMemberships() {
    User requester = signUpUser(
        "member-search-denied@test.com", "Denied Requester");
    Workspace workspace = saveWorkspace("Denied Member Search", "Description");
    Project project = saveProject(workspace, "Denied Member Search");

    StepVerifier.create(searchWorkspaceMembersUseCase.searchWorkspaceMembers(
        new SearchWorkspaceMembersQuery(
            workspace.getId(), requester.id(), "search", 0, 10)))
        .expectErrorMatches(DomainException.hasErrorCode(
            WorkspaceErrorCode.ACCESS_DENIED))
        .verify();

    StepVerifier.create(searchProjectMembersUseCase.searchProjectMembers(
        new SearchProjectMembersQuery(
            project.getId(), requester.id(), "search", 0, 10)))
        .expectErrorMatches(DomainException.hasErrorCode(
            ProjectErrorCode.ACCESS_DENIED))
        .verify();
  }

}
