package com.schemafy.core.user.service.user;

import org.springframework.stereotype.Component;

import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.project.repository.vo.WorkspaceSettings;
import com.schemafy.domain.user.domain.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserWorkspaceProvisioner {

  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;

  public Mono<User> createDefaultWorkspace(User user) {
    return Mono.fromSupplier(() -> Workspace.create(
        user.id(),
        user.name() + "'s Workspace",
        "Personal workspace for " + user.name(),
        WorkspaceSettings.defaultSettings()))
        .flatMap(workspace -> workspaceRepository.save(workspace)
            .flatMap(savedWorkspace -> workspaceMemberRepository
                .save(WorkspaceMember.create(
                    savedWorkspace.getId(),
                    user.id(),
                    WorkspaceRole.ADMIN))
                .thenReturn(user)))
        .doOnSuccess(u -> log.info("Created default workspace for user: {}", user.id()))
        .doOnError(e -> log.error("Failed to create default workspace for user: {}", user.id(),
            e));
  }

}
