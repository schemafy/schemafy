package com.schemafy.api.mcp.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.security.principal.AuthenticatedUser;
import com.schemafy.api.mcp.controller.dto.request.McpTokenRevokeRequest;
import com.schemafy.api.mcp.controller.dto.response.McpTokenIssueResponse;
import com.schemafy.api.mcp.service.McpTokenService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class McpTokenController {

  private final McpTokenService mcpTokenService;

  @PostMapping("/mcp/tokens")
  public Mono<ResponseEntity<McpTokenIssueResponse>> issue(
      @AuthenticationPrincipal AuthenticatedUser user) {
    return mcpTokenService.issue(user.userId())
        .map(McpTokenIssueResponse::from)
        .map(ResponseEntity::ok);
  }

  @PostMapping("/mcp/tokens/revoke")
  public Mono<ResponseEntity<Void>> revoke(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody McpTokenRevokeRequest request) {
    return mcpTokenService.revoke(user.userId(), request.token())
        .thenReturn(ResponseEntity.noContent().build());
  }

}
