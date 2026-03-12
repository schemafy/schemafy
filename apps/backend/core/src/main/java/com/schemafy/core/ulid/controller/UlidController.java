package com.schemafy.core.ulid.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.annotation.ApiVersion;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.ulid.controller.dto.UlidResponse;
import com.schemafy.domain.ulid.application.port.in.GenerateUlidUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(ApiPath.PUBLIC_API)
@RequiredArgsConstructor
public class UlidController {

  private final GenerateUlidUseCase generateUlidUseCase;

  @GetMapping("/ulid/generate")
  public Mono<ResponseEntity<UlidResponse>> generateUlid(
      @ApiVersion String version) {
    return generateUlidUseCase
        .generateUlid()
        .map(UlidResponse::new)
        .map(ResponseEntity::ok);
  }

}
