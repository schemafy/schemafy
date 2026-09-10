package com.schemafy.api.user.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.security.principal.AuthenticatedUser;
import com.schemafy.api.user.controller.dto.request.ChangePasswordRequest;
import com.schemafy.api.user.controller.dto.request.PasswordResetRequest;
import com.schemafy.core.user.application.port.in.ChangePasswordUseCase;
import com.schemafy.core.user.application.port.in.RequestPasswordResetUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.PUBLIC_API)
@RequiredArgsConstructor
public class PasswordController {

  private final RequestPasswordResetUseCase requestPasswordResetUseCase;
  private final ChangePasswordUseCase changePasswordUseCase;

  @PostMapping("/users/password-reset-requests")
  public Mono<ResponseEntity<Void>> requestPasswordReset(
      @Valid @RequestBody PasswordResetRequest request) {
    return requestPasswordResetUseCase.requestPasswordReset(request.toCommand())
        .thenReturn(ResponseEntity.ok().build());
  }

  @PostMapping("/users/password")
  public Mono<ResponseEntity<Void>> changePassword(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody ChangePasswordRequest request) {
    String userId = authenticatedUser == null ? null : authenticatedUser.userId();
    return changePasswordUseCase.changePassword(userId, request.toCommand())
        .thenReturn(ResponseEntity.noContent().build());
  }

}
