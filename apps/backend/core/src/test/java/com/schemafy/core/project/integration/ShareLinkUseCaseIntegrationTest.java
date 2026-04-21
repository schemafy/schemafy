package com.schemafy.core.project.integration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AccessShareLinkQuery;
import com.schemafy.core.project.application.port.in.AccessShareLinkUseCase;
import com.schemafy.core.project.application.port.in.CreateShareLinkCommand;
import com.schemafy.core.project.application.port.in.CreateShareLinkUseCase;
import com.schemafy.core.project.application.port.in.DeleteShareLinkCommand;
import com.schemafy.core.project.application.port.in.DeleteShareLinkUseCase;
import com.schemafy.core.project.application.port.in.GetShareLinkQuery;
import com.schemafy.core.project.application.port.in.GetShareLinkUseCase;
import com.schemafy.core.project.application.port.in.GetShareLinksQuery;
import com.schemafy.core.project.application.port.in.GetShareLinksUseCase;
import com.schemafy.core.project.application.port.in.RevokeShareLinkCommand;
import com.schemafy.core.project.application.port.in.RevokeShareLinkUseCase;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.project.domain.exception.ShareLinkErrorCode;
import com.schemafy.core.user.domain.User;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("공유 링크 유스케이스 통합 테스트")
class ShareLinkUseCaseIntegrationTest extends ProjectDomainIntegrationSupport {

  @Autowired
  private AccessShareLinkUseCase accessShareLinkUseCase;

  @Autowired
  private CreateShareLinkUseCase createShareLinkUseCase;

  @Autowired
  private GetShareLinksUseCase getShareLinksUseCase;

  @Autowired
  private GetShareLinkUseCase getShareLinkUseCase;

  @Autowired
  private RevokeShareLinkUseCase revokeShareLinkUseCase;

  @Autowired
  private DeleteShareLinkUseCase deleteShareLinkUseCase;

  @Test
  @DisplayName("프로젝트 관리자는 공유 링크를 생성, 단건 조회, 목록 조회할 수 있다")
  void createGetAndListShareLinks() {
    User admin = signUpUser("admin-share@test.com", "Admin");
    var workspace = saveWorkspace("Share WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Share Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);

    ShareLink created = createShareLinkUseCase.createShareLink(
        new CreateShareLinkCommand(project.getId(), admin.id()))
        .block();

    ShareLink fetched = getShareLinkUseCase.getShareLink(
        new GetShareLinkQuery(project.getId(), created.getId(), admin.id()))
        .block();
    PageResult<ShareLink> listed = getShareLinksUseCase.getShareLinks(
        new GetShareLinksQuery(project.getId(), admin.id(), 0, 10))
        .block();

    assertThat(fetched.getId()).isEqualTo(created.getId());
    assertThat(listed.content()).extracting(ShareLink::getId).contains(created.getId());
  }

  @Test
  @DisplayName("공유 링크 생성은 프로젝트 조회자면 거부된다")
  void createShareLink_rejectsProjectViewer() {
    User admin = signUpUser("admin-share-create-auth@test.com", "Admin");
    User viewer = signUpUser("viewer-share-create-auth@test.com", "Viewer");
    var workspace = saveWorkspace("Create Auth Share WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, viewer, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Create Auth Share Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    saveProjectMember(project, viewer, ProjectRole.VIEWER);

    StepVerifier.create(createShareLinkUseCase.createShareLink(
        new CreateShareLinkCommand(project.getId(), viewer.id())))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("공유 링크 목록 조회는 프로젝트 조회자면 거부된다")
  void getShareLinks_rejectsProjectViewer() {
    User admin = signUpUser("admin-share-list-auth@test.com", "Admin");
    User viewer = signUpUser("viewer-share-list-auth@test.com", "Viewer");
    var workspace = saveWorkspace("List Auth Share WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, viewer, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "List Auth Share Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    saveProjectMember(project, viewer, ProjectRole.VIEWER);

    StepVerifier.create(getShareLinksUseCase.getShareLinks(
        new GetShareLinksQuery(project.getId(), viewer.id(), 0, 10)))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("공유 링크 단건 조회는 링크 존재 여부 검증보다 프로젝트 관리자 권한을 먼저 확인한다")
  void getShareLink_rejectsProjectViewerBeforeMissingLinkValidation() {
    User admin = signUpUser("admin-share-get-auth@test.com", "Admin");
    User viewer = signUpUser("viewer-share-get-auth@test.com", "Viewer");
    var workspace = saveWorkspace("Get Auth Share WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, viewer, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Get Auth Share Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    saveProjectMember(project, viewer, ProjectRole.VIEWER);

    StepVerifier.create(getShareLinkUseCase.getShareLink(
        new GetShareLinkQuery(project.getId(), "missing-share-link-id", viewer.id())))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("공유 링크 접근은 프로젝트를 반환하고 접근 횟수를 증가시킨다")
  void accessShareLink_incrementsAccessCount() {
    User admin = signUpUser("admin-share-access@test.com", "Admin");
    var workspace = saveWorkspace("Access WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Access Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    ShareLink link = saveShareLink(project);

    Project accessedProject = accessShareLinkUseCase.accessShareLink(new AccessShareLinkQuery(
        link.getCode(),
        admin.id(),
        "127.0.0.1",
        "JUnit"))
        .block();

    ShareLink updatedLink = shareLinkRepository.findById(link.getId()).block();

    assertThat(accessedProject.getId()).isEqualTo(project.getId());
    assertThat(updatedLink.getAccessCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("공유 링크 접근은 익명 공개 접근을 허용한다")
  void accessShareLink_allowsAnonymousPublicAccess() {
    User admin = signUpUser("admin-share-public@test.com", "Admin");
    var workspace = saveWorkspace("Public Access WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Public Access Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    ShareLink link = saveShareLink(project);

    Project accessedProject = accessShareLinkUseCase.accessShareLink(new AccessShareLinkQuery(
        link.getCode(),
        null,
        "127.0.0.1",
        "JUnit"))
        .block();

    assertThat(accessedProject.getId()).isEqualTo(project.getId());
  }

  @Test
  @DisplayName("공유 링크 접근은 폐기되었거나 만료된 링크를 거부한다")
  void accessShareLink_rejectsInvalidLinks() {
    User admin = signUpUser("admin-share-invalid@test.com", "Admin");
    var workspace = saveWorkspace("Invalid WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Invalid Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    ShareLink revokedLink = saveShareLink(project);
    revokeShareLink(revokedLink.getId());
    ShareLink expiredLink = saveShareLink(project, Instant.now().minus(1, ChronoUnit.DAYS));

    StepVerifier.create(accessShareLinkUseCase.accessShareLink(
        new AccessShareLinkQuery(revokedLink.getCode(), null, null, null)))
        .expectErrorMatches(DomainException.hasErrorCode(ShareLinkErrorCode.INVALID_LINK))
        .verify();

    StepVerifier.create(accessShareLinkUseCase.accessShareLink(
        new AccessShareLinkQuery(expiredLink.getCode(), null, null, null)))
        .expectErrorMatches(DomainException.hasErrorCode(ShareLinkErrorCode.INVALID_LINK))
        .verify();
  }

  @Test
  @DisplayName("공유 링크 폐기와 삭제는 생명주기 상태를 갱신한다")
  void revokeAndDeleteShareLink() {
    User admin = signUpUser("admin-share-delete@test.com", "Admin");
    var workspace = saveWorkspace("Delete Share WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Delete Share Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    ShareLink link = saveShareLink(project);

    ShareLink revoked = revokeShareLinkUseCase.revokeShareLink(
        new RevokeShareLinkCommand(project.getId(), link.getId(), admin.id()))
        .block();
    deleteShareLinkUseCase.deleteShareLink(new DeleteShareLinkCommand(
        project.getId(),
        link.getId(),
        admin.id())).block();

    ShareLink deleted = shareLinkRepository.findById(link.getId()).block();

    assertThat(revoked.getIsRevoked()).isTrue();
    assertThat(deleted.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("공유 링크 폐기는 링크 존재 여부 검증보다 프로젝트 관리자 권한을 먼저 확인한다")
  void revokeShareLink_rejectsProjectViewerBeforeMissingLinkValidation() {
    User admin = signUpUser("admin-share-revoke-auth@test.com", "Admin");
    User viewer = signUpUser("viewer-share-revoke-auth@test.com", "Viewer");
    var workspace = saveWorkspace("Revoke Auth Share WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, viewer, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Revoke Auth Share Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    saveProjectMember(project, viewer, ProjectRole.VIEWER);

    StepVerifier.create(revokeShareLinkUseCase.revokeShareLink(
        new RevokeShareLinkCommand(project.getId(), "missing-share-link-id", viewer.id())))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("공유 링크 삭제는 링크 존재 여부 검증보다 프로젝트 관리자 권한을 먼저 확인한다")
  void deleteShareLink_rejectsProjectViewerBeforeMissingLinkValidation() {
    User admin = signUpUser("admin-share-delete-auth@test.com", "Admin");
    User viewer = signUpUser("viewer-share-delete-auth@test.com", "Viewer");
    var workspace = saveWorkspace("Delete Auth Share WS", "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    saveWorkspaceMember(workspace, viewer, WorkspaceRole.MEMBER);
    var project = saveProject(workspace, "Delete Auth Share Project");
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    saveProjectMember(project, viewer, ProjectRole.VIEWER);

    StepVerifier.create(deleteShareLinkUseCase.deleteShareLink(
        new DeleteShareLinkCommand(project.getId(), "missing-share-link-id", viewer.id())))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ADMIN_REQUIRED))
        .verify();
  }

}
