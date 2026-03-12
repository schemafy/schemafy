package com.schemafy.api.ulid.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.Test;

import com.schemafy.api.common.config.TestSecurityConfig;
import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.security.jwt.JwtProvider;
import com.schemafy.api.common.security.jwt.WebExchangeErrorWriter;
import com.schemafy.core.ulid.application.port.in.GenerateUlidUseCase;

import reactor.core.publisher.Mono;

import static com.schemafy.api.ulid.docs.UlidApiSnippets.*;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@WebFluxTest(controllers = UlidController.class)
@AutoConfigureRestDocs
@Import(TestSecurityConfig.class)
class UlidControllerTest {

  private static final String API_BASE_PATH = ApiPath.PUBLIC_API.replace(
      "{version}",
      "v1.0");

  @MockitoBean
  WebExchangeErrorWriter errorResponseWriter;

  @MockitoBean
  JwtProvider jwtProvider;

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private GenerateUlidUseCase generateUlidUseCase;

  @Test
  void generateUlid() {
    String mockUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    when(generateUlidUseCase.generateUlid())
        .thenReturn(Mono.just(mockUlid));

    webTestClient
        .get()
        .uri(API_BASE_PATH + "/ulid/generate")
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.ulid").isEqualTo(mockUlid)
        .consumeWith(document("ulid-generate",
            generateUlidRequestHeaders(),
            generateUlidResponseHeaders(),
            generateUlidResponse()));
  }

}
