package com.schemafy.core.project.application.service;

import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleCommand;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.project.application.service.MutationGuardTestSupport.invokeGuardAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("프로젝트 멤버 역할 변경 서비스 테스트")
class UpdateProjectMemberRoleServiceTest {

  private static final String PROJECT_ID = "project-id";
  private static final String REQUESTER_ID = "requester-id";
  private static final String TARGET_ID = "target-id";

  @Mock
  WorkspaceMutationGuard workspaceMutationGuard;

  @Mock
  ProjectMemberPort projectMemberPort;

  @Mock
  ProjectAccessHelper projectAccessHelper;

  @InjectMocks
  UpdateProjectMemberRoleService sut;

  @Test
  @DisplayName("활성 멤버십만 조건부로 역할을 변경한다")
  void updateProjectMemberRoleUpdatesOnlyActiveMembership() {
    ProjectMember requester = ProjectMember.create("requester-member", PROJECT_ID,
        REQUESTER_ID, ProjectRole.ADMIN);
    ProjectMember target = ProjectMember.create("target-member", PROJECT_ID,
        TARGET_ID, ProjectRole.VIEWER);
    UpdateProjectMemberRoleCommand command = new UpdateProjectMemberRoleCommand(
        PROJECT_ID, TARGET_ID, ProjectRole.EDITOR, REQUESTER_ID);
    var project = Project.create(PROJECT_ID, "workspace-id", "Project", "Description");
    var enteredGuard = new java.util.concurrent.atomic.AtomicBoolean();
    given(projectAccessHelper.findProjectById(PROJECT_ID)).willReturn(Mono.just(project));
    given(workspaceMutationGuard.protectShared(org.mockito.ArgumentMatchers.eq("workspace-id"),
        org.mockito.ArgumentMatchers.any()))
        .willAnswer(invocation -> {
          enteredGuard.set(true);
          Supplier<Mono<ProjectMember>> action = invocation.getArgument(1);
          return action.get();
        });
    given(projectAccessHelper.findProjectAdminMember(REQUESTER_ID, PROJECT_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.just(requester);
        });
    given(projectAccessHelper.findProjectMember(TARGET_ID, PROJECT_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.just(target);
        });
    given(projectMemberPort.updateRoleIfActive(PROJECT_ID, TARGET_ID,
        ProjectRole.EDITOR.name())).willReturn(Mono.just(1L));

    StepVerifier.create(sut.updateProjectMemberRole(command))
        .expectNext(target)
        .verifyComplete();

    assertThat(target.getRoleAsEnum()).isEqualTo(ProjectRole.EDITOR);
    then(projectMemberPort).should().updateRoleIfActive(PROJECT_ID, TARGET_ID,
        ProjectRole.EDITOR.name());
    then(projectMemberPort).should(never()).save(target);
    then(workspaceMutationGuard).should().protectShared(
        org.mockito.ArgumentMatchers.eq("workspace-id"), org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("대상 멤버십이 이미 삭제되었으면 역할을 변경하지 않는다")
  void updateProjectMemberRoleRejectsAlreadyDeletedMembership() {
    ProjectMember requester = ProjectMember.create("requester-member", PROJECT_ID,
        REQUESTER_ID, ProjectRole.ADMIN);
    ProjectMember target = ProjectMember.create("target-member", PROJECT_ID,
        TARGET_ID, ProjectRole.VIEWER);
    UpdateProjectMemberRoleCommand command = new UpdateProjectMemberRoleCommand(
        PROJECT_ID, TARGET_ID, ProjectRole.EDITOR, REQUESTER_ID);
    var project = Project.create(PROJECT_ID, "workspace-id", "Project", "Description");
    given(projectAccessHelper.findProjectById(PROJECT_ID)).willReturn(Mono.just(project));
    given(workspaceMutationGuard.protectShared(org.mockito.ArgumentMatchers.eq("workspace-id"),
        org.mockito.ArgumentMatchers.any()))
        .willAnswer(invokeGuardAction());
    given(projectAccessHelper.findProjectAdminMember(REQUESTER_ID, PROJECT_ID))
        .willReturn(Mono.just(requester));
    given(projectAccessHelper.findProjectMember(TARGET_ID, PROJECT_ID))
        .willReturn(Mono.just(target));
    given(projectMemberPort.updateRoleIfActive(PROJECT_ID, TARGET_ID,
        ProjectRole.EDITOR.name())).willReturn(Mono.just(0L));

    StepVerifier.create(sut.updateProjectMemberRole(command))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(DomainException.class);
          assertThat(((DomainException) error).getErrorCode())
              .isEqualTo(ProjectErrorCode.MEMBER_NOT_FOUND);
        })
        .verify();

    assertThat(target.getRoleAsEnum()).isEqualTo(ProjectRole.VIEWER);
    then(projectMemberPort).should(never()).save(target);
  }

  @Test
  @DisplayName("워크스페이스 공유 락 대기 중 요청자가 비관리자로 변경되면 역할을 변경하지 않는다")
  void rejectsDemotedRequesterAfterAcquiringSharedWorkspaceLock() {
    ProjectMember target = ProjectMember.create("target-member", PROJECT_ID,
        TARGET_ID, ProjectRole.VIEWER);
    UpdateProjectMemberRoleCommand command = new UpdateProjectMemberRoleCommand(
        PROJECT_ID, TARGET_ID, ProjectRole.EDITOR, REQUESTER_ID);
    var project = Project.create(PROJECT_ID, "workspace-id", "Project", "Description");
    given(projectAccessHelper.findProjectById(PROJECT_ID)).willReturn(Mono.just(project));
    given(workspaceMutationGuard.protectShared(org.mockito.ArgumentMatchers.eq("workspace-id"),
        org.mockito.ArgumentMatchers.any()))
        .willAnswer(invokeGuardAction());
    given(projectAccessHelper.findProjectAdminMember(REQUESTER_ID, PROJECT_ID))
        .willReturn(Mono.error(new DomainException(ProjectErrorCode.ADMIN_REQUIRED)));
    given(projectAccessHelper.findProjectMember(TARGET_ID, PROJECT_ID))
        .willReturn(Mono.just(target));

    StepVerifier.create(sut.updateProjectMemberRole(command))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ADMIN_REQUIRED))
        .verify();

    then(projectMemberPort).should(never()).updateRoleIfActive(
        PROJECT_ID, TARGET_ID, ProjectRole.EDITOR.name());
  }

  @Test
  @DisplayName("워크스페이스 공유 락 대기 중 요청자 멤버십이 삭제되면 역할을 변경하지 않는다")
  void rejectsRemovedRequesterAfterAcquiringSharedWorkspaceLock() {
    ProjectMember target = ProjectMember.create("target-member", PROJECT_ID,
        TARGET_ID, ProjectRole.VIEWER);
    UpdateProjectMemberRoleCommand command = new UpdateProjectMemberRoleCommand(
        PROJECT_ID, TARGET_ID, ProjectRole.EDITOR, REQUESTER_ID);
    var project = Project.create(PROJECT_ID, "workspace-id", "Project", "Description");
    given(projectAccessHelper.findProjectById(PROJECT_ID)).willReturn(Mono.just(project));
    given(workspaceMutationGuard.protectShared(org.mockito.ArgumentMatchers.eq("workspace-id"),
        org.mockito.ArgumentMatchers.any()))
        .willAnswer(invokeGuardAction());
    given(projectAccessHelper.findProjectAdminMember(REQUESTER_ID, PROJECT_ID))
        .willReturn(Mono.error(new DomainException(ProjectErrorCode.ACCESS_DENIED)));
    given(projectAccessHelper.findProjectMember(TARGET_ID, PROJECT_ID))
        .willReturn(Mono.just(target));

    StepVerifier.create(sut.updateProjectMemberRole(command))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.ACCESS_DENIED))
        .verify();

    then(projectMemberPort).should(never()).updateRoleIfActive(
        PROJECT_ID, TARGET_ID, ProjectRole.EDITOR.name());
  }

}
