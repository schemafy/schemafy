package com.schemafy.domain.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.project.application.port.in.CreateWorkspaceCommand;
import com.schemafy.domain.project.application.port.in.CreateWorkspaceUseCase;
import com.schemafy.domain.project.application.port.in.WorkspaceDetail;
import com.schemafy.domain.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.domain.project.application.port.out.WorkspacePort;
import com.schemafy.domain.project.domain.Workspace;
import com.schemafy.domain.project.domain.WorkspaceMember;
import com.schemafy.domain.project.domain.WorkspaceRole;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class CreateWorkspaceService implements CreateWorkspaceUseCase {

  private final TransactionalOperator transactionalOperator;
  private final UlidGeneratorPort ulidGeneratorPort;
  private final WorkspacePort workspacePort;
  private final WorkspaceMemberPort workspaceMemberPort;
  private final WorkspaceAccessHelper workspaceAccessHelper;

  @Override
  public Mono<WorkspaceDetail> createWorkspace(CreateWorkspaceCommand command) {
    return Mono.defer(() -> Mono.zip(
        Mono.fromCallable(ulidGeneratorPort::generate),
        Mono.fromCallable(ulidGeneratorPort::generate))
        .flatMap(tuple -> {
          Workspace workspace = Workspace.create(tuple.getT1(), command.name(),
              command.description());
          WorkspaceMember adminMember = WorkspaceMember.create(
              tuple.getT2(),
              workspace.getId(),
              command.requesterId(),
              WorkspaceRole.ADMIN);

          return workspacePort.save(workspace)
              .flatMap(savedWorkspace -> workspaceMemberPort.save(adminMember)
                  .thenReturn(savedWorkspace))
              .flatMap(savedWorkspace -> workspaceAccessHelper
                  .buildWorkspaceDetail(savedWorkspace, command.requesterId()));
        }))
        .as(transactionalOperator::transactional);
  }

}
