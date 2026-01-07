package com.schemafy.core.project.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.CreateShareLinkRequest;
import com.schemafy.core.project.controller.dto.response.ShareLinkResponse;
import com.schemafy.core.project.service.ShareLinkService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class ShareLinkController {

  private final ShareLinkService shareLinkService;

  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  @PostMapping("/workspaces/{workspaceId}/projects/{projectId}/share-links")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<BaseResponse<ShareLinkResponse>> createShareLink(
      @PathVariable String workspaceId, @PathVariable String projectId,
      @Valid @RequestBody CreateShareLinkRequest request,
      Authentication authentication) {
    String userId = authentication.getName();
    return shareLinkService
        .createShareLink(workspaceId, projectId, request, userId)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  @GetMapping("/workspaces/{workspaceId}/projects/{projectId}/share-links")
  public Mono<BaseResponse<PageResponse<ShareLinkResponse>>> getShareLinks(
      @PathVariable String workspaceId, @PathVariable String projectId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Authentication authentication) {
    String userId = authentication.getName();
    return shareLinkService
        .getShareLinks(workspaceId, projectId, userId, page, size)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @GetMapping("/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}")
  public Mono<BaseResponse<ShareLinkResponse>> getShareLink(
      @PathVariable String workspaceId, @PathVariable String projectId,
      @PathVariable String shareLinkId, Authentication authentication) {
    String userId = authentication.getName();
    return shareLinkService
        .getShareLink(workspaceId, projectId, shareLinkId, userId)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
  @PatchMapping("/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}/revoke")
  public Mono<BaseResponse<Void>> revokeShareLink(
      @PathVariable String workspaceId, @PathVariable String projectId,
      @PathVariable String shareLinkId, Authentication authentication) {
    String userId = authentication.getName();
    return shareLinkService
        .revokeShareLink(workspaceId, projectId, shareLinkId, userId)
        .thenReturn(BaseResponse.success(null));
  }

  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  @DeleteMapping("/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deleteShareLink(@PathVariable String workspaceId,
      @PathVariable String projectId, @PathVariable String shareLinkId,
      Authentication authentication) {
    String userId = authentication.getName();
    return shareLinkService.deleteShareLink(workspaceId, projectId,
        shareLinkId, userId);
  }

}
