package com.schemafy.core.project.application.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.in.CreateProjectInvitationCommand;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("프로젝트 초대 생성 서비스 테스트")
class CreateProjectInvitationServiceTest {

  private static final String PROJECT_ID = "project-id";
  private static final String WORKSPACE_ID = "workspace-id";

  @Mock
  ProjectMutationGuard projectMutationGuard;

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  InvitationPort invitationPort;

  @Mock
  ProjectInvitationHelper projectInvitationHelper;

  @InjectMocks
  CreateProjectInvitationService sut;

  @Test
  @DisplayName("프로젝트 공유 락을 획득한 트랜잭션에서 최신 프로젝트와 중복 상태를 확인한 뒤 초대를 저장한다")
  void createsInvitationAfterAcquiringSharedProjectLock() {
    var command = new CreateProjectInvitationCommand(
        PROJECT_ID, "invitee@test.com", ProjectRole.EDITOR, "requester-id");
    var enteredGuard = new AtomicBoolean();

    given(projectMutationGuard.protectChildCreation(eq(PROJECT_ID), any()))
        .willAnswer(invocation -> {
          enteredGuard.set(true);
          Supplier<Mono<Invitation>> action = invocation.getArgument(1);
          return action.get();
        });
    given(projectInvitationHelper.findProjectOrThrow(PROJECT_ID))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.just(Project.create(PROJECT_ID, WORKSPACE_ID, "Project", "Description"));
        });
    given(projectInvitationHelper.checkNotAlreadyProjectMemberByEmail(eq(PROJECT_ID), any()))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.empty();
        });
    given(projectInvitationHelper.checkDuplicatePendingInvitation(eq(PROJECT_ID), any()))
        .willAnswer(invocation -> {
          assertThat(enteredGuard).isTrue();
          return Mono.empty();
        });
    given(ulidGeneratorPort.generate()).willReturn("invitation-id");
    given(invitationPort.save(any(Invitation.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(sut.createProjectInvitation(command))
        .assertNext(invitation -> {
          assertThat(invitation.getId()).isEqualTo("invitation-id");
          assertThat(invitation.getProjectId()).isEqualTo(PROJECT_ID);
        })
        .verifyComplete();

    then(projectMutationGuard).should().protectChildCreation(eq(PROJECT_ID), any());
  }

}
