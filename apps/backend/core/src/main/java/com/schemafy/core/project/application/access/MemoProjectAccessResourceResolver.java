package com.schemafy.core.project.application.access;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.memo.domain.exception.MemoErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class MemoProjectAccessResourceResolver implements ProjectAccessResourceResolver {

  private final GetProjectIdByAccessResourcePort projectIdPort;

  @Override
  public Set<ProjectAccessResourceType> resourceTypes() {
    return Set.of(ProjectAccessResourceType.MEMO, ProjectAccessResourceType.MEMO_COMMENT);
  }

  @Override
  public List<ProjectAccessAccessorRule> accessorRules() {
    return List.of(new ProjectAccessAccessorRule("memoId", ProjectAccessResourceType.MEMO));
  }

  @Override
  public Mono<ProjectAccessResourceRef> resolveParent(ProjectAccessResourceType type, String id) {
    return switch (type) {
    case MEMO -> resolveMemoParent(id);
    case MEMO_COMMENT -> resolveMemoCommentParent(id);
    default -> Mono.error(new IllegalStateException("Unsupported memo access type: " + type));
    };
  }

  private Mono<ProjectAccessResourceRef> resolveMemoParent(String id) {
    return projectIdPort.findProjectId(ProjectAccessResourceType.MEMO, id)
        .switchIfEmpty(Mono.error(
            new DomainException(MemoErrorCode.NOT_FOUND, "Memo not found: " + id)))
        .map(projectId -> new ProjectAccessResourceRef(
            ProjectAccessResourceType.PROJECT,
            projectId));
  }

  private Mono<ProjectAccessResourceRef> resolveMemoCommentParent(String id) {
    return projectIdPort.findProjectId(ProjectAccessResourceType.MEMO_COMMENT, id)
        .switchIfEmpty(Mono.error(new DomainException(
            MemoErrorCode.COMMENT_NOT_FOUND, "Memo comment not found: " + id)))
        .map(projectId -> new ProjectAccessResourceRef(
            ProjectAccessResourceType.PROJECT,
            projectId));
  }

}
