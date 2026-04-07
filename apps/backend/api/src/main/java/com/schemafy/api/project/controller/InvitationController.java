package com.schemafy.api.project.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.type.CursorResponse;
import com.schemafy.api.project.controller.dto.response.MyInvitationResponse;
import com.schemafy.core.project.application.port.in.GetMyInvitationsQuery;
import com.schemafy.core.project.application.port.in.GetMyInvitationsUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class InvitationController {

  private final GetMyInvitationsUseCase getMyInvitationsUseCase;

  @GetMapping("/users/me/invitations")
  public Mono<CursorResponse<MyInvitationResponse>> getMyInvitations(
      @RequestParam(required = false) @Size(max = 64) String cursorId,
      @RequestParam(defaultValue = "5") @Positive @Max(100) int size,
      Authentication auth) {
    String currentUserId = auth.getName();
    return getMyInvitationsUseCase.getMyInvitations(
        new GetMyInvitationsQuery(currentUserId, cursorId, size))
        .map(result -> CursorResponse.of(
            result.content().stream().map(MyInvitationResponse::of).toList(),
            result.size(),
            result.hasNext(),
            result.nextCursorId()));
  }

}
