package com.schemafy.core.erd.support;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;

import com.schemafy.core.project.adapter.out.persistence.DomainProjectRepository;
import com.schemafy.core.project.adapter.out.persistence.DomainWorkspaceRepository;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.ulid.application.service.UlidGenerator;

public abstract class ErdProjectIntegrationSupport {

  @Autowired
  protected DomainWorkspaceRepository workspaceRepository;

  @Autowired
  protected DomainProjectRepository projectRepository;

  @Autowired
  protected DatabaseClient databaseClient;

  protected String createActiveProjectId(String prefix) {
    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

    Workspace workspace = workspaceRepository.save(Workspace.create(
        UlidGenerator.generate(),
        prefix + "_workspace_" + uniqueSuffix,
        "description")).block();

    Project project = projectRepository.save(Project.create(
        UlidGenerator.generate(),
        workspace.getId(),
        prefix + "_project_" + uniqueSuffix,
        "description")).block();

    return project.getId();
  }

  protected void softDeleteProject(String projectId) {
    databaseClient.sql(
        "UPDATE projects SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id")
        .bind("id", projectId)
        .fetch()
        .rowsUpdated()
        .block();
  }

}
