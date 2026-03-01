package com.schemafy.core.erd.controller;

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

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.principal.AuthenticatedUser;
import com.schemafy.core.erd.controller.dto.request.CreateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.CreateMemoRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoCommentRequest;
import com.schemafy.core.erd.controller.dto.request.UpdateMemoRequest;
import com.schemafy.core.erd.controller.dto.response.MemoCommentResponse;
import com.schemafy.core.erd.controller.dto.response.MemoDetailResponse;
import com.schemafy.core.erd.controller.dto.response.MemoResponse;
import com.schemafy.core.erd.service.MemoService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class MemoController {

  private final MemoService memoService;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER')")
  @PostMapping("/memos")
  public Mono<MemoDetailResponse> createMemo(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody CreateMemoRequest request) {
    return memoService.createMemo(request, user);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/memos/{memoId}")
  public Mono<MemoDetailResponse> getMemo(
      @PathVariable String memoId) {
    return memoService.getMemo(memoId);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/schemas/{schemaId}/memos")
  public Mono<List<MemoResponse>> getMemosBySchemaId(
      @PathVariable String schemaId) {
    return memoService.getMemosBySchemaId(schemaId)
        .collectList();
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER')")
  @PutMapping("/memos/{memoId}")
  public Mono<MemoResponse> updateMemo(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String memoId,
      @Valid @RequestBody UpdateMemoRequest request) {
    return memoService.updateMemo(memoId, request, user);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER')")
  @DeleteMapping("/memos/{memoId}")
  public Mono<Void> deleteMemo(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String memoId) {
    return memoService.deleteMemo(memoId, user)
        .then();
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER')")
  @PostMapping("/memos/{memoId}/comments")
  public Mono<MemoCommentResponse> createMemoComment(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String memoId,
      @Valid @RequestBody CreateMemoCommentRequest request) {
    return memoService.createComment(memoId, request, user);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/memos/{memoId}/comments")
  public Mono<List<MemoCommentResponse>> getMemoComments(
      @PathVariable String memoId) {
    return memoService.getComments(memoId)
        .collectList();
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER')")
  @PutMapping("/memos/{memoId}/comments/{commentId}")
  public Mono<MemoCommentResponse> updateMemoComment(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String memoId,
      @PathVariable String commentId,
      @Valid @RequestBody UpdateMemoCommentRequest request) {
    return memoService.updateComment(memoId, commentId, request, user);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER')")
  @DeleteMapping("/memos/{memoId}/comments/{commentId}")
  public Mono<Void> deleteMemoComment(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String memoId,
      @PathVariable String commentId) {
    return memoService.deleteComment(memoId, commentId, user)
        .then();
  }

}
