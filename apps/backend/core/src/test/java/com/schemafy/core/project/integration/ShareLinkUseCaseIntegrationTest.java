package com.schemafy.core.project.integration;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AccessShareLinkQuery;
import com.schemafy.core.project.application.port.in.AccessShareLinkUseCase;
import com.schemafy.core.project.application.port.in.GetProjectShareLinkQuery;
import com.schemafy.core.project.application.port.in.GetProjectShareLinkUseCase;
import com.schemafy.core.project.application.port.in.UpdateProjectShareLinkCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectShareLinkUseCase;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.project.domain.exception.ShareLinkErrorCode;
import com.schemafy.core.user.domain.User;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("프로젝트 공유 링크 유스케이스 통합 테스트")
class ShareLinkUseCaseIntegrationTest extends ProjectDomainIntegrationSupport {

  @Autowired
  private AccessShareLinkUseCase accessShareLinkUseCase;

  @Autowired
  private GetProjectShareLinkUseCase getProjectShareLinkUseCase;

  @Autowired
  private UpdateProjectShareLinkUseCase updateProjectShareLinkUseCase;

  @Test
  @DisplayName("활성화는 링크를 한 번 생성하고 반복 호출해도 같은 링크를 활성 상태로 반환한다")
  void activateCreatesOneStableLinkIdempotently() {
    Fixture fixture = fixture("activate");

    ShareLink first = updateProjectShareLinkUseCase.updateProjectShareLink(
        new UpdateProjectShareLinkCommand(fixture.project().getId(), true,
            fixture.admin().id())).block();
    ShareLink repeated = updateProjectShareLinkUseCase.updateProjectShareLink(
        new UpdateProjectShareLinkCommand(fixture.project().getId(), true,
            fixture.admin().id())).block();

    assertThat(repeated.getId()).isEqualTo(first.getId());
    assertThat(repeated.isActive()).isTrue();
    assertThat(shareLinkRepository.findByProjectIdAndNotDeleted(
        fixture.project().getId()).block()).isNotNull();
  }

  @Test
  @DisplayName("동시 최초 활성화는 하나의 링크를 저장하고 같은 링크를 반환한다")
  void concurrentFirstActivationReturnsTheSameStoredLink() {
    Fixture fixture = fixture("concurrent-first-on");
    UpdateProjectShareLinkCommand command = new UpdateProjectShareLinkCommand(
        fixture.project().getId(), true, fixture.admin().id());

    var links = Flux.merge(
        Mono.defer(() -> updateProjectShareLinkUseCase.updateProjectShareLink(command))
            .subscribeOn(Schedulers.parallel()),
        Mono.defer(() -> updateProjectShareLinkUseCase.updateProjectShareLink(command))
            .subscribeOn(Schedulers.parallel()))
        .collectList()
        .block();

    ShareLink stored = shareLinkRepository.findByProjectIdAndNotDeleted(
        fixture.project().getId()).block();
    assertThat(links).hasSize(2);
    assertThat(links).extracting(ShareLink::getId).containsOnly(stored.getId());
  }

  @Test
  @DisplayName("비활성화는 링크가 없으면 no-op이고, 있으면 같은 링크를 비활성화한다")
  void deactivateIsIdempotentAndDoesNotCreateLink() {
    Fixture fixture = fixture("deactivate");

    StepVerifier.create(updateProjectShareLinkUseCase.updateProjectShareLink(
        new UpdateProjectShareLinkCommand(fixture.project().getId(), false,
            fixture.admin().id())))
        .verifyComplete();

    ShareLink created = updateProjectShareLinkUseCase.updateProjectShareLink(
        new UpdateProjectShareLinkCommand(fixture.project().getId(), true,
            fixture.admin().id())).block();
    ShareLink deactivated = updateProjectShareLinkUseCase.updateProjectShareLink(
        new UpdateProjectShareLinkCommand(fixture.project().getId(), false,
            fixture.admin().id())).block();

    assertThat(deactivated.getId()).isEqualTo(created.getId());
    assertThat(deactivated.isActive()).isFalse();
  }

  @Test
  @DisplayName("동시 활성화와 비활성화는 충돌 없이 마지막 저장 상태 중 하나로 끝난다")
  void concurrentActivationAndDeactivationCompletesWithoutConflict() {
    Fixture fixture = fixture("concurrent-on-off");
    updateProjectShareLinkUseCase.updateProjectShareLink(
        new UpdateProjectShareLinkCommand(fixture.project().getId(), true,
            fixture.admin().id())).block();

    Flux.merge(
        Mono.defer(() -> updateProjectShareLinkUseCase.updateProjectShareLink(
            new UpdateProjectShareLinkCommand(fixture.project().getId(), true,
                fixture.admin().id()))).subscribeOn(Schedulers.parallel()),
        Mono.defer(() -> updateProjectShareLinkUseCase.updateProjectShareLink(
            new UpdateProjectShareLinkCommand(fixture.project().getId(), false,
                fixture.admin().id()))).subscribeOn(Schedulers.parallel()))
        .collectList()
        .block();

    ShareLink stored = shareLinkRepository.findByProjectIdAndNotDeleted(
        fixture.project().getId()).block();
    assertThat(stored.getIsActive()).isIn(true, false);
  }

  @Test
  @DisplayName("단건 조회는 링크가 없으면 비어 있고 프로젝트 관리자가 아니면 거부된다")
  void getReturnsEmptyForNoLinkAndRejectsNonAdmin() {
    Fixture fixture = fixture("get");
    User viewer = signUpUser("viewer-share-get@test.com", "Viewer");
    saveWorkspaceMember(fixture.workspace(), viewer, WorkspaceRole.MEMBER);
    saveProjectMember(fixture.project(), viewer, ProjectRole.VIEWER);

    StepVerifier.create(getProjectShareLinkUseCase.getProjectShareLink(
        new GetProjectShareLinkQuery(fixture.project().getId(), fixture.admin().id())))
        .verifyComplete();
    StepVerifier.create(getProjectShareLinkUseCase.getProjectShareLink(
        new GetProjectShareLinkQuery(fixture.project().getId(), viewer.id())))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("프로젝트 공유 링크의 활성 상태 변경은 프로젝트 관리자만 할 수 있다")
  void updateRejectsNonAdmin() {
    Fixture fixture = fixture("update-access");
    User viewer = signUpUser("viewer-share-update@test.com", "Viewer");
    saveWorkspaceMember(fixture.workspace(), viewer, WorkspaceRole.MEMBER);
    saveProjectMember(fixture.project(), viewer, ProjectRole.VIEWER);

    StepVerifier.create(updateProjectShareLinkUseCase.updateProjectShareLink(
        new UpdateProjectShareLinkCommand(fixture.project().getId(), true, viewer.id())))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ADMIN_REQUIRED))
        .verify();
  }

  @Test
  @DisplayName("공개 접근은 활성 링크만 허용한다")
  void publicAccessAllowsOnlyActiveLink() {
    Fixture fixture = fixture("public");
    ShareLink link = updateProjectShareLinkUseCase.updateProjectShareLink(
        new UpdateProjectShareLinkCommand(fixture.project().getId(), true,
            fixture.admin().id())).block();

    Project accessed = accessShareLinkUseCase.accessShareLink(
        new AccessShareLinkQuery(link.getId(), null, null, null)).block();
    assertThat(accessed.getId()).isEqualTo(fixture.project().getId());

    updateProjectShareLinkUseCase.updateProjectShareLink(
        new UpdateProjectShareLinkCommand(fixture.project().getId(), false,
            fixture.admin().id())).block();
    StepVerifier.create(accessShareLinkUseCase.accessShareLink(
        new AccessShareLinkQuery(link.getId(), null, null, null)))
        .expectErrorMatches(DomainException.hasErrorCode(ShareLinkErrorCode.INVALID_LINK))
        .verify();
  }

  private Fixture fixture(String suffix) {
    User admin = signUpUser("admin-share-" + suffix + "@test.com", "Admin");
    var workspace = saveWorkspace("Share " + suffix, "Description");
    saveWorkspaceMember(workspace, admin, WorkspaceRole.ADMIN);
    var project = saveProject(workspace, "Share Project " + suffix);
    saveProjectMember(project, admin, ProjectRole.ADMIN);
    return new Fixture(admin, workspace, project);
  }

  private record Fixture(User admin, com.schemafy.core.project.domain.Workspace workspace,
      Project project) {
  }

}
