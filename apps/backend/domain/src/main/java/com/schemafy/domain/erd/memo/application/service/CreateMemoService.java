package com.schemafy.domain.erd.memo.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.memo.application.port.in.CreateMemoCommand;
import com.schemafy.domain.erd.memo.application.port.in.CreateMemoUseCase;
import com.schemafy.domain.erd.memo.application.port.out.CreateMemoCommentPort;
import com.schemafy.domain.erd.memo.application.port.out.CreateMemoPort;
import com.schemafy.domain.erd.memo.domain.Memo;
import com.schemafy.domain.erd.memo.domain.MemoComment;
import com.schemafy.domain.erd.memo.domain.MemoDetail;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class CreateMemoService implements CreateMemoUseCase {

  private final GetSchemaByIdPort getSchemaByIdPort;
  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateMemoPort createMemoPort;
  private final CreateMemoCommentPort createMemoCommentPort;
  private final TransactionalOperator transactionalOperator;

  @Override
  public Mono<MemoDetail> createMemo(CreateMemoCommand command) {
    return getSchemaByIdPort.findSchemaById(command.schemaId())
        .switchIfEmpty(Mono.error(
            new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found: " + command.schemaId())))
        .then(Mono.zip(
            Mono.fromCallable(ulidGeneratorPort::generate),
            Mono.fromCallable(ulidGeneratorPort::generate)))
        .flatMap(tuple -> {
          Memo memo = new Memo(
              tuple.getT1(),
              command.schemaId(),
              command.authorId(),
              command.positions(),
              null,
              null,
              null);
          MemoComment comment = new MemoComment(
              tuple.getT2(),
              tuple.getT1(),
              command.authorId(),
              command.body(),
              null,
              null,
              null);

          return createMemoPort.createMemo(memo)
              .flatMap(savedMemo -> createMemoCommentPort.createMemoComment(comment)
                  .map(savedComment -> new MemoDetail(savedMemo,
                      List.of(savedComment))));
        })
        .as(transactionalOperator::transactional);
  }

}
