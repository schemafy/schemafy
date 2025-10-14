package com.schemafy.core.user.controller;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.common.security.jwt.JwtTokenIssuer;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.user.service.UserService;
import com.schemafy.core.user.controller.dto.request.LoginRequest;
import com.schemafy.core.user.controller.dto.request.SignUpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.USERS)
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final JwtProvider jwtProvider;

    @PostMapping("/signup")
    public Mono<ResponseEntity<BaseResponse<UserInfoResponse>>> signUp(@Valid @RequestBody SignUpRequest request) {
        return userService.signUp(request.toCommand())
                .map(user -> jwtTokenIssuer.issueTokens(
                        user.getId(),
                        BaseResponse.success(UserInfoResponse.from(user))
                ));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<BaseResponse<UserInfoResponse>>> login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request.toCommand())
                .map(user -> jwtTokenIssuer.issueTokens(
                        user.getId(),
                        BaseResponse.success(UserInfoResponse.from(user))
                ));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<BaseResponse<Void>>> refresh(ServerHttpRequest request) {
        return Mono.fromCallable(() -> {
                    // HttpOnly Cookie에서 Refresh Token 추출
                    var refreshTokenCookie = request.getCookies().getFirst("refreshToken");
                    if (refreshTokenCookie == null) {
                        throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
                    }

                    String refreshToken = refreshTokenCookie.getValue();
                    String userId = jwtProvider.extractUserId(refreshToken);
                    String tokenType = jwtProvider.getTokenType(refreshToken);

                    if (!JwtProvider.ACCESS_TOKEN.equals(tokenType)) {
                        throw new BusinessException(ErrorCode.INVALID_TOKEN_TYPE);
                    }

                    if (!jwtProvider.validateToken(refreshToken, userId)) {
                        throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
                    }

                    return userId;
                })
                .map(userId -> jwtTokenIssuer.issueTokens(userId, BaseResponse.<Void>success(null)))
                .onErrorResume(BusinessException.class, Mono::error)
                .onErrorMap(e -> !(e instanceof BusinessException),
                        e -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    @GetMapping("/{userId}")
    public Mono<ResponseEntity<BaseResponse<UserInfoResponse>>> getMember(@PathVariable String userId) {
        return userService.getUserById(userId)
                .map(BaseResponse::success)
                .map(ResponseEntity::ok);
    }
}
