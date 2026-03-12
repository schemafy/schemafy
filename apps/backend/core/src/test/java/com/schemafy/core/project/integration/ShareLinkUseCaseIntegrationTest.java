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
import com.schemafy.core.project.domain.exception.ShareLinkErrorCode;
import com.schemafy.core.user.domain.User;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShareLink usecase integration")
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
  @DisplayName("create/get/list share links for project admins")
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
  @DisplayName("accessShareLink returns project and increments access count")
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
  @DisplayName("accessShareLink rejects revoked or expired links")
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
  @DisplayName("revokeShareLink and deleteShareLink update lifecycle state")
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

}
