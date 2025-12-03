package com.schemafy.core.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.principal.AuthenticatedUser;
import com.schemafy.core.common.type.BaseResponse;
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

    @GetMapping("/users")
    public Mono<ResponseEntity<BaseResponse<UserInfoResponse>>> getMyInfo(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return userService.getUserById(user.userId())
                .map(BaseResponse::success)
                .map(ResponseEntity::ok);
    }

    /**
     * 사용자 정보를 조회합니다. 인증된 사용자는 타 사용자의 프로필도 조회할 수 있습니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자 정보
     */
    @GetMapping("/users/{userId}")
    public Mono<ResponseEntity<BaseResponse<UserInfoResponse>>> getUser(
            @PathVariable String userId) {
        return userService.getUserById(userId).map(BaseResponse::success)
                .map(ResponseEntity::ok);
    }

}
