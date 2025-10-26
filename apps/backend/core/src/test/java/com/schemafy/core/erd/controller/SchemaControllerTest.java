package com.schemafy.core.erd.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.repository.entity.Schema;
import com.schemafy.core.erd.service.SchemaService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import validation.Validation.ChangeSchemaNameRequest;
import validation.Validation.CreateSchemaRequest;
import validation.Validation.DeleteSchemaRequest;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@DisplayName("SchemaController 통합 테스트")
class SchemaControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private SchemaService schemaService;

    @Test
    @DisplayName("스키마 생성: 성공 시 200과 ID 매핑 반환")
    void createSchema_success() {
        // given
        CreateSchemaRequest request = CreateSchemaRequest.newBuilder()
                .setSchema(validation.Validation.Schema.newBuilder()
                        .setId("fe-id").setName("new-schema").build())
                .build();
        AffectedMappingResponse mockResponse = new AffectedMappingResponse(
                Map.of("fe-id", "be-id"), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap());
        given(schemaService.createSchema(any(CreateSchemaRequest.class)))
                .willReturn(Mono.just(mockResponse));

        // when & then
        webTestClient.post().uri("/schemas")
                .contentType(new MediaType("application", "x-protobuf"))
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.schemas.fe-id").isEqualTo("be-id");
    }

    @Test
    @DisplayName("스키마 단건 조회: 성공 시 200 반환")
    void getSchema_success_returns_ok() {
        String id = "schema-id";
        Schema found = Schema.builder()
                .projectId("proj")
                .dbVendorId("MYSQL")
                .name("orders")
                .charset("utf8mb4")
                .collation("utf8mb4_general_ci")
                .vendorOption("null")
                .build();
        ReflectionTestUtils.setField(found, "id", id);

        given(schemaService.getSchema(id))
                .willReturn(Mono.just(found));

        webTestClient.get().uri("/schemas/{schemaId}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.name").isEqualTo("orders");
    }

    @Test
    @DisplayName("스키마 이름 변경: 성공 시 200 반환")
    void changeSchemaName_success_returns_ok() {
        String id = "schema-id";
        ChangeSchemaNameRequest request = ChangeSchemaNameRequest.newBuilder()
                .setSchemaId(id)
                .setNewName("new-name")
                .build();

        Schema updated = Schema.builder()
                .projectId("proj")
                .dbVendorId("MYSQL")
                .name("new-name")
                .build();
        ReflectionTestUtils.setField(updated, "id", id);

        given(schemaService.changeSchemaName(id, "new-name"))
                .willReturn(Mono.just(updated));

        webTestClient.put().uri("/schemas/{schemaId}/name", id)
                .contentType(new MediaType("application", "x-protobuf"))
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.result.name").isEqualTo("new-name");
    }

    @Test
    @DisplayName("스키마 이름 변경: 경로와 바디의 schemaId 불일치 시 400")
    void changeSchemaName_mismatch_returns_bad_request() {
        String pathId = "path-id";
        ChangeSchemaNameRequest request = ChangeSchemaNameRequest.newBuilder()
                .setSchemaId("body-id")
                .setNewName("new-name")
                .build();

        webTestClient.put().uri("/schemas/{schemaId}/name", pathId)
                .contentType(new MediaType("application", "x-protobuf"))
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    @DisplayName("스키마 삭제: 성공 시 200 반환")
    void deleteSchema_success() {
        // given
        String id = "schema-to-delete";
        DeleteSchemaRequest request = DeleteSchemaRequest.newBuilder()
                .setSchemaId(id)
                .build();
        given(schemaService.deleteSchema(id)).willReturn(Mono.empty());

        // when & then
        webTestClient.method(HttpMethod.DELETE).uri("/schemas/{schemaId}", id)
                .contentType(new MediaType("application", "x-protobuf"))
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    @Test
    @DisplayName("스키마 삭제: 경로와 바디의 schemaId 불일치 시 400")
    void deleteSchema_mismatch_returns_bad_request() {
        String pathId = "path-id";
        DeleteSchemaRequest request = DeleteSchemaRequest.newBuilder()
                .setSchemaId("body-id")
                .build();

        webTestClient.method(HttpMethod.DELETE)
                .uri("/schemas/{schemaId}", pathId)
                .contentType(new MediaType("application", "x-protobuf"))
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

}
