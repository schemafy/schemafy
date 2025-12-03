package com.schemafy.core.erd.service;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.security.principal.AuthenticatedUser;
import com.schemafy.core.common.security.principal.ProjectRole;
import com.schemafy.core.erd.controller.dto.request.CreateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.CreateMemoRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoRequest;
import com.schemafy.core.erd.controller.dto.response.MemoCommentResponse;
import com.schemafy.core.erd.controller.dto.response.MemoDetailResponse;
import com.schemafy.core.erd.controller.dto.response.MemoResponse;
import com.schemafy.core.erd.repository.MemoCommentRepository;
import com.schemafy.core.erd.repository.MemoRepository;
import com.schemafy.core.erd.repository.SchemaRepository;
import com.schemafy.core.erd.repository.entity.Memo;
import com.schemafy.core.erd.repository.entity.MemoComment;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MemoService {

    private final SchemaRepository schemaRepository;
    private final MemoRepository memoRepository;
    private final MemoCommentRepository memoCommentRepository;
    private final TransactionalOperator transactionalOperator;

    public Mono<MemoDetailResponse> createMemo(CreateMemoRequest request,
            AuthenticatedUser user) {
        return transactionalOperator.transactional(
                ensureSchemaExists(request.schemaId())
                        .then(saveMemo(request, user))
                        .flatMap(memo -> saveFirstComment(memo.getId(),
                                request.body(), user)
                                .map(comment -> MemoDetailResponse.from(memo,
                                        Collections.singletonList(comment)))));
    }

    private Mono<Memo> saveMemo(CreateMemoRequest request,
            AuthenticatedUser user) {
        return memoRepository.save(Memo.builder()
                .schemaId(request.schemaId())
                .authorId(user.userId())
                .positions(request.positions())
                .build());
    }

    private Mono<MemoCommentResponse> saveFirstComment(String memoId,
            String body,
            AuthenticatedUser user) {
        return memoCommentRepository
                .save(MemoComment.builder()
                        .memoId(memoId)
                        .authorId(user.userId())
                        .body(body)
                        .build())
                .map(MemoCommentResponse::from);
    }

    public Mono<MemoDetailResponse> getMemo(String memoId) {
        return memoRepository.findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .flatMap(memo -> memoCommentRepository
                        .findByMemoIdAndDeletedAtIsNullOrderByIdAsc(
                                memoId)
                        .map(MemoCommentResponse::from)
                        .collectList()
                        .map(comments -> MemoDetailResponse.from(memo,
                                comments)));
    }

    public Flux<MemoResponse> getMemosBySchemaId(String schemaId) {
        return memoRepository.findBySchemaIdAndDeletedAtIsNull(schemaId)
                .map(MemoResponse::from);
    }

    public Mono<MemoResponse> updateMemo(String memoId,
            UpdateMemoRequest request, AuthenticatedUser user) {
        return memoRepository.findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .flatMap(memo -> {
                    if (!memo.getAuthorId().equals(user.userId())) {
                        return Mono.error(
                                new BusinessException(ErrorCode.ACCESS_DENIED));
                    }
                    memo.setPositions(request.positions());
                    return memoRepository.save(memo);
                })
                .map(MemoResponse::from);
    }

    public Mono<Void> deleteMemo(String memoId, AuthenticatedUser user) {
        return memoRepository
                .findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .flatMap(memo -> checkDeletePermission(user, memo.getAuthorId())
                        .then(Mono.defer(() -> {
                            memo.delete();
                            return memoRepository.save(memo);
                        })))
                .then();
    }

    public Mono<MemoCommentResponse> createComment(String memoId,
            CreateMemoCommentRequest request, AuthenticatedUser user) {
        return memoRepository.findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .then(memoCommentRepository.save(MemoComment.builder()
                        .memoId(memoId)
                        .authorId(user.userId())
                        .body(request.body())
                        .build()))
                .map(MemoCommentResponse::from);
    }

    public Flux<MemoCommentResponse> getComments(String memoId) {
        return memoRepository.findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .thenMany(memoCommentRepository
                        .findByMemoIdAndDeletedAtIsNullOrderByIdAsc(
                                memoId)
                        .map(MemoCommentResponse::from));
    }

    public Mono<MemoCommentResponse> updateComment(String memoId,
            String commentId, UpdateMemoCommentRequest request,
            AuthenticatedUser user) {
        return memoCommentRepository
                .findByIdAndDeletedAtIsNull(commentId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_MEMO_COMMENT_NOT_FOUND)))
                .flatMap(comment -> {
                    if (!comment.getMemoId().equals(memoId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.COMMON_INVALID_PARAMETER));
                    }
                    return memoRepository
                            .findByIdAndDeletedAtIsNull(memoId)
                            .switchIfEmpty(Mono.error(
                                    new BusinessException(
                                            ErrorCode.ERD_MEMO_NOT_FOUND)))
                            .then(Mono.defer(() -> {
                                if (!comment.getAuthorId()
                                        .equals(user.userId())) {
                                    return Mono.error(new BusinessException(
                                            ErrorCode.ACCESS_DENIED));
                                }
                                comment.setBody(request.body());
                                return memoCommentRepository.save(comment);
                            }));
                })
                .map(MemoCommentResponse::from);
    }

    public Mono<Void> deleteComment(String memoId, String commentId,
            AuthenticatedUser user) {
        return transactionalOperator.transactional(
                memoCommentRepository
                        .findByIdAndDeletedAtIsNull(commentId)
                        .switchIfEmpty(Mono.error(
                                new BusinessException(
                                        ErrorCode.ERD_MEMO_COMMENT_NOT_FOUND)))
                        .flatMap(comment -> validateCommentMemoId(comment,
                                memoId)
                                .then(checkDeletePermission(user,
                                        comment.getAuthorId()))
                                .then(findMemoAndDeleteComment(memoId,
                                        commentId, comment)))
                        .then());
    }

    private Mono<Void> validateCommentMemoId(MemoComment comment,
            String memoId) {
        if (!comment.getMemoId().equals(memoId)) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return Mono.empty();
    }

    private Mono<Void> findMemoAndDeleteComment(String memoId, String commentId,
            MemoComment comment) {
        return memoRepository
                .findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .flatMap(memo -> isFirstComment(memoId, commentId)
                        .flatMap(isFirst -> isFirst
                                ? deleteMemo(memo)
                                : deleteCommentOnly(comment)));
    }

    private Mono<Void> deleteMemo(Memo memo) {
        memo.delete();
        return memoRepository.save(memo).then();
    }

    private Mono<Void> deleteCommentOnly(MemoComment comment) {
        comment.delete();
        return memoCommentRepository.save(comment).then();
    }

    private Mono<Boolean> isFirstComment(String memoId, String commentId) {
        return memoCommentRepository
                .findByMemoIdAndDeletedAtIsNullOrderByIdAsc(memoId)
                .next()
                .map(first -> first.getId().equals(commentId))
                .defaultIfEmpty(false);
    }

    private Mono<Void> ensureSchemaExists(String schemaId) {
        return schemaRepository.findByIdAndDeletedAtIsNull(schemaId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_SCHEMA_NOT_FOUND)))
                .then();
    }

    private Mono<Void> checkDeletePermission(AuthenticatedUser user,
            String authorId) {
        boolean isAdminOrOwner = user.roles().contains(ProjectRole.OWNER)
                || user.roles().contains(ProjectRole.ADMIN);
        if (!isAdminOrOwner && !authorId.equals(user.userId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.ACCESS_DENIED));
        }
        return Mono.empty();
    }

}
