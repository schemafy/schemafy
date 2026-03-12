package com.schemafy.api.project.orchestrator;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.api.common.type.PageResponse;
import com.schemafy.core.common.PageResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberUseCase;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersQuery;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersUseCase;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleUseCase;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.ulid.application.service.UlidGenerator;
import com.schemafy.core.user.application.port.in.GetUserByIdUseCase;
import com.schemafy.core.user.application.port.in.GetUsersByIdsQuery;
import com.schemafy.core.user.application.port.in.GetUsersByIdsUseCase;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceMemberOrchestrator")
class WorkspaceMemberOrchestratorTest {

  @Mock
  private GetWorkspaceMembersUseCase getWorkspaceMembersUseCase;

  @Mock
  private AddWorkspaceMemberUseCase addWorkspaceMemberUseCase;

  @Mock
  private UpdateWorkspaceMemberRoleUseCase updateWorkspaceMemberRoleUseCase;

  @Mock
  private AcceptWorkspaceInvitationUseCase acceptWorkspaceInvitationUseCase;

  @Mock
  private GetUserByIdUseCase getUserByIdUseCase;

  @Mock
  private GetUsersByIdsUseCase getUsersByIdsUseCase;

  @InjectMocks
  private WorkspaceMemberOrchestrator orchestrator;

  @Test
  @DisplayName("getMembers hydrates users in bulk")
  void getMembers_hydratesUsers() {
    WorkspaceMember member = WorkspaceMember.create(UlidGenerator.generate(),
        "workspace-1", "user-1", WorkspaceRole.ADMIN);
    User user = User.signUp("user-1", "user-1@test.com", "User 1",
        "encoded");

    when(getWorkspaceMembersUseCase.getWorkspaceMembers(any()))
        .thenReturn(Mono.just(PageResult.of(
            List.of(member), 0, 10, 1)));
    when(getUsersByIdsUseCase.getUsersByIds(any(GetUsersByIdsQuery.class)))
        .thenReturn(Flux.just(user));

    StepVerifier.create(orchestrator.getMembers(new GetWorkspaceMembersQuery(
        "workspace-1", "requester-1", 0, 10)))
        .assertNext(result -> {
          assertThat(result).isEqualTo(PageResponse.of(result.content(), 0, 10, 1));
          assertThat(result.content()).hasSize(1);
          assertThat(result.content().get(0).member().getId()).isEqualTo(member.getId());
          assertThat(result.content().get(0).user().id()).isEqualTo(user.id());
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("getMembers fails fast when bulk user lookup is incomplete")
  void getMembers_failsWhenAnyUserMissing() {
    WorkspaceMember member1 = WorkspaceMember.create(UlidGenerator.generate(),
        "workspace-1", "user-1", WorkspaceRole.ADMIN);
    WorkspaceMember member2 = WorkspaceMember.create(UlidGenerator.generate(),
        "workspace-1", "user-2", WorkspaceRole.MEMBER);
    User user1 = User.signUp("user-1", "user-1@test.com", "User 1",
        "encoded");

    when(getWorkspaceMembersUseCase.getWorkspaceMembers(any()))
        .thenReturn(Mono.just(PageResult.of(
            List.of(member1, member2), 0, 10, 2)));
    when(getUsersByIdsUseCase.getUsersByIds(any(GetUsersByIdsQuery.class)))
        .thenReturn(Flux.just(user1));

    StepVerifier.create(orchestrator.getMembers(new GetWorkspaceMembersQuery(
        "workspace-1", "requester-1", 0, 10)))
        .expectErrorMatches(DomainException.hasErrorCode(UserErrorCode.NOT_FOUND))
        .verify();
  }

  @Test
  @DisplayName("acceptInvitation hydrates accepted member with user detail")
  void acceptInvitation_hydratesMember() {
    WorkspaceMember member = WorkspaceMember.create(UlidGenerator.generate(),
        "workspace-1", "user-1", WorkspaceRole.MEMBER);
    User user = User.signUp("user-1", "user-1@test.com", "User 1",
        "encoded");

    when(acceptWorkspaceInvitationUseCase.acceptWorkspaceInvitation(
        any(AcceptWorkspaceInvitationCommand.class)))
        .thenReturn(Mono.just(member));
    when(getUserByIdUseCase.getUserById(any()))
        .thenReturn(Mono.just(user));

    StepVerifier.create(orchestrator.acceptInvitation(
        new AcceptWorkspaceInvitationCommand("invitation-1", "user-1")))
        .assertNext(view -> {
          assertThat(view.member().getId()).isEqualTo(member.getId());
          assertThat(view.user().email()).isEqualTo(user.email());
        })
        .verifyComplete();
  }

}
