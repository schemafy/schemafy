package com.schemafy.api.erd.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.security.WithMockCustomUser;
import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.vendor.application.port.in.GetDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.in.GetDbVendorUseCase;
import com.schemafy.core.erd.vendor.application.port.in.ListDbVendorsUseCase;
import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.erd.vendor.domain.DbVendorSummary;
import com.schemafy.core.erd.vendor.domain.VendorCapabilities;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.epages.restdocs.apispec.WebTestClientRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("DbVendorController 통합 테스트")
@WithMockCustomUser
class DbVendorControllerTest {

  private static final Integer DB_VENDOR_ID = 1;
  private static final String API_BASE_PATH = ApiPath.API
      .replace("{version}", "v1.0");

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private ListDbVendorsUseCase listDbVendorsUseCase;

  @MockitoBean
  private GetDbVendorUseCase getDbVendorUseCase;

  @Test
  @DisplayName("벤더 목록 조회 API 문서화")
  void listVendors() {
    var summary = new DbVendorSummary(DB_VENDOR_ID, "MySQL 8.0", "mysql", "8.0");

    given(listDbVendorsUseCase.listDbVendors())
        .willReturn(Flux.just(summary));

    webTestClient.get()
        .uri(API_BASE_PATH + "/vendors")
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[0].id").isEqualTo(DB_VENDOR_ID)
        .jsonPath("$[0].displayName").isEqualTo("MySQL 8.0")
        .jsonPath("$[0].name").isEqualTo("mysql")
        .jsonPath("$[0].version").isEqualTo("8.0")
        .consumeWith(document("vendor-list",
            requestHeaders(
                headerWithName("Accept")
                    .description("응답 포맷 (application/json)")),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("[].id").description("벤더 프로필 ID"),
                fieldWithPath("[].displayName").description("벤더 표시 이름"),
                fieldWithPath("[].name").description("벤더 이름"),
                fieldWithPath("[].version").description("벤더 버전"))));
  }

  @Test
  @DisplayName("벤더 상세 조회 API 문서화")
  void getVendor() {
    var vendor = new DbVendor(
        DB_VENDOR_ID,
        "MySQL 8.0", "mysql", "8.0",
        "{\"schemaVersion\":1,\"vendor\":\"mysql\",\"types\":[]}",
        mysqlCapabilities());

    given(getDbVendorUseCase.getDbVendor(any(GetDbVendorQuery.class)))
        .willReturn(Mono.just(vendor));

    webTestClient.get()
        .uri(API_BASE_PATH + "/vendors/{id}", DB_VENDOR_ID)
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.id").isEqualTo(DB_VENDOR_ID)
        .jsonPath("$.displayName").isEqualTo("MySQL 8.0")
        .jsonPath("$.name").isEqualTo("mysql")
        .jsonPath("$.version").isEqualTo("8.0")
        .jsonPath("$.datatypeMappings.schemaVersion").isEqualTo(1)
        .jsonPath("$.capabilities.schemaVersion").isEqualTo(1)
        .jsonPath("$.capabilities.indexes.supportedTypes").isArray()
        .jsonPath("$.capabilities.indexes.sortDirectionTypes[0]").isEqualTo("BTREE")
        .consumeWith(document("vendor-get",
            pathParameters(
                parameterWithName("id")
                    .description("벤더 프로필 ID")),
            requestHeaders(
                headerWithName("Accept")
                    .description("응답 포맷 (application/json)")),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("id").description("벤더 프로필 ID"),
                fieldWithPath("displayName").description("벤더 표시 이름"),
                fieldWithPath("name").description("벤더 이름"),
                fieldWithPath("version").description("벤더 버전"),
                fieldWithPath("datatypeMappings").description("데이터타입 매핑 정보"),
                fieldWithPath("datatypeMappings.schemaVersion")
                    .description("스키마 버전"),
                fieldWithPath("datatypeMappings.vendor")
                    .description("벤더 식별자"),
                fieldWithPath("datatypeMappings.types")
                    .description("데이터타입 목록"),
                fieldWithPath("capabilities")
                    .description("벤더 기능 정보"),
                fieldWithPath("capabilities.schemaVersion")
                    .description("기능 정보 스키마 버전"),
                fieldWithPath("capabilities.indexes")
                    .description("인덱스 기능 정보"),
                fieldWithPath("capabilities.indexes.supportedTypes")
                    .description("지원 인덱스 타입 목록"),
                fieldWithPath("capabilities.indexes.sortDirectionTypes")
                    .description("정렬 방향이 의미 있는 인덱스 타입 목록"))));
  }

  private static VendorCapabilities mysqlCapabilities() {
    return new VendorCapabilities(
        1,
        new IndexCapabilities(
            Set.of(IndexType.BTREE, IndexType.FULLTEXT, IndexType.SPATIAL),
            Set.of(IndexType.BTREE)));
  }

}
