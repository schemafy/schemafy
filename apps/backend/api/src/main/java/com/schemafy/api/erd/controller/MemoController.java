package com.schemafy.api.erd.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.security.principal.AuthenticatedUser;
import com.schemafy.api.erd.controller.dto.request.CreateMemoCommentRequest;
import com.schemafy.api.erd.controller.dto.request.CreateMemoRequest;
import com.schemafy.api.erd.controller.dto.request.UpdateMemoCommentRequest;
import com.schemafy.api.erd.controller.dto.request.UpdateMemoRequest;
import com.schemafy.api.erd.controller.dto.response.MemoCommentResponse;
import com.schemafy.api.erd.controller.dto.response.MemoDetailResponse;
import com.schemafy.api.erd.controller.dto.response.MemoResponse;
import com.schemafy.api.erd.service.MemoOrchestrator;
import com.schemafy.api.erd.service.memo.MemoApiCommandMapper;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommentUseCase;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class MemoController {

  private final MemoOrchestrator memoOrchestrator;
  private final DeleteMemoUseCase deleteMemoUseCase;
  private final DeleteMemoCommentUseCase deleteMemoCommentUseCase;
  private final MemoApiCommandMapper commandMapper;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER')")
  @PostMapping("/memos")
  public Mono<MemoDetailResponse> createMemo(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody CreateMemoRequest request) {
    return memoOrchestrator.createMemo(request, user);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/memos/{memoId}")
  public Mono<MemoDetailResponse> getMemo(
      @PathVariable String memoId) {
    return memoOrchestrator.getMemo(memoId);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/schemas/{schemaId}/memos")
  public Mono<List<MemoResponse>> getMemosBySchemaId(
      @PathVariable String schemaId) {
    return memoOrchestrator.getMemosBySchemaId(schemaId)
        .collectList();
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER')")
  @PutMapping("/memos/{memoId}")
  public Mono<MemoResponse> updateMemo(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String memoId,
      @Valid @RequestBody UpdateMemoRequest request) {
    return memoOrchestrator.updateMemo(memoId, request, user);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER')")
  @DeleteMapping("/memos/{memoId}")
  public Mono<Void> deleteMemo(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String memoId) {
    return deleteMemoUseCase.deleteMemo(
        commandMapper.toDeleteMemoCommand(memoId, user))
        .then();
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER')")
  @PostMapping("/memos/{memoId}/comments")
  public Mono<MemoCommentResponse> createMemoComment(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String memoId,
      @Valid @RequestBody CreateMemoCommentRequest request) {
    return memoOrchestrator.createComment(memoId, request, user);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/memos/{memoId}/comments")
  public Mono<List<MemoCommentResponse>> getMemoComments(
      @PathVariable String memoId) {
    return memoOrchestrator.getComments(memoId)
        .collectList();
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER')")
  @PutMapping("/memo-comments/{commentId}")
  public Mono<MemoCommentResponse> updateMemoComment(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String commentId,
      @Valid @RequestBody UpdateMemoCommentRequest request) {
    return memoOrchestrator.updateComment(commentId, request, user);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER')")
  @DeleteMapping("/memo-comments/{commentId}")
  public Mono<Void> deleteMemoComment(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String commentId) {
    return deleteMemoCommentUseCase.deleteMemoComment(
        commandMapper.toDeleteMemoCommentCommand(commentId, user))
        .then();
  }

}
