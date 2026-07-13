package com.schemafy.core.erd.memo.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonObjectMetadataConverter;
import com.schemafy.core.erd.memo.application.port.in.UpdateMemoPositionCommand;
import com.schemafy.core.erd.memo.application.port.in.UpdateMemoPositionUseCase;
import com.schemafy.core.erd.memo.application.port.out.ChangeMemoPositionPort;
import com.schemafy.core.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.core.erd.memo.domain.Memo;
import com.schemafy.core.erd.memo.domain.exception.MemoErrorCode;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.MEMO;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = MEMO, id = "memoId"))
class UpdateMemoPositionService implements UpdateMemoPositionUseCase {

  private final GetMemoByIdPort getMemoByIdPort;
  private final ChangeMemoPositionPort changeMemoPositionPort;
  private final JsonObjectMetadataConverter jsonObjectMetadataConverter;

  @Override
  public Mono<Memo> updateMemoPosition(UpdateMemoPositionCommand command) {
    return Mono.defer(() -> {
      String canonicalPositions = jsonObjectMetadataConverter.toStorageJson(command.positions());
      return getMemoByIdPort.findMemoById(command.memoId())
          .switchIfEmpty(Mono.error(new DomainException(MemoErrorCode.NOT_FOUND)))
          .flatMap(memo -> {
            if (!memo.authorId().equals(command.requesterId())) {
              return Mono.error(new DomainException(MemoErrorCode.ACCESS_DENIED));
            }
            return changeMemoPositionPort
                .changeMemoPosition(memo.id(), canonicalPositions)
                .then(getMemoByIdPort.findMemoById(memo.id()))
                .switchIfEmpty(
                    Mono.error(new DomainException(MemoErrorCode.NOT_FOUND)));
          });
    });
  }

}
