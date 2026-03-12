package com.schemafy.core.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.jwt.JwtTokenIssuer;
import com.schemafy.core.common.security.principal.AuthenticatedUser;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;
import com.schemafy.domain.user.application.port.in.GetUserByIdQuery;
import com.schemafy.domain.user.application.port.in.GetUserByIdUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class UserController {

  private final GetUserByIdUseCase getUserByIdUseCase;
  private final JwtTokenIssuer jwtTokenIssuer;

  @GetMapping("/users")
  public Mono<ResponseEntity<UserInfoResponse>> getMyInfo(
      @AuthenticationPrincipal AuthenticatedUser user) {
    return getUserByIdUseCase.getUserById(new GetUserByIdQuery(user.userId()))
        .map(UserInfoResponse::from)
        .map(ResponseEntity::ok);
  }

  /** 사용자 정보를 조회합니다. 인증된 사용자는 타 사용자의 프로필도 조회할 수 있습니다. */
  @GetMapping("/users/{userId}")
  public Mono<ResponseEntity<UserInfoResponse>> getUser(
      @PathVariable String userId) {
    return getUserByIdUseCase.getUserById(new GetUserByIdQuery(userId))
        .map(UserInfoResponse::from)
        .map(ResponseEntity::ok);
  }

  /** 로그아웃 API - 인증된 사용자만 호출 가능하며, 쿠키에 저장된 토큰을 만료시킵니다. */
  @PostMapping("/users/logout")
  public Mono<ResponseEntity<Void>> logout(
      @AuthenticationPrincipal AuthenticatedUser user) {
    log.info("User logout: userId={}", user.userId());
    return Mono.just(ResponseEntity.ok()
        .headers(jwtTokenIssuer.expireTokens())
        .build());
  }

}
