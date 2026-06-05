package com.schemafy.core.project.application.access;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("ProjectAccessResourceResolver direct lookup")
class ProjectAccessDirectResourceResolverTest {

  @Test
  @DisplayName("리소스 pk로 projectId를 바로 해석한다")
  void resolveParent_returnsProjectRefFromDirectLookup() {
    FakeProjectIdPort projectIdPort = new FakeProjectIdPort(Map.ofEntries(
        entry(ProjectAccessResourceType.SCHEMA, "schema-1"),
        entry(ProjectAccessResourceType.TABLE, "table-1"),
        entry(ProjectAccessResourceType.COLUMN, "column-1"),
        entry(ProjectAccessResourceType.CONSTRAINT, "constraint-1"),
        entry(ProjectAccessResourceType.CONSTRAINT_COLUMN, "constraint-column-1"),
        entry(ProjectAccessResourceType.INDEX, "index-1"),
        entry(ProjectAccessResourceType.INDEX_COLUMN, "index-column-1"),
        entry(ProjectAccessResourceType.RELATIONSHIP, "relationship-1"),
        entry(ProjectAccessResourceType.RELATIONSHIP_COLUMN, "relationship-column-1"),
        entry(ProjectAccessResourceType.MEMO, "memo-1"),
        entry(ProjectAccessResourceType.MEMO_COMMENT, "memo-comment-1"),
        entry(ProjectAccessResourceType.OPERATION, "operation-1")));

    List<ResolverCase> cases = List.of(
        new ResolverCase(
            new SchemaProjectAccessResourceResolver(projectIdPort),
            ProjectAccessResourceType.SCHEMA,
            "schema-1"),
        new ResolverCase(
            new TableProjectAccessResourceResolver(projectIdPort),
            ProjectAccessResourceType.TABLE,
            "table-1"),
        new ResolverCase(
            new ColumnProjectAccessResourceResolver(projectIdPort),
            ProjectAccessResourceType.COLUMN,
            "column-1"),
        new ResolverCase(
            new ConstraintProjectAccessResourceResolver(projectIdPort),
            ProjectAccessResourceType.CONSTRAINT,
            "constraint-1"),
        new ResolverCase(
            new ConstraintProjectAccessResourceResolver(projectIdPort),
            ProjectAccessResourceType.CONSTRAINT_COLUMN,
            "constraint-column-1"),
        new ResolverCase(
            new IndexProjectAccessResourceResolver(projectIdPort),
            ProjectAccessResourceType.INDEX,
            "index-1"),
        new ResolverCase(
            new IndexProjectAccessResourceResolver(projectIdPort),
            ProjectAccessResourceType.INDEX_COLUMN,
            "index-column-1"),
        new ResolverCase(
            new RelationshipProjectAccessResourceResolver(projectIdPort),
            ProjectAccessResourceType.RELATIONSHIP,
            "relationship-1"),
        new ResolverCase(
            new RelationshipProjectAccessResourceResolver(projectIdPort),
            ProjectAccessResourceType.RELATIONSHIP_COLUMN,
            "relationship-column-1"),
        new ResolverCase(
            new MemoProjectAccessResourceResolver(projectIdPort),
            ProjectAccessResourceType.MEMO,
            "memo-1"),
        new ResolverCase(
            new MemoProjectAccessResourceResolver(projectIdPort),
            ProjectAccessResourceType.MEMO_COMMENT,
            "memo-comment-1"),
        new ResolverCase(
            new OperationProjectAccessResourceResolver(projectIdPort),
            ProjectAccessResourceType.OPERATION,
            "operation-1"));

    for (ResolverCase resolverCase : cases) {
      StepVerifier.create(resolverCase.resolver()
          .resolveParent(resolverCase.type(), resolverCase.id()))
          .expectNext(new ProjectAccessResourceRef(
              ProjectAccessResourceType.PROJECT,
              "project-1"))
          .verifyComplete();
    }
  }

  private static Map.Entry<ResourceKey, String> entry(ProjectAccessResourceType type, String id) {
    return Map.entry(new ResourceKey(type, id), "project-1");
  }

  private record ResolverCase(
      ProjectAccessResourceResolver resolver,
      ProjectAccessResourceType type,
      String id) {
  }

  private record ResourceKey(ProjectAccessResourceType type, String id) {
  }

  private static class FakeProjectIdPort implements GetProjectIdByAccessResourcePort {

    private final Map<ResourceKey, String> projectIds;

    FakeProjectIdPort(Map<ResourceKey, String> projectIds) {
      this.projectIds = new LinkedHashMap<>(projectIds);
    }

    @Override
    public Mono<String> findProjectId(ProjectAccessResourceType type, String id) {
      return Mono.justOrEmpty(projectIds.get(new ResourceKey(type, id)));
    }

  }

}
