package com.schemafy.core.user.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.constant.ApiPath;
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

    @PostMapping("/users/signup")
    public Mono<BaseResponse<UserInfoResponse>> signUp(
            @Valid @RequestBody SignUpRequest request) {
        return userService.signUp(request.toCommand())
                .map(BaseResponse::success);
    }

    @PostMapping("/users/login")
    public Mono<BaseResponse<UserInfoResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return userService.login(request.toCommand())
                .map(BaseResponse::success);
    }

    @GetMapping("/users/{userId}")
    public Mono<BaseResponse<UserInfoResponse>> getUser(
            @PathVariable String userId) {
        return userService.getUserById(userId)
                .map(BaseResponse::success);
    }
}
