package com.schemafy.core.project.application.access;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErdProjectContextResolver")
class ErdProjectContextResolverTest {

  @Test
  @DisplayName("리소스 부모 체인을 따라 projectId를 해석한다")
  void resolveProjectId_followsResourceParentChain() {
    ErdProjectContextResolver resolver = new ErdProjectContextResolver(
        new ProjectAccessTargetRegistry(List.of(new TestResourceResolver())));

    StepVerifier.create(resolver.resolveProjectId(ProjectAccessResourceType.COLUMN, "column-1"))
        .expectNext("project-1")
        .verifyComplete();
  }

  @Test
  @DisplayName("리소스 부모 체인에 순환이 있으면 실패한다")
  void resolveProjectId_failsOnResourceCycle() {
    ErdProjectContextResolver resolver = new ErdProjectContextResolver(
        new ProjectAccessTargetRegistry(List.of(new CyclicResourceResolver())));

    StepVerifier.create(resolver.resolveProjectId(ProjectAccessResourceType.COLUMN, "column-1"))
        .expectErrorMatches(error -> error instanceof IllegalStateException
            && error.getMessage().contains("contains a cycle"))
        .verify();
  }

  @Test
  @DisplayName("같은 검증 흐름에서는 이미 해석한 리소스 부모 체인을 재사용한다")
  void resolveProjectId_reusesParentChainInSameVerification() {
    CountingResourceResolver resourceResolver = new CountingResourceResolver();
    ErdProjectContextResolver resolver = new ErdProjectContextResolver(
        new ProjectAccessTargetRegistry(List.of(resourceResolver)));
    Map<ProjectAccessResourceRef, Mono<String>> cache = new HashMap<>();

    StepVerifier.create(Flux.concat(
        resolver.resolveProjectId(ProjectAccessResourceType.COLUMN, "column-1", cache),
        resolver.resolveProjectId(ProjectAccessResourceType.TABLE, "table-1", cache),
        resolver.resolveProjectId(ProjectAccessResourceType.SCHEMA, "schema-1", cache)))
        .expectNext("project-1", "project-1", "project-1")
        .verifyComplete();

    assertThat(resourceResolver.count(ProjectAccessResourceType.COLUMN, "column-1")).isEqualTo(1);
    assertThat(resourceResolver.count(ProjectAccessResourceType.TABLE, "table-1")).isEqualTo(1);
    assertThat(resourceResolver.count(ProjectAccessResourceType.SCHEMA, "schema-1")).isEqualTo(1);
  }

  private static class TestResourceResolver implements ProjectAccessResourceResolver {

    @Override
    public Set<ProjectAccessResourceType> resourceTypes() {
      return Set.of(ProjectAccessResourceType.COLUMN, ProjectAccessResourceType.TABLE);
    }

    @Override
    public Mono<ProjectAccessResourceRef> resolveParent(ProjectAccessResourceType type, String id) {
      return switch (type) {
      case COLUMN -> Mono.just(new ProjectAccessResourceRef(
          ProjectAccessResourceType.TABLE,
          "table-1"));
      case TABLE -> Mono.just(new ProjectAccessResourceRef(
          ProjectAccessResourceType.PROJECT,
          "project-1"));
      default -> Mono.error(new UnsupportedOperationException());
      };
    }

  }

  private static class CyclicResourceResolver implements ProjectAccessResourceResolver {

    @Override
    public Set<ProjectAccessResourceType> resourceTypes() {
      return Set.of(ProjectAccessResourceType.COLUMN, ProjectAccessResourceType.TABLE);
    }

    @Override
    public Mono<ProjectAccessResourceRef> resolveParent(ProjectAccessResourceType type, String id) {
      return switch (type) {
      case COLUMN -> Mono.just(new ProjectAccessResourceRef(
          ProjectAccessResourceType.TABLE,
          "table-1"));
      case TABLE -> Mono.just(new ProjectAccessResourceRef(
          ProjectAccessResourceType.COLUMN,
          "column-1"));
      default -> Mono.error(new UnsupportedOperationException());
      };
    }

  }

  private static class CountingResourceResolver implements ProjectAccessResourceResolver {

    private final Map<ProjectAccessResourceRef, AtomicInteger> counts = new HashMap<>();

    @Override
    public Set<ProjectAccessResourceType> resourceTypes() {
      return Set.of(
          ProjectAccessResourceType.COLUMN,
          ProjectAccessResourceType.TABLE,
          ProjectAccessResourceType.SCHEMA);
    }

    @Override
    public Mono<ProjectAccessResourceRef> resolveParent(ProjectAccessResourceType type, String id) {
      counts.computeIfAbsent(new ProjectAccessResourceRef(type, id), ignored -> new AtomicInteger())
          .incrementAndGet();
      return switch (type) {
      case COLUMN -> Mono.just(new ProjectAccessResourceRef(
          ProjectAccessResourceType.TABLE,
          "table-1"));
      case TABLE -> Mono.just(new ProjectAccessResourceRef(
          ProjectAccessResourceType.SCHEMA,
          "schema-1"));
      case SCHEMA -> Mono.just(new ProjectAccessResourceRef(
          ProjectAccessResourceType.PROJECT,
          "project-1"));
      default -> Mono.error(new UnsupportedOperationException());
      };
    }

    private int count(ProjectAccessResourceType type, String id) {
      AtomicInteger count = counts.get(new ProjectAccessResourceRef(type, id));
      return count == null ? 0 : count.get();
    }

  }

}
