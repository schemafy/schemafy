package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Component;

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

  private final ProjectPort projectPort;
  private final ProjectMemberPort projectMemberPort;
  private final InvitationPort invitationPort;
  private final ShareLinkPort shareLinkPort;

  Mono<Void> softDeleteProjectCascade(Project project) {
    String projectId = project.getId();
    Mono<Void> softDeleteProject = project.isDeleted()
        ? Mono.empty()
        : Mono.defer(() -> {
          project.delete();
          return projectPort.save(project).then();
        });

    return softDeleteProject
        .then(projectMemberPort.softDeleteByProjectId(projectId))
        .then(invitationPort.softDeleteByTarget(
            InvitationType.PROJECT.name(),
            projectId))
        .then(shareLinkPort.softDeleteByProjectId(projectId))
        .then();
  }

}
