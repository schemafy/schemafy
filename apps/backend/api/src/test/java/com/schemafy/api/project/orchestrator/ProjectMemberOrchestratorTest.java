package com.schemafy.api.project.orchestrator;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationUseCase;
import com.schemafy.core.project.application.port.in.GetProjectMembersQuery;
import com.schemafy.core.project.application.port.in.GetProjectMembersUseCase;
import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleUseCase;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.ulid.application.service.UlidGenerator;
import com.schemafy.core.user.application.port.in.GetUserByIdUseCase;
import com.schemafy.core.user.application.port.in.GetUsersByIdsQuery;
import com.schemafy.core.user.application.port.in.GetUsersByIdsUseCase;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.UserStatus;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectMemberOrchestrator")
class ProjectMemberOrchestratorTest {

  @Mock
  private GetProjectMembersUseCase getProjectMembersUseCase;

  @Mock
  private UpdateProjectMemberRoleUseCase updateProjectMemberRoleUseCase;

  @Mock
  private AcceptProjectInvitationUseCase acceptProjectInvitationUseCase;

  @Mock
  private GetUserByIdUseCase getUserByIdUseCase;

  @Mock
  private GetUsersByIdsUseCase getUsersByIdsUseCase;

  @InjectMocks
  private ProjectMemberOrchestrator orchestrator;

  @Test
  @DisplayName("getMembers hydrates active and inactive users")
  void getMembers_hydratesUsers() {
    ProjectMember member1 = ProjectMember.create(UlidGenerator.generate(),
        "project-1", "user-1", ProjectRole.ADMIN);
    ProjectMember member2 = ProjectMember.create(UlidGenerator.generate(),
        "project-1", "user-2", ProjectRole.EDITOR);
    User user1 = User.signUp("user-1", "user-1@test.com", "User 1",
        "encoded");
    User user2 = User.signUp("user-2", "user-2@test.com", "User 2",
        "encoded").withdraw(Instant.parse("2026-09-12T00:00:00Z"));

    when(getProjectMembersUseCase.getProjectMembers(any()))
        .thenReturn(Mono.just(PageResult.of(
            List.of(member1, member2), 0, 10, 2)));
    when(getUsersByIdsUseCase.getUsersByIds(any(GetUsersByIdsQuery.class)))
        .thenReturn(Flux.just(user1, user2));

    StepVerifier.create(orchestrator.getMembers(new GetProjectMembersQuery(
        "project-1", "requester-1", 0, 10)))
        .assertNext(result -> {
          assertThat(result.content()).hasSize(2);
          assertThat(result.content().get(0).user().id()).isEqualTo(user1.id());
          assertThat(result.content().get(1).user().status())
              .isEqualTo(UserStatus.INACTIVE);
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("getMembers fails when a membership has no user row")
  void getMembers_failsWhenUserIsMissing() {
    ProjectMember existingMember = ProjectMember.create(UlidGenerator.generate(),
        "project-1", "user-1", ProjectRole.ADMIN);
    ProjectMember missingMember = ProjectMember.create(UlidGenerator.generate(),
        "project-1", "user-2", ProjectRole.VIEWER);
    User existingUser = User.signUp("user-1", "user-1@test.com", "User 1",
        "encoded");

    when(getProjectMembersUseCase.getProjectMembers(any()))
        .thenReturn(Mono.just(PageResult.of(
            List.of(existingMember, missingMember), 0, 10, 2)));
    when(getUsersByIdsUseCase.getUsersByIds(any(GetUsersByIdsQuery.class)))
        .thenReturn(Flux.just(existingUser));

    StepVerifier.create(orchestrator.getMembers(new GetProjectMembersQuery(
        "project-1", "requester-1", 0, 10)))
        .expectErrorMatches(DomainException.hasErrorCode(UserErrorCode.NOT_FOUND))
        .verify();
  }

  @Test
  @DisplayName("acceptInvitation hydrates accepted project member with user detail")
  void acceptInvitation_hydratesMember() {
    ProjectMember member = ProjectMember.create(UlidGenerator.generate(),
        "project-1", "user-1", ProjectRole.EDITOR);
    User user = User.signUp("user-1", "user-1@test.com", "User 1",
        "encoded");

    when(acceptProjectInvitationUseCase.acceptProjectInvitation(
        any(AcceptProjectInvitationCommand.class)))
        .thenReturn(Mono.just(member));
    when(getUserByIdUseCase.getUserById(any()))
        .thenReturn(Mono.just(user));

    StepVerifier.create(orchestrator.acceptInvitation(
        new AcceptProjectInvitationCommand("invitation-1", "user-1")))
        .assertNext(view -> {
          assertThat(view.member().getUserId()).isEqualTo(member.getUserId());
          assertThat(view.user().email()).isEqualTo(user.email());
        })
        .verifyComplete();
  }

}
