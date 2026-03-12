package com.schemafy.api.user.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.exception.AuthErrorCode;
import com.schemafy.api.common.security.jwt.JwtProvider;
import com.schemafy.api.common.security.jwt.JwtTokenIssuer;
import com.schemafy.api.user.controller.dto.request.LoginRequest;
import com.schemafy.api.user.controller.dto.request.SignUpRequest;
import com.schemafy.api.user.controller.dto.response.UserInfoResponse;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.application.port.in.GetUserByIdQuery;
import com.schemafy.core.user.application.port.in.GetUserByIdUseCase;
import com.schemafy.core.user.application.port.in.LoginUserUseCase;
import com.schemafy.core.user.application.port.in.SignUpUserUseCase;
import com.schemafy.core.user.domain.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(ApiPath.PUBLIC_API)
@RequiredArgsConstructor
public class AuthController {

  private final SignUpUserUseCase signUpUserUseCase;
  private final LoginUserUseCase loginUserUseCase;
  private final GetUserByIdUseCase getUserByIdUseCase;
  private final JwtProvider jwtProvider;
  private final JwtTokenIssuer jwtTokenIssuer;

  /** 회원가입 API 새로운 사용자를 등록하고 JWT 토큰을 발급합니다. */
  @PostMapping("/users/signup")
  public Mono<ResponseEntity<UserInfoResponse>> signUp(
      @Valid @RequestBody SignUpRequest request) {
    return signUpUserUseCase.signUpUser(request.toCommand())
        .map(user -> ResponseEntity.ok()
            .headers(jwtTokenIssuer.issueTokens(user.id(), user.name()))
            .body(UserInfoResponse.from(user)));
  }

  /** 사용자 인증 후 JWT 토큰을 발급합니다. */
  @PostMapping("/users/login")
  public Mono<ResponseEntity<UserInfoResponse>> login(
      @Valid @RequestBody LoginRequest request) {
    return loginUserUseCase.loginUser(request.toCommand())
        .map(user -> ResponseEntity.ok()
            .headers(jwtTokenIssuer.issueTokens(user.id(), user.name()))
            .body(UserInfoResponse.from(user)));
  }

  /** Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급합니다. */
  @PostMapping("/users/refresh")
  public Mono<ResponseEntity<Void>> refresh(
      ServerHttpRequest request) {
    return Mono.fromCallable(() -> extractRefreshTokenFromCookie(request))
        .flatMap(this::getUserFromRefreshToken)
        .map(user -> ResponseEntity.ok()
            .headers(jwtTokenIssuer.issueTokens(user.id(), user.name()))
            .build());
  }

  /** HTTP 요청의 쿠키에서 Refresh Token을 추출합니다. */
  private String extractRefreshTokenFromCookie(ServerHttpRequest request) {
    var refreshTokenCookie = request.getCookies().getFirst("refreshToken");
    if (refreshTokenCookie == null) {
      throw new DomainException(AuthErrorCode.MISSING_REFRESH_TOKEN);
    }
    return refreshTokenCookie.getValue();
  }

  private Mono<User> getUserFromRefreshToken(
      String refreshToken) {
    return Mono.fromCallable(() -> {
      String userId = jwtProvider.extractUserId(refreshToken);
      String tokenType = jwtProvider.getTokenType(refreshToken);

      if (!JwtProvider.REFRESH_TOKEN.equals(tokenType)) {
        throw new DomainException(AuthErrorCode.INVALID_TOKEN_TYPE);
      }

      if (!jwtProvider.validateToken(refreshToken, userId)) {
        throw new DomainException(AuthErrorCode.INVALID_REFRESH_TOKEN);
      }

      return userId;
    })
        .flatMap(userId -> getUserByIdUseCase.getUserById(new GetUserByIdQuery(userId)))
        .onErrorMap(error -> !(error instanceof DomainException),
            error -> new DomainException(AuthErrorCode.INVALID_REFRESH_TOKEN));
  }

}
