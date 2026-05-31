package com.schemafy.core.project.application.access;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationCommand;
import com.schemafy.core.erd.table.application.port.in.GetTableQuery;
import com.schemafy.core.project.domain.ProjectRole;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.OPERATION;
import static com.schemafy.core.project.application.access.ProjectAccessResourceType.TABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProjectAccessTargetInference")
class ProjectAccessTargetInferenceTest {

  private final ProjectAccessTargetInference inference = new ProjectAccessTargetInference();

  @Test
  @DisplayName("target이 명시되지 않으면 빈 target 목록을 반환한다")
  void emptyTargetsWhenTargetIsNotExplicit() throws Exception {
    var targets = inference.resolveTargets(annotation("viewer"), GetTableQuery.class);

    assertThat(targets).isEmpty();
  }

  @Test
  @DisplayName("명시 target을 해석한다")
  void parseExplicitTarget() throws Exception {
    var targets = inference.resolveTargets(annotation("operation"), UndoErdOperationCommand.class);

    assertThat(targets)
        .containsExactly(new ProjectAccessTarget(ProjectAccessResourceType.OPERATION, "opId"));
  }

  @Test
  @DisplayName("명시 targets를 해석한다")
  void parseExplicitTargets() throws Exception {
    var targets = inference.resolveTargets(annotation("relationship"), RelationshipCommand.class);

    assertThat(targets)
        .containsExactlyInAnyOrder(
            new ProjectAccessTarget(ProjectAccessResourceType.TABLE, "fkTableId"),
            new ProjectAccessTarget(ProjectAccessResourceType.TABLE, "pkTableId"));
  }

  @Test
  @DisplayName("target id accessor가 없으면 실패한다")
  void missingAccessorFails() throws Exception {
    assertThatThrownBy(() -> inference.resolveTargets(annotation("missing"), GetTableQuery.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Project access target requires accessor missingId()");
  }

  @Test
  @DisplayName("target id가 비어 있으면 실패한다")
  void blankTargetIdFails() throws Exception {
    assertThatThrownBy(() -> inference.resolveTargets(annotation("invalid"), GetTableQuery.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Project access target must define type and id accessor");
  }

  @Test
  @DisplayName("단일 target id가 비어 있으면 실패한다")
  void blankSingleTargetIdFails() throws Exception {
    assertThatThrownBy(() -> inference.resolveTargets(annotation("invalidSingle"), GetTableQuery.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Project access target must define type and id accessor");
  }

  private RequireProjectAccess annotation(String methodName) throws Exception {
    Method method = AnnotatedMethods.class.getDeclaredMethod(methodName);
    return method.getAnnotation(RequireProjectAccess.class);
  }

  private static class AnnotatedMethods {

    @RequireProjectAccess(role = ProjectRole.VIEWER)
    void viewer() {}

    @RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = OPERATION, id = "opId"))
    void operation() {}

    @RequireProjectAccess(role = ProjectRole.EDITOR, targets = {
      @AccessTarget(value = TABLE, id = "fkTableId"),
      @AccessTarget(value = TABLE, id = "pkTableId")
    })
    void relationship() {}

    @RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = TABLE, id = "missingId"))
    void missing() {}

    @RequireProjectAccess(role = ProjectRole.EDITOR, targets = @AccessTarget(value = TABLE, id = ""))
    void invalid() {}

    @RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = TABLE, id = ""))
    void invalidSingle() {}

  }

  record RelationshipCommand(String fkTableId, String pkTableId) {
  }

}
