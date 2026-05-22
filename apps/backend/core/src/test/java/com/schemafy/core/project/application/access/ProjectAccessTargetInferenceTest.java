package com.schemafy.core.project.application.access;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.erd.constraint.application.port.in.AddConstraintColumnCommand;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationCommand;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.core.erd.schema.application.port.in.GetSchemasByProjectIdQuery;
import com.schemafy.core.erd.table.application.port.in.GetTableQuery;
import com.schemafy.core.project.domain.ProjectRole;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProjectAccessTargetInference")
class ProjectAccessTargetInferenceTest {

  private final ProjectAccessTargetInference inference = new ProjectAccessTargetInference(
      new ProjectAccessResourceRegistry(List.of(new TestResourceResolver())));

  @Test
  @DisplayName("단일 id accessor는 자동 target으로 추론한다")
  void inferSingleTarget() throws Exception {
    var targets = inference.resolveTargets(annotation("viewer"), GetTableQuery.class);

    assertThat(targets)
        .containsExactly(new ProjectAccessTarget(ProjectAccessResourceType.TABLE, "tableId"));
  }

  @Test
  @DisplayName("ERD projectId는 PROJECT target으로 추론한다")
  void inferProjectTarget() throws Exception {
    var targets = inference.resolveTargets(annotation("viewer"),
        GetSchemasByProjectIdQuery.class);

    assertThat(targets)
        .containsExactly(new ProjectAccessTarget(ProjectAccessResourceType.PROJECT, "projectId"));
  }

  @Test
  @DisplayName("fkTableId와 pkTableId는 허용된 다중 TABLE target이다")
  void inferRelationshipTablePair() throws Exception {
    var targets = inference.resolveTargets(annotation("viewer"),
        CreateRelationshipCommand.class);

    assertThat(targets)
        .containsExactlyInAnyOrder(
            new ProjectAccessTarget(ProjectAccessResourceType.TABLE, "fkTableId"),
            new ProjectAccessTarget(ProjectAccessResourceType.TABLE, "pkTableId"));
  }

  @Test
  @DisplayName("허용되지 않은 다중 target은 명시 targets를 요구한다")
  void ambiguousTargetsFail() throws Exception {
    assertThatThrownBy(() -> inference.resolveTargets(annotation("viewer"),
        AddConstraintColumnCommand.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Ambiguous project access targets");
  }

  @Test
  @DisplayName("opId는 자동 추론하지 않고 명시 DSL로만 해석한다")
  void opIdRequiresExplicitTarget() throws Exception {
    assertThat(inference.resolveTargets(annotation("viewer"),
        UndoErdOperationCommand.class))
        .isEmpty();

    assertThat(inference.resolveTargets(annotation("operation"), UndoErdOperationCommand.class))
        .containsExactly(new ProjectAccessTarget(ProjectAccessResourceType.OPERATION, "opId"));
  }

  private RequireProjectAccess annotation(String methodName) throws Exception {
    Method method = AnnotatedMethods.class.getDeclaredMethod(methodName);
    return method.getAnnotation(RequireProjectAccess.class);
  }

  private static class AnnotatedMethods {

    @RequireProjectAccess(role = ProjectRole.VIEWER)
    void viewer() {}

    @RequireProjectAccess(role = ProjectRole.EDITOR, target = "operation:opId")
    void operation() {}

  }

  private static class TestResourceResolver implements ProjectAccessResourceResolver {

    @Override
    public Set<ProjectAccessResourceType> resourceTypes() {
      return Set.of();
    }

    @Override
    public List<ProjectAccessAccessorRule> accessorRules() {
      return List.of(
          new ProjectAccessAccessorRule(
              "constraintColumnId",
              ProjectAccessResourceType.CONSTRAINT_COLUMN),
          new ProjectAccessAccessorRule(
              "relationshipColumnId",
              ProjectAccessResourceType.RELATIONSHIP_COLUMN),
          new ProjectAccessAccessorRule("indexColumnId", ProjectAccessResourceType.INDEX_COLUMN),
          new ProjectAccessAccessorRule("fkTableId", ProjectAccessResourceType.TABLE),
          new ProjectAccessAccessorRule("pkTableId", ProjectAccessResourceType.TABLE),
          new ProjectAccessAccessorRule("schemaId", ProjectAccessResourceType.SCHEMA),
          new ProjectAccessAccessorRule("tableId", ProjectAccessResourceType.TABLE),
          new ProjectAccessAccessorRule("columnId", ProjectAccessResourceType.COLUMN),
          new ProjectAccessAccessorRule("constraintId", ProjectAccessResourceType.CONSTRAINT),
          new ProjectAccessAccessorRule("relationshipId", ProjectAccessResourceType.RELATIONSHIP),
          new ProjectAccessAccessorRule("indexId", ProjectAccessResourceType.INDEX),
          new ProjectAccessAccessorRule("memoId", ProjectAccessResourceType.MEMO));
    }

    @Override
    public Mono<ProjectAccessResourceRef> resolveParent(ProjectAccessResourceType type, String id) {
      return Mono.error(new UnsupportedOperationException());
    }

  }

}
