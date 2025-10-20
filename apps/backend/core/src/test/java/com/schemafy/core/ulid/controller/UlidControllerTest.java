package com.schemafy.core.ulid.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.Test;

import com.schemafy.core.TestSecurityConfig;
import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.jwt.JwtProvider;
import com.schemafy.core.common.security.jwt.WebExchangeErrorWriter;
import com.schemafy.core.ulid.service.UlidService;

import reactor.core.publisher.Mono;

import static com.schemafy.core.ulid.docs.UlidApiSnippets.*;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@WebFluxTest(controllers = UlidController.class)
@AutoConfigureRestDocs
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class UlidControllerTest {
    private static final String API_BASE_PATH = ApiPath.AUTH_API.replace(
            "{version}",
            "v1.0");

    @MockitoBean
    WebExchangeErrorWriter errorResponseWriter;

    @MockitoBean
    JwtProvider jwtProvider;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UlidService ulidService;

    @Test
    void generateTemporaryUlid() {
        // Given
        String mockUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        when(ulidService.generateTemporaryUlid())
                .thenReturn(Mono.just(mockUlid));

        // When & Then
        webTestClient
                .get()
                .uri(API_BASE_PATH + "/ulid/generate")
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.ulid").isEqualTo(mockUlid)
                .consumeWith(document("ulid-generate",
                        generateUlidRequestHeaders(),
                        generateUlidResponseHeaders(),
                        generateUlidResponse()));
    }
}
