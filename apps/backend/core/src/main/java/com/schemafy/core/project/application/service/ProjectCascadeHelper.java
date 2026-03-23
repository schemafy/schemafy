package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.DeleteSchemaUseCase;
import com.schemafy.core.erd.schema.application.port.out.GetSchemasByProjectIdPort;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.InvitationType;
import com.schemafy.core.project.domain.Project;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ProjectCascadeHelper {

  private final GetSchemasByProjectIdPort getSchemasByProjectIdPort;
  private final DeleteSchemaUseCase deleteSchemaUseCase;
  private final ProjectPort projectPort;
  private final ProjectMemberPort projectMemberPort;
  private final InvitationPort invitationPort;
  private final ShareLinkPort shareLinkPort;

  Mono<Void> softDeleteProjectCascade(Project project) {
    String projectId = project.getId();
    Mono<Void> deleteSchemas = getSchemasByProjectIdPort.findSchemasByProjectId(projectId)
        .concatMap(schema -> deleteSchemaUseCase.deleteSchema(new DeleteSchemaCommand(schema.id()))
            .then())
        .then();
    Mono<Void> softDeleteProject = project.isDeleted()
        ? Mono.empty()
        : Mono.defer(() -> {
          project.delete();
          return projectPort.save(project).then();
        });

    return deleteSchemas
        .then(softDeleteProject)
        .then(projectMemberPort.softDeleteByProjectId(projectId))
        .then(invitationPort.softDeleteByTarget(
            InvitationType.PROJECT.name(),
            projectId))
        .then(shareLinkPort.softDeleteByProjectId(projectId))
        .then();
  }

}
