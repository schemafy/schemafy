package com.schemafy.api.project.orchestrator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.schemafy.api.common.type.PageResponse;
import com.schemafy.api.project.orchestrator.dto.WorkspaceMemberView;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberUseCase;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersQuery;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersUseCase;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleCommand;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleUseCase;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.user.application.port.in.GetUserByIdQuery;
import com.schemafy.core.user.application.port.in.GetUserByIdUseCase;
import com.schemafy.core.user.application.port.in.GetUsersByIdsQuery;
import com.schemafy.core.user.application.port.in.GetUsersByIdsUseCase;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberOrchestrator {

  private final GetWorkspaceMembersUseCase getWorkspaceMembersUseCase;
  private final AddWorkspaceMemberUseCase addWorkspaceMemberUseCase;
  private final UpdateWorkspaceMemberRoleUseCase updateWorkspaceMemberRoleUseCase;
  private final AcceptWorkspaceInvitationUseCase acceptWorkspaceInvitationUseCase;
  private final GetUserByIdUseCase getUserByIdUseCase;
  private final GetUsersByIdsUseCase getUsersByIdsUseCase;

  public Mono<PageResponse<WorkspaceMemberView>> getMembers(
      GetWorkspaceMembersQuery query) {
    return getWorkspaceMembersUseCase.getWorkspaceMembers(query)
        .flatMap(result -> {
          Set<String> userIds = result.content().stream()
              .map(WorkspaceMember::getUserId)
              .collect(Collectors.toSet());

          if (userIds.isEmpty()) {
            return Mono.just(PageResponse.of(Collections.emptyList(),
                result.page(), result.size(), result.totalElements()));
          }

          return loadUsersById(userIds)
              .map(usersById -> result.content().stream()
                  .map(member -> new WorkspaceMemberView(member,
                      requireUser(usersById, member.getUserId())))
                  .toList())
              .map(content -> PageResponse.of(content, result.page(),
                  result.size(), result.totalElements()));
        });
  }

  public Mono<WorkspaceMemberView> addMember(AddWorkspaceMemberCommand command) {
    return addWorkspaceMemberUseCase.addWorkspaceMember(command)
        .flatMap(this::hydrateMember);
  }

  public Mono<WorkspaceMemberView> updateMemberRole(
      UpdateWorkspaceMemberRoleCommand command) {
    return updateWorkspaceMemberRoleUseCase.updateWorkspaceMemberRole(command)
        .flatMap(this::hydrateMember);
  }

  public Mono<WorkspaceMemberView> acceptInvitation(
      AcceptWorkspaceInvitationCommand command) {
    return acceptWorkspaceInvitationUseCase.acceptWorkspaceInvitation(command)
        .flatMap(this::hydrateMember);
  }

  private Mono<WorkspaceMemberView> hydrateMember(WorkspaceMember member) {
    return getUserByIdUseCase.getUserById(new GetUserByIdQuery(member.getUserId()))
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)))
        .map(user -> new WorkspaceMemberView(member, user));
  }

  private Mono<Map<String, User>> loadUsersById(Set<String> userIds) {
    if (userIds.isEmpty()) {
      return Mono.just(Collections.emptyMap());
    }
    return getUsersByIdsUseCase.getUsersByIds(new GetUsersByIdsQuery(userIds))
        .collectMap(User::id, Function.identity())
        .flatMap(usersById -> {
          if (usersById.size() != userIds.size()) {
            return Mono.error(new DomainException(UserErrorCode.NOT_FOUND));
          }
          return Mono.just(usersById);
        });
  }

  private User requireUser(Map<String, User> usersById, String userId) {
    User user = usersById.get(userId);
    if (user == null) {
      throw new DomainException(UserErrorCode.NOT_FOUND);
    }
    return user;
  }

}
