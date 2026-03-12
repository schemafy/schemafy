package com.schemafy.core.project.orchestrator;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.common.PageResult;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.project.application.port.in.AcceptProjectInvitationCommand;
import com.schemafy.domain.project.application.port.in.AcceptProjectInvitationUseCase;
import com.schemafy.domain.project.application.port.in.GetProjectMembersQuery;
import com.schemafy.domain.project.application.port.in.GetProjectMembersUseCase;
import com.schemafy.domain.project.application.port.in.UpdateProjectMemberRoleUseCase;
import com.schemafy.domain.project.domain.ProjectMember;
import com.schemafy.domain.project.domain.ProjectRole;
import com.schemafy.domain.ulid.application.service.UlidGenerator;
import com.schemafy.domain.user.application.port.in.GetUserByIdUseCase;
import com.schemafy.domain.user.application.port.in.GetUsersByIdsQuery;
import com.schemafy.domain.user.application.port.in.GetUsersByIdsUseCase;
import com.schemafy.domain.user.domain.User;
import com.schemafy.domain.user.domain.exception.UserErrorCode;

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
  @DisplayName("getMembers fails fast when bulk user lookup is incomplete")
  void getMembers_failsWhenAnyUserMissing() {
    ProjectMember member1 = ProjectMember.create(UlidGenerator.generate(),
        "project-1", "user-1", ProjectRole.ADMIN);
    ProjectMember member2 = ProjectMember.create(UlidGenerator.generate(),
        "project-1", "user-2", ProjectRole.VIEWER);
    User user1 = User.signUp("user-1", "user-1@test.com", "User 1",
        "encoded");

    when(getProjectMembersUseCase.getProjectMembers(any()))
        .thenReturn(Mono.just(PageResult.of(
            List.of(member1, member2), 0, 10, 2)));
    when(getUsersByIdsUseCase.getUsersByIds(any(GetUsersByIdsQuery.class)))
        .thenReturn(Flux.just(user1));

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
          assertThat(view.member().getId()).isEqualTo(member.getId());
          assertThat(view.user().email()).isEqualTo(user.email());
        })
        .verifyComplete();
  }

}
