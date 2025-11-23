package com.schemafy.core.user.controller;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.security.jwt.JwtTokenIssuer;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.user.controller.dto.request.LoginRequest;
import com.schemafy.core.user.controller.dto.request.SignUpRequest;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;
import com.schemafy.core.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtTokenIssuer jwtTokenIssuer;

    /**
     * 회원가입 API
     * 새로운 사용자를 등록하고 JWT 토큰을 발급합니다.
     *
     * @param request 회원가입 요청 정보 (이메일, 이름, 비밀번호)
     * @return 생성된 사용자 정보 및 JWT 토큰
     */
    @PostMapping("/users/signup")
    public Mono<ResponseEntity<BaseResponse<UserInfoResponse>>> signUp(
            @Valid @RequestBody SignUpRequest request) {
        return userService.signUp(request.toCommand())
                .map(user -> jwtTokenIssuer.issueTokens(
                        user.getId(),
                        BaseResponse.success(UserInfoResponse.from(user))));
    }

    /**
     * 사용자 인증 후 JWT 토큰을 발급합니다.
     *
     * @param request 로그인 요청 정보 (이메일, 비밀번호)
     * @return 사용자 정보 및 JWT 토큰
     */
    @PostMapping("/users/login")
    public Mono<ResponseEntity<BaseResponse<UserInfoResponse>>> login(
            @Valid @RequestBody LoginRequest request) {
        return userService.login(request.toCommand())
                .map(user -> jwtTokenIssuer.issueTokens(
                        user.getId(),
                        BaseResponse.success(UserInfoResponse.from(user))));
    }

    /**
     * Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급합니다.
     *
     * @param request HTTP 요청 (쿠키에서 Refresh Token 추출)
     * @return 새로운 JWT 토큰
     */
    @PostMapping("/users/refresh")
    public Mono<ResponseEntity<BaseResponse<Void>>> refresh(
            ServerHttpRequest request) {
        return Mono.fromCallable(() -> extractRefreshTokenFromCookie(request))
                .flatMap(userService::getUserIdFromRefreshToken)
                .map(userId -> jwtTokenIssuer.issueTokens(userId,
                        BaseResponse.success(null)));
    }

    /**
     * HTTP 요청의 쿠키에서 Refresh Token을 추출합니다.
     */
    private String extractRefreshTokenFromCookie(ServerHttpRequest request) {
        var refreshTokenCookie = request.getCookies().getFirst("refreshToken");
        if (refreshTokenCookie == null) {
            throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
        }
        return refreshTokenCookie.getValue();
    }
}
