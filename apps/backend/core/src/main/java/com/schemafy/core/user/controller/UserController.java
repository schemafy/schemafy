package com.schemafy.core.user.controller;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.user.controller.dto.response.UserInfoResponse;
import com.schemafy.core.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(ApiPath.AUTH_API)
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * 인증된 사용자 본인의 정보만 조회 가능합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자 정보
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("authentication.name == #userId")
    public Mono<ResponseEntity<BaseResponse<UserInfoResponse>>> getUser(
            @PathVariable String userId) {
        return userService.getUserById(userId).map(BaseResponse::success)
                .map(ResponseEntity::ok);
    }
}
