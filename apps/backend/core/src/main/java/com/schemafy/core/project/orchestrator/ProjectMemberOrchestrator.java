package com.schemafy.core.project.orchestrator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.orchestrator.dto.ProjectMemberView;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.project.application.port.in.AcceptProjectInvitationCommand;
import com.schemafy.domain.project.application.port.in.AcceptProjectInvitationUseCase;
import com.schemafy.domain.project.application.port.in.GetProjectMembersQuery;
import com.schemafy.domain.project.application.port.in.GetProjectMembersUseCase;
import com.schemafy.domain.project.application.port.in.UpdateProjectMemberRoleCommand;
import com.schemafy.domain.project.application.port.in.UpdateProjectMemberRoleUseCase;
import com.schemafy.domain.project.domain.ProjectMember;
import com.schemafy.domain.user.application.port.in.GetUserByIdQuery;
import com.schemafy.domain.user.application.port.in.GetUserByIdUseCase;
import com.schemafy.domain.user.application.port.in.GetUsersByIdsQuery;
import com.schemafy.domain.user.application.port.in.GetUsersByIdsUseCase;
import com.schemafy.domain.user.domain.User;
import com.schemafy.domain.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ProjectMemberOrchestrator {

  private final GetProjectMembersUseCase getProjectMembersUseCase;
  private final UpdateProjectMemberRoleUseCase updateProjectMemberRoleUseCase;
  private final AcceptProjectInvitationUseCase acceptProjectInvitationUseCase;
  private final GetUserByIdUseCase getUserByIdUseCase;
  private final GetUsersByIdsUseCase getUsersByIdsUseCase;

  public Mono<PageResponse<ProjectMemberView>> getMembers(
      GetProjectMembersQuery query) {
    return getProjectMembersUseCase.getProjectMembers(query)
        .flatMap(result -> {
          Set<String> userIds = result.content().stream()
              .map(ProjectMember::getUserId)
              .collect(Collectors.toSet());

          if (userIds.isEmpty()) {
            return Mono.just(PageResponse.of(Collections.emptyList(),
                result.page(), result.size(), result.totalElements()));
          }

          return loadUsersById(userIds)
              .map(usersById -> result.content().stream()
                  .map(member -> new ProjectMemberView(member,
                      requireUser(usersById, member.getUserId())))
                  .toList())
              .map(content -> PageResponse.of(content, result.page(),
                  result.size(), result.totalElements()));
        });
  }

  public Mono<ProjectMemberView> updateMemberRole(
      UpdateProjectMemberRoleCommand command) {
    return updateProjectMemberRoleUseCase.updateProjectMemberRole(command)
        .flatMap(this::hydrateMember);
  }

  public Mono<ProjectMemberView> acceptInvitation(
      AcceptProjectInvitationCommand command) {
    return acceptProjectInvitationUseCase.acceptProjectInvitation(command)
        .flatMap(this::hydrateMember);
  }

  private Mono<ProjectMemberView> hydrateMember(ProjectMember member) {
    return getUserByIdUseCase.getUserById(new GetUserByIdQuery(member.getUserId()))
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)))
        .map(user -> new ProjectMemberView(member, user));
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
