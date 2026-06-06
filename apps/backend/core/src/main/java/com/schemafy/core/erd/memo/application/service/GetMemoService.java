package com.schemafy.core.erd.memo.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.memo.application.port.in.GetMemoQuery;
import com.schemafy.core.erd.memo.application.port.in.GetMemoUseCase;
import com.schemafy.core.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.core.erd.memo.application.port.out.GetMemoCommentsByMemoIdPort;
import com.schemafy.core.erd.memo.domain.MemoDetail;
import com.schemafy.core.erd.memo.domain.exception.MemoErrorCode;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.MEMO;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER, target = @AccessTarget(value = MEMO, id = "memoId"))
class GetMemoService implements GetMemoUseCase {

  private final GetMemoByIdPort getMemoByIdPort;
  private final GetMemoCommentsByMemoIdPort getMemoCommentsByMemoIdPort;

  @Override
  public Mono<MemoDetail> getMemo(GetMemoQuery query) {
    return getMemoByIdPort.findMemoById(query.memoId())
        .switchIfEmpty(Mono.error(new DomainException(MemoErrorCode.NOT_FOUND)))
        .flatMap(memo -> getMemoCommentsByMemoIdPort.findMemoCommentsByMemoId(memo.id())
            .collectList()
            .map(comments -> new MemoDetail(memo, comments)));
  }

}
