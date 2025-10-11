package com.schemafy.core.ulid.controller;

import com.schemafy.core.common.annotation.ApiVersion;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.ulid.controller.dto.UlidResponse;
import com.schemafy.core.ulid.service.UlidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(ApiPath.AUTH_API)
@RequiredArgsConstructor
public class UlidController {

    private final UlidService ulidService;

    @GetMapping("/ulid/generate")
    public Mono<BaseResponse<UlidResponse>> generateTemporaryUlid(@ApiVersion String version) {
        return ulidService
                .generateTemporaryUlid()
                .map(UlidResponse::new)
                .map(BaseResponse::success);
    }
}
