package com.schemafy.core.erd.service;

import java.util.Collections;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
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

    public Mono<MemoDetailResponse> createMemo(CreateMemoRequest request,
            String authorId) {
        return ensureSchemaExists(request.schemaId())
                .then(memoRepository.save(Memo.builder()
                        .schemaId(request.schemaId())
                        .authorId(authorId)
                        .positions(request.positions())
                        .build()))
                .flatMap(memo -> memoCommentRepository.save(MemoComment.builder()
                        .memoId(memo.getId())
                        .authorId(authorId)
                        .body(request.body())
                        .build())
                        .map(MemoCommentResponse::from)
                        .map(comment -> MemoDetailResponse.from(memo,
                                Collections.singletonList(comment))));
    }

    public Mono<MemoDetailResponse> getMemo(String memoId) {
        return memoRepository.findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .flatMap(memo -> memoCommentRepository
                        .findByMemoIdAndDeletedAtIsNull(memoId)
                        .map(MemoCommentResponse::from)
                        .collectList()
                        .map(comments -> MemoDetailResponse.from(memo,
                                comments)));
    }

    public Flux<MemoResponse> getMemosBySchemaId(String schemaId) {
        return memoRepository.findBySchemaIdAndDeletedAtIsNull(schemaId)
                .map(MemoResponse::from);
    }

    public Mono<MemoResponse> updateMemo(UpdateMemoRequest request, String authorId) {
        return memoRepository.findByIdAndDeletedAtIsNull(request.memoId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .flatMap(memo -> {
                    if (!memo.getAuthorId().equals(authorId)) {
                        return Mono.error(
                                new BusinessException(ErrorCode.ACCESS_DENIED));
                    }
                    memo.setPositions(request.positions());
                    return memoRepository.save(memo);
                })
                .map(MemoResponse::from);
    }

    public Mono<Void> deleteMemo(String memoId, String authorId) {
        return memoRepository.findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .flatMap(memo -> {
                    if (!memo.getAuthorId().equals(authorId)) {
                        return Mono.error(
                                new BusinessException(ErrorCode.ACCESS_DENIED));
                    }
                    memo.delete();
                    return memoRepository.save(memo);
                })
                .then(memoCommentRepository
                        .findByMemoIdAndDeletedAtIsNull(memoId)
                        .flatMap(comment -> {
                            comment.delete();
                            return memoCommentRepository.save(comment);
                        })
                        .then());
    }

    public Mono<MemoCommentResponse> createComment(String memoId,
            CreateMemoCommentRequest request, String authorId) {
        return memoRepository.findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .then(memoCommentRepository.save(MemoComment.builder()
                        .memoId(memoId)
                        .authorId(authorId)
                        .body(request.body())
                        .build()))
                .map(MemoCommentResponse::from);
    }

    public Flux<MemoCommentResponse> getComments(String memoId) {
        return memoRepository.findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .thenMany(memoCommentRepository
                        .findByMemoIdAndDeletedAtIsNull(memoId)
                        .map(MemoCommentResponse::from));
    }

    public Mono<MemoCommentResponse> updateComment(
            UpdateMemoCommentRequest request, String authorId) {
        return memoCommentRepository
                .findByIdAndDeletedAtIsNull(request.commentId())
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_MEMO_COMMENT_NOT_FOUND)))
                .flatMap(comment -> {
                    if (!comment.getAuthorId().equals(authorId)) {
                        return Mono.error(
                                new BusinessException(ErrorCode.ACCESS_DENIED));
                    }
                    comment.setBody(request.body());
                    return memoCommentRepository.save(comment);
                })
                .map(MemoCommentResponse::from);
    }

    public Mono<Void> deleteComment(String commentId,
            String authorId) {
        return memoCommentRepository
                .findByIdAndDeletedAtIsNull(commentId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_MEMO_COMMENT_NOT_FOUND)))
                .flatMap(comment -> {
                    if (!comment.getAuthorId().equals(authorId)) {
                        return Mono.error(
                                new BusinessException(ErrorCode.ACCESS_DENIED));
                    }
                    comment.delete();
                    return memoCommentRepository.save(comment)
                            .then(handleMemoDeletionIfNoComments(
                                    comment.getMemoId()));
                })
                .then();
    }

    private Mono<Void> handleMemoDeletionIfNoComments(String memoId) {
        return memoCommentRepository
                .findByMemoIdAndDeletedAtIsNull(memoId)
                .hasElements()
                .flatMap(hasComments -> {
                    if (hasComments) {
                        return Mono.empty();
                    }
                    return memoRepository.findByIdAndDeletedAtIsNull(memoId)
                            .flatMap(memo -> {
                                memo.delete();
                                return memoRepository.save(memo);
                            })
                            .then();
                });
    }

    private Mono<Void> ensureSchemaExists(String schemaId) {
        return schemaRepository.findByIdAndDeletedAtIsNull(schemaId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_SCHEMA_NOT_FOUND)))
                .then();
    }

}
