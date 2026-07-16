package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.vendor.application.port.out.GetDbVendorByIdPort;
import com.schemafy.core.erd.vendor.domain.exception.VendorErrorCode;
import com.schemafy.core.project.application.access.RequireWorkspaceAccess;
import com.schemafy.core.project.application.port.in.CreateProjectCommand;
import com.schemafy.core.project.application.port.in.CreateProjectUseCase;
import com.schemafy.core.project.application.port.in.ProjectDetail;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class CreateProjectService implements CreateProjectUseCase {

  private final TransactionalOperator transactionalOperator;
  private final UlidGeneratorPort ulidGeneratorPort;
  private final GetDbVendorByIdPort getDbVendorByIdPort;
  private final ProjectPort projectPort;
  private final ProjectMemberPort projectMemberPort;
  private final ProjectAccessHelper projectAccessHelper;
  private final ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @Override
  @RequireWorkspaceAccess(role = WorkspaceRole.ADMIN)
  public Mono<ProjectDetail> createProject(CreateProjectCommand command) {
    return getDbVendorByIdPort.findActiveById(command.dbVendorId())
        .switchIfEmpty(Mono.error(
            new DomainException(VendorErrorCode.NOT_FOUND,
                "DB Vendor not found: " + command.dbVendorId())))
        .flatMap(dbVendor -> Mono.zip(
            Mono.fromCallable(ulidGeneratorPort::generate),
            Mono.fromCallable(ulidGeneratorPort::generate))
            .flatMap(tuple -> {
              Project project = Project.create(tuple.getT1(),
                  command.workspaceId(), dbVendor.id(), command.name(), command.description());
              ProjectMember adminMember = ProjectMember.create(
                  tuple.getT2(),
                  project.getId(),
                  command.requesterId(),
                  ProjectRole.ADMIN);

              return projectPort.save(project)
                  .flatMap(savedProject -> projectMemberPort.save(adminMember)
                      .thenReturn(savedProject))
                  .flatMap(savedProject -> projectMembershipPropagationHelper
                      .propagateWorkspaceMembersToProject(
                          savedProject.getId(),
                          command.workspaceId(),
                          command.requesterId())
                      .thenReturn(savedProject))
                  .flatMap(savedProject -> projectAccessHelper
                      .buildProjectDetail(savedProject, command.requesterId()));
            }))
        .as(transactionalOperator::transactional);
  }

}
