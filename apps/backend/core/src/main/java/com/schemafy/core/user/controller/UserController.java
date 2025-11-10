package com.schemafy.core.user.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.security.jwt.JwtTokenIssuer;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.user.controller.dto.request.LoginRequest;
import com.schemafy.core.user.controller.dto.request.SignUpRequest;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;
import com.schemafy.core.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JwtTokenIssuer jwtTokenIssuer;

    @PostMapping("/users/signup")
    public Mono<ResponseEntity<BaseResponse<UserInfoResponse>>> signUp(
            @Valid @RequestBody SignUpRequest request) {
        return userService.signUp(request.toCommand())
                .map(user -> jwtTokenIssuer.issueTokens(
                        user.getId(),
                        BaseResponse.success(UserInfoResponse.from(user))));
    }

    @PostMapping("/users/login")
    public Mono<ResponseEntity<BaseResponse<UserInfoResponse>>> login(
            @Valid @RequestBody LoginRequest request) {
        return userService.login(request.toCommand())
                .map(user -> jwtTokenIssuer.issueTokens(
                        user.getId(),
                        BaseResponse.success(UserInfoResponse.from(user))));
    }

    @PostMapping("/users/refresh")
    public Mono<ResponseEntity<BaseResponse<Void>>> refresh(
            ServerHttpRequest request) {
        return Mono.fromCallable(() -> extractRefreshTokenFromCookie(request))
                .flatMap(userService::getUserIdFromRefreshToken)
                .map(userId -> jwtTokenIssuer.issueTokens(userId,
                        BaseResponse.success(null)));
    }

    private String extractRefreshTokenFromCookie(ServerHttpRequest request) {
        var refreshTokenCookie = request.getCookies().getFirst("refreshToken");
        if (refreshTokenCookie == null) {
            throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
        }
        return refreshTokenCookie.getValue();
    }

    @GetMapping("/users/{userId}")
    public Mono<ResponseEntity<BaseResponse<UserInfoResponse>>> getUser(
            @PathVariable String userId) {
        return userService.getUserById(userId)
                .map(BaseResponse::success)
                .map(ResponseEntity::ok);
    }
}
