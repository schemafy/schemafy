package com.schemafy.core.ulid.controller;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.schemafy.core.TestSecurityConfig;
import com.schemafy.core.ulid.service.UlidService;

import reactor.core.publisher.Mono;

@WebFluxTest(controllers = UlidController.class)
@AutoConfigureRestDocs
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class UlidControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UlidService ulidService;

    @Test
    void generateTemporaryUlid() {
        // Given
        String mockUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        when(ulidService.generateTemporaryUlid()).thenReturn(Mono.just(mockUlid));

        // When & Then
        webTestClient
                .get()
                .uri("/api/ulid/generate")
                .header("Accept", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.ulid").isEqualTo(mockUlid)
                .consumeWith(document("ulid-generate",
                        requestHeaders(
                                headerWithName("Accept")
                                        .description("요청 응답 포맷(Accept 헤더)")),
                        responseHeaders(
                                headerWithName("Content-Type")
                                        .description("응답 컨텐츠 타입")),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부")
                                        .type("boolean"),
                                fieldWithPath("result").description("응답 데이터")
                                        .type("object"),
                                fieldWithPath("result.ulid").description("생성된 ULID 문자열")
                                        .type("string"))));
    }
}
