package com.schemafy.core.ulid.controller;

import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.ulid.controller.dto.UlidResponse;
import com.schemafy.core.ulid.service.UlidService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ulid")
@RequiredArgsConstructor
public class UlidController {

    private final UlidService ulidService;

    @PostMapping("/generate")
    public Mono<BaseResponse<UlidResponse>> generateTemporaryUlid() {
        return ulidService.generateTemporaryUlid()
                .map(UlidResponse::new)
                .map(BaseResponse::success);
    }
}
