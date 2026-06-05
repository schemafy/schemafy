package com.schemafy.core.erd.support;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.reactivestreams.Subscription;

import com.schemafy.core.project.adapter.out.persistence.ProjectMemberRepository;
import com.schemafy.core.project.adapter.out.persistence.ProjectRepository;
import com.schemafy.core.project.adapter.out.persistence.WorkspaceMemberRepository;
import com.schemafy.core.project.adapter.out.persistence.WorkspaceRepository;
import com.schemafy.core.project.application.access.ProjectAccessRequesterContext;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.ulid.application.service.UlidGenerator;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

@ResourceLock(value = "reactor-hooks", mode = ResourceAccessMode.READ_WRITE)
public abstract class ErdProjectIntegrationSupport {

  protected static final String TEST_REQUESTER_ID = "test-erd-requester";
  private static final String PROJECT_ACCESS_REQUESTER_HOOK = ErdProjectIntegrationSupport.class.getName();

  @Autowired
  protected WorkspaceRepository workspaceRepository;

  @Autowired
  protected WorkspaceMemberRepository workspaceMemberRepository;

  @Autowired
  protected ProjectRepository projectRepository;

  @Autowired
  protected ProjectMemberRepository projectMemberRepository;

  @Autowired
  protected DatabaseClient databaseClient;

  @BeforeEach
  void setUpProjectAccessRequesterContext() {
    Hooks.onEachOperator(
        PROJECT_ACCESS_REQUESTER_HOOK,
        Operators.lift((ignored, subscriber) -> withProjectAccessRequester(subscriber)));
  }

  @AfterEach
  void tearDownProjectAccessRequesterContext() {
    Hooks.resetOnEachOperator(PROJECT_ACCESS_REQUESTER_HOOK);
  }

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

    workspaceMemberRepository.save(WorkspaceMember.create(
        UlidGenerator.generate(),
        workspace.getId(),
        TEST_REQUESTER_ID,
        WorkspaceRole.ADMIN)).block();

    projectMemberRepository.save(ProjectMember.create(
        UlidGenerator.generate(),
        project.getId(),
        TEST_REQUESTER_ID,
        ProjectRole.ADMIN)).block();

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

  protected long countRowsByColumn(String tableName, String columnName, String value) {
    return databaseClient.sql(
        "SELECT COUNT(*) AS count FROM " + tableName + " WHERE " + columnName + " = :value")
        .bind("value", value)
        .map((row, metadata) -> row.get("count", Long.class))
        .one()
        .block();
  }

  protected long countRelationshipsByTableId(String tableId) {
    return databaseClient.sql("""
        SELECT COUNT(*) AS count
        FROM db_relationships
        WHERE fk_table_id = :tableId OR pk_table_id = :tableId
        """)
        .bind("tableId", tableId)
        .map((row, metadata) -> row.get("count", Long.class))
        .one()
        .block();
  }

  private static <T> CoreSubscriber<T> withProjectAccessRequester(
      CoreSubscriber<? super T> subscriber) {
    return new CoreSubscriber<>() {

      @Override
      public void onSubscribe(Subscription subscription) {
        subscriber.onSubscribe(subscription);
      }

      @Override
      public void onNext(T value) {
        subscriber.onNext(value);
      }

      @Override
      public void onError(Throwable throwable) {
        subscriber.onError(throwable);
      }

      @Override
      public void onComplete() {
        subscriber.onComplete();
      }

      @Override
      public Context currentContext() {
        return ProjectAccessRequesterContext.withRequesterId(TEST_REQUESTER_ID)
            .putAll(subscriber.currentContext().readOnly());
      }

    };
  }

}
