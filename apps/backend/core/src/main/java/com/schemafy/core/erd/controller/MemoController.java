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
import com.schemafy.core.common.type.BaseResponse;
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

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  @PostMapping("/memos")
  public Mono<BaseResponse<MemoDetailResponse>> createMemo(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody CreateMemoRequest request) {
    return memoService.createMemo(request, user)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @GetMapping("/memos/{memoId}")
  public Mono<BaseResponse<MemoDetailResponse>> getMemo(
      @PathVariable String memoId) {
    return memoService.getMemo(memoId)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  @PutMapping("/memos/{memoId}")
  public Mono<BaseResponse<MemoResponse>> updateMemo(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String memoId,
      @Valid @RequestBody UpdateMemoRequest request) {
    return memoService.updateMemo(memoId, request, user)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  @DeleteMapping("/memos/{memoId}")
  public Mono<BaseResponse<Void>> deleteMemo(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String memoId) {
    return memoService.deleteMemo(memoId, user)
        .then(Mono.just(BaseResponse.success(null)));
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  @PostMapping("/memos/{memoId}/comments")
  public Mono<BaseResponse<MemoCommentResponse>> createMemoComment(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String memoId,
      @Valid @RequestBody CreateMemoCommentRequest request) {
    return memoService.createComment(memoId, request, user)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @GetMapping("/memos/{memoId}/comments")
  public Mono<BaseResponse<List<MemoCommentResponse>>> getMemoComments(
      @PathVariable String memoId) {
    return memoService.getComments(memoId)
        .collectList()
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  @PutMapping("/memos/{memoId}/comments/{commentId}")
  public Mono<BaseResponse<MemoCommentResponse>> updateMemoComment(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String memoId,
      @PathVariable String commentId,
      @Valid @RequestBody UpdateMemoCommentRequest request) {
    return memoService.updateComment(memoId, commentId, request, user)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  @DeleteMapping("/memos/{memoId}/comments/{commentId}")
  public Mono<BaseResponse<Void>> deleteMemoComment(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable String memoId,
      @PathVariable String commentId) {
    return memoService.deleteComment(memoId, commentId, user)
        .then(Mono.just(BaseResponse.success(null)));
  }

}
