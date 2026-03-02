package com.schemafy.core.erd.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.security.principal.AuthenticatedUser;
import com.schemafy.core.erd.controller.dto.request.CreateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.CreateMemoRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoRequest;
import com.schemafy.core.erd.controller.dto.response.MemoCommentResponse;
import com.schemafy.core.erd.controller.dto.response.MemoDetailResponse;
import com.schemafy.core.erd.controller.dto.response.MemoResponse;
import com.schemafy.core.erd.service.memo.MemoApiCommandMapper;
import com.schemafy.core.erd.service.memo.MemoApiResponseMapper;
import com.schemafy.core.user.controller.dto.response.UserSummaryResponse;
import com.schemafy.domain.erd.memo.application.port.in.CreateMemoCommentUseCase;
import com.schemafy.domain.erd.memo.application.port.in.CreateMemoUseCase;
import com.schemafy.domain.erd.memo.application.port.in.GetMemoCommentsUseCase;
import com.schemafy.domain.erd.memo.application.port.in.GetMemoUseCase;
import com.schemafy.domain.erd.memo.application.port.in.GetMemosBySchemaIdUseCase;
import com.schemafy.domain.erd.memo.application.port.in.UpdateMemoCommentUseCase;
import com.schemafy.domain.erd.memo.application.port.in.UpdateMemoPositionUseCase;
import com.schemafy.domain.erd.memo.domain.Memo;
import com.schemafy.domain.erd.memo.domain.MemoComment;
import com.schemafy.domain.erd.memo.domain.MemoDetail;
import com.schemafy.domain.user.application.port.in.GetUsersByIdsQuery;
import com.schemafy.domain.user.application.port.in.GetUsersByIdsUseCase;
import com.schemafy.domain.user.domain.User;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MemoOrchestrator {

  private final CreateMemoUseCase createMemoUseCase;
  private final GetMemoUseCase getMemoUseCase;
  private final GetMemosBySchemaIdUseCase getMemosBySchemaIdUseCase;
  private final UpdateMemoPositionUseCase updateMemoPositionUseCase;
  private final CreateMemoCommentUseCase createMemoCommentUseCase;
  private final GetMemoCommentsUseCase getMemoCommentsUseCase;
  private final UpdateMemoCommentUseCase updateMemoCommentUseCase;
  private final GetUsersByIdsUseCase getUsersByIdsUseCase;
  private final MemoApiCommandMapper commandMapper;
  private final MemoApiResponseMapper responseMapper;

  public Mono<MemoDetailResponse> createMemo(CreateMemoRequest request,
      AuthenticatedUser user) {
    return createMemoUseCase.createMemo(
        commandMapper.toCreateMemoCommand(request, user))
        .flatMap(this::buildMemoDetailResponse);
  }

  public Mono<MemoDetailResponse> getMemo(String memoId) {
    return getMemoUseCase.getMemo(
        commandMapper.toGetMemoQuery(memoId))
        .flatMap(this::buildMemoDetailResponse);
  }

  public Flux<MemoResponse> getMemosBySchemaId(String schemaId) {
    return getMemosBySchemaIdUseCase.getMemosBySchemaId(
        commandMapper.toGetMemosBySchemaIdQuery(schemaId))
        .collectList()
        .flatMapMany(this::mapMemosToResponses);
  }

  public Mono<MemoResponse> updateMemo(String memoId,
      UpdateMemoRequest request, AuthenticatedUser user) {
    return updateMemoPositionUseCase.updateMemoPosition(
        commandMapper.toUpdateMemoPositionCommand(memoId, request, user))
        .flatMap(memo -> getUserSummary(memo.authorId())
            .map(author -> responseMapper.toMemoResponse(memo, author)));
  }

  public Mono<MemoCommentResponse> createComment(String memoId,
      CreateMemoCommentRequest request, AuthenticatedUser user) {
    return createMemoCommentUseCase.createMemoComment(
        commandMapper.toCreateMemoCommentCommand(memoId, request, user))
        .flatMap(comment -> getUserSummary(comment.authorId())
            .map(author -> responseMapper.toMemoCommentResponse(comment,
                author)));
  }

  public Flux<MemoCommentResponse> getComments(String memoId) {
    return getMemoCommentsUseCase.getMemoComments(
        commandMapper.toGetMemoCommentsQuery(memoId))
        .collectList()
        .flatMapMany(this::mapCommentsToResponses);
  }

  public Mono<MemoCommentResponse> updateComment(String memoId,
      String commentId, UpdateMemoCommentRequest request,
      AuthenticatedUser user) {
    return updateMemoCommentUseCase.updateMemoComment(
        commandMapper.toUpdateMemoCommentCommand(memoId, commentId, request,
            user))
        .flatMap(comment -> getUserSummary(comment.authorId())
            .map(author -> responseMapper.toMemoCommentResponse(comment,
                author)));
  }

  private Mono<MemoDetailResponse> buildMemoDetailResponse(MemoDetail detail) {
    Set<String> authorIds = detail.comments().stream()
        .map(MemoComment::authorId)
        .collect(Collectors.toSet());
    authorIds.add(detail.memo().authorId());

    return loadUserSummaryMap(authorIds)
        .map(userMap -> {
          List<MemoCommentResponse> comments = detail.comments().stream()
              .map(comment -> responseMapper.toMemoCommentResponse(comment,
                  getUserFromMap(userMap, comment.authorId())))
              .toList();

          return MemoDetailResponse.builder()
              .id(detail.memo().id())
              .schemaId(detail.memo().schemaId())
              .author(getUserFromMap(userMap, detail.memo().authorId()))
              .positions(detail.memo().positions())
              .createdAt(detail.memo().createdAt())
              .updatedAt(detail.memo().updatedAt())
              .comments(comments)
              .build();
        });
  }

  private Flux<MemoResponse> mapMemosToResponses(List<Memo> memos) {
    if (memos.isEmpty()) {
      return Flux.empty();
    }

    Set<String> authorIds = memos.stream()
        .map(Memo::authorId)
        .collect(Collectors.toSet());

    return loadUserSummaryMap(authorIds)
        .flatMapMany(userMap -> Flux.fromIterable(memos)
            .map(memo -> responseMapper.toMemoResponse(memo,
                getUserFromMap(userMap, memo.authorId()))));
  }

  private Flux<MemoCommentResponse> mapCommentsToResponses(
      List<MemoComment> comments) {
    if (comments.isEmpty()) {
      return Flux.empty();
    }

    Set<String> authorIds = comments.stream()
        .map(MemoComment::authorId)
        .collect(Collectors.toSet());

    return loadUserSummaryMap(authorIds)
        .flatMapMany(userMap -> Flux.fromIterable(comments)
            .map(comment -> responseMapper.toMemoCommentResponse(comment,
                getUserFromMap(userMap, comment.authorId()))));
  }

  private Mono<Map<String, UserSummaryResponse>> loadUserSummaryMap(
      Set<String> userIds) {
    if (userIds.isEmpty()) {
      return Mono.just(Collections.emptyMap());
    }
    return getUsersByIdsUseCase.getUsersByIds(new GetUsersByIdsQuery(userIds))
        .collectMap(User::id,
            user -> new UserSummaryResponse(user.id(), user.name()));
  }

  private Mono<UserSummaryResponse> getUserSummary(String userId) {
    return loadUserSummaryMap(Collections.singleton(userId))
        .map(userMap -> getUserFromMap(userMap, userId));
  }

  private UserSummaryResponse getUserFromMap(
      Map<String, UserSummaryResponse> userMap, String userId) {
    return userMap.getOrDefault(userId,
        new UserSummaryResponse(userId, "Unknown"));
  }

}
