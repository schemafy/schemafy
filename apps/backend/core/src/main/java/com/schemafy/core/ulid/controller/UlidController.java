package com.schemafy.core.ulid.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.annotation.ApiVersion;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.ulid.controller.dto.UlidResponse;
import com.schemafy.core.ulid.service.UlidService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(ApiPath.PUBLIC_API)
@RequiredArgsConstructor
public class UlidController {

  private final UlidService ulidService;

  @GetMapping("/ulid/generate")
  public Mono<ResponseEntity<BaseResponse<UlidResponse>>> generateTemporaryUlid(
      @ApiVersion String version) {
    return ulidService
        .generateTemporaryUlid()
        .map(UlidResponse::new)
        .map(BaseResponse::success)
        .map(ResponseEntity::ok);
  }

}
