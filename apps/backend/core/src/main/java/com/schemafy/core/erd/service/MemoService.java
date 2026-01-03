package com.schemafy.core.erd.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.schemafy.core.user.controller.dto.response.UserSummaryResponse;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.user.repository.entity.User;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MemoService {

    private final SchemaRepository schemaRepository;
    private final MemoRepository memoRepository;
    private final MemoCommentRepository memoCommentRepository;
    private final UserRepository userRepository;
    private final TransactionalOperator transactionalOperator;

    public Mono<MemoDetailResponse> createMemo(CreateMemoRequest request,
            AuthenticatedUser user) {
        return transactionalOperator.transactional(
                ensureSchemaExists(request.schemaId())
                        .then(saveMemo(request, user))
                        .flatMap(memo -> createInitialCommentAndBuildResponse(
                                memo, request.body(), user)));
    }

    private Mono<MemoDetailResponse> createInitialCommentAndBuildResponse(
            Memo memo, String body, AuthenticatedUser user) {
        return saveFirstComment(memo.getId(), body, user)
                .zipWith(getUserSummary(user.userId()))
                .map(tuple -> MemoDetailResponse.from(memo,
                        Collections.singletonList(tuple.getT1()),
                        tuple.getT2()));
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
                .flatMap(comment -> getUserSummary(user.userId())
                        .map(userInfo -> MemoCommentResponse.from(comment,
                                userInfo)));
    }

    public Mono<MemoDetailResponse> getMemo(String memoId) {
        return memoRepository.findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .flatMap(this::buildMemoDetailResponse);
    }

    private Mono<MemoDetailResponse> buildMemoDetailResponse(Memo memo) {
        Mono<List<MemoCommentResponse>> commentsMono = getComments(memo.getId())
                .collectList();
        Mono<UserSummaryResponse> authorMono = getUserSummary(
                memo.getAuthorId());

        return Mono.zip(commentsMono, authorMono)
                .map(tuple -> MemoDetailResponse.from(memo, tuple.getT1(),
                        tuple.getT2()));
    }

    public Flux<MemoResponse> getMemosBySchemaId(String schemaId) {
        return memoRepository.findBySchemaIdAndDeletedAtIsNull(schemaId)
                .collectList()
                .flatMapMany(this::mapMemosToResponses);
    }

    private Flux<MemoResponse> mapMemosToResponses(List<Memo> memos) {
        if (memos.isEmpty()) {
            return Flux.empty();
        }
        Set<String> authorIds = memos.stream()
                .map(Memo::getAuthorId)
                .collect(Collectors.toSet());

        return userRepository.findAllById(authorIds)
                .collectMap(User::getId, UserSummaryResponse::from)
                .flatMapMany(userMap -> Flux.fromIterable(memos)
                        .map(memo -> MemoResponse.from(memo,
                                getUserFromMap(userMap, memo.getAuthorId()))));
    }

    public Mono<MemoResponse> updateMemo(String memoId,
            UpdateMemoRequest request, AuthenticatedUser user) {
        return memoRepository.findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .flatMap(memo -> updateMemoIfAuthor(memo, request, user));
    }

    private Mono<MemoResponse> updateMemoIfAuthor(Memo memo,
            UpdateMemoRequest request, AuthenticatedUser user) {
        if (!memo.getAuthorId().equals(user.userId())) {
            return Mono.error(new BusinessException(ErrorCode.ACCESS_DENIED));
        }
        memo.setPositions(request.positions());
        return memoRepository.save(memo)
                .flatMap(savedMemo -> getUserSummary(user.userId())
                        .map(userInfo -> MemoResponse.from(savedMemo,
                                userInfo)));
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
                .then(saveComment(memoId, request.body(), user));
    }

    private Mono<MemoCommentResponse> saveComment(String memoId, String body,
            AuthenticatedUser user) {
        return memoCommentRepository.save(MemoComment.builder()
                .memoId(memoId)
                .authorId(user.userId())
                .body(body)
                .build())
                .flatMap(comment -> getUserSummary(user.userId())
                        .map(userInfo -> MemoCommentResponse.from(comment,
                                userInfo)));
    }

    public Flux<MemoCommentResponse> getComments(String memoId) {
        return memoRepository.findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .thenMany(memoCommentRepository
                        .findByMemoIdAndDeletedAtIsNullOrderByIdAsc(memoId)
                        .collectList()
                        .flatMapMany(this::mapCommentsToResponses));
    }

    private Flux<MemoCommentResponse> mapCommentsToResponses(
            List<MemoComment> comments) {
        if (comments.isEmpty()) {
            return Flux.empty();
        }
        Set<String> authorIds = comments.stream()
                .map(MemoComment::getAuthorId)
                .collect(Collectors.toSet());

        return userRepository.findAllById(authorIds)
                .collectMap(User::getId, UserSummaryResponse::from)
                .flatMapMany(userMap -> Flux.fromIterable(comments)
                        .map(comment -> MemoCommentResponse.from(comment,
                                getUserFromMap(userMap,
                                        comment.getAuthorId()))));
    }

    public Mono<MemoCommentResponse> updateComment(String memoId,
            String commentId, UpdateMemoCommentRequest request,
            AuthenticatedUser user) {
        return memoCommentRepository
                .findByIdAndDeletedAtIsNull(commentId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.ERD_MEMO_COMMENT_NOT_FOUND)))
                .flatMap(comment -> updateCommentIfValid(comment, memoId,
                        request, user));
    }

    private Mono<MemoCommentResponse> updateCommentIfValid(MemoComment comment,
            String memoId, UpdateMemoCommentRequest request,
            AuthenticatedUser user) {
        if (!comment.getMemoId().equals(memoId)) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }
        return memoRepository.findByIdAndDeletedAtIsNull(memoId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.ERD_MEMO_NOT_FOUND)))
                .then(Mono.defer(() -> {
                    if (!comment.getAuthorId().equals(user.userId())) {
                        return Mono.error(
                                new BusinessException(ErrorCode.ACCESS_DENIED));
                    }
                    comment.setBody(request.body());
                    return memoCommentRepository.save(comment)
                            .flatMap(savedComment -> getUserSummary(
                                    user.userId())
                                    .map(userInfo -> MemoCommentResponse.from(
                                            savedComment, userInfo)));
                }));
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
                .flatMap(memo -> isLastRemainingComment(memoId, commentId)
                        .flatMap(isLast -> isLast
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

    private Mono<Boolean> isLastRemainingComment(String memoId, String commentId) {
        return memoCommentRepository
                .findByMemoIdAndDeletedAtIsNullOrderByIdAsc(memoId)
                .collectList()
                .map(comments -> comments.size() == 1
                        && comments.get(0).getId().equals(commentId))
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

    private Mono<UserSummaryResponse> getUserSummary(String userId) {
        return userRepository.findById(userId)
                .map(UserSummaryResponse::from)
                .defaultIfEmpty(new UserSummaryResponse(userId, "Unknown"));
    }

    private UserSummaryResponse getUserFromMap(
            Map<String, UserSummaryResponse> userMap, String userId) {
        return userMap.getOrDefault(userId,
                new UserSummaryResponse(userId, "Unknown"));
    }

}
