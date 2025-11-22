package com.schemafy.core.erd.controller;

import java.util.List;

import jakarta.validation.Valid;

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
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
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

    @PostMapping("/memos")
    public Mono<BaseResponse<MemoDetailResponse>> createMemo(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateMemoRequest request) {
        return memoService.createMemo(request, userId)
                .map(BaseResponse::success);
    }

    @GetMapping("/memos/{memoId}")
    public Mono<BaseResponse<MemoDetailResponse>> getMemo(
            @PathVariable String memoId) {
        return memoService.getMemo(memoId)
                .map(BaseResponse::success);
    }

    @GetMapping("/memos/schema/{schemaId}")
    public Mono<BaseResponse<List<MemoResponse>>> getMemosBySchemaId(
            @PathVariable String schemaId) {
        return memoService.getMemosBySchemaId(schemaId)
                .collectList()
                .map(BaseResponse::success);
    }

    @PutMapping("/memos/{memoId}")
    public Mono<BaseResponse<MemoResponse>> updateMemo(
            @AuthenticationPrincipal String userId,
            @PathVariable String memoId,
            @Valid @RequestBody UpdateMemoRequest request) {
        if (!memoId.equals(request.memoId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }

        return memoService.updateMemo(request, userId)
                .map(BaseResponse::success);
    }

    @DeleteMapping("/memos/{memoId}")
    public Mono<BaseResponse<Void>> deleteMemo(
            @AuthenticationPrincipal String userId,
            @PathVariable String memoId) {
        return memoService.deleteMemo(memoId, userId)
                .then(Mono.just(BaseResponse.success(null)));
    }

    @PostMapping("/memos/{memoId}/comments")
    public Mono<BaseResponse<MemoCommentResponse>> createMemoComment(
            @AuthenticationPrincipal String userId,
            @PathVariable String memoId,
            @Valid @RequestBody CreateMemoCommentRequest request) {
        return memoService.createComment(memoId, request, userId)
                .map(BaseResponse::success);
    }

    @GetMapping("/memos/{memoId}/comments")
    public Mono<BaseResponse<List<MemoCommentResponse>>> getMemoComments(
            @PathVariable String memoId) {
        return memoService.getComments(memoId)
                .collectList()
                .map(BaseResponse::success);
    }

    @PutMapping("/memos/{memoId}/comments/{commentId}")
    public Mono<BaseResponse<MemoCommentResponse>> updateMemoComment(
            @AuthenticationPrincipal String userId,
            @PathVariable String memoId,
            @PathVariable String commentId,
            @Valid @RequestBody UpdateMemoCommentRequest request) {
        if (!memoId.equals(request.memoId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }

        if (!commentId.equals(request.commentId())) {
            return Mono.error(
                    new BusinessException(ErrorCode.COMMON_INVALID_PARAMETER));
        }

        return memoService.updateComment(request, userId)
                .map(BaseResponse::success);
    }

    @DeleteMapping("/memos/{memoId}/comments/{commentId}")
    public Mono<BaseResponse<Void>> deleteMemoComment(
            @AuthenticationPrincipal String userId,
            @PathVariable String memoId,
            @PathVariable String commentId) {
        return memoService.deleteComment(commentId, userId)
                .then(Mono.just(BaseResponse.success(null)));
    }

}
