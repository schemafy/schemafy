package com.schemafy.core.erd.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.security.WithMockCustomUser;
import com.schemafy.domain.erd.vendor.application.port.in.GetDbVendorQuery;
import com.schemafy.domain.erd.vendor.application.port.in.GetDbVendorUseCase;
import com.schemafy.domain.erd.vendor.application.port.in.ListDbVendorsUseCase;
import com.schemafy.domain.erd.vendor.domain.DbVendor;
import com.schemafy.domain.erd.vendor.domain.DbVendorSummary;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureWebTestClient
@AutoConfigureRestDocs
@DisplayName("DbVendorController 통합 테스트")
@WithMockCustomUser(roles = "VIEWER")
class DbVendorControllerTest {

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
    var summary = new DbVendorSummary("MySQL 8.0", "mysql", "8.0");

    given(listDbVendorsUseCase.listDbVendors())
        .willReturn(Flux.just(summary));

    webTestClient.get()
        .uri(API_BASE_PATH + "/vendors")
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
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
                fieldWithPath("[].displayName").description("벤더 표시 이름"),
                fieldWithPath("[].name").description("벤더 이름"),
                fieldWithPath("[].version").description("벤더 버전"))));
  }

  @Test
  @DisplayName("벤더 상세 조회 API 문서화")
  void getVendor() {
    var vendor = new DbVendor(
        "MySQL 8.0", "mysql", "8.0",
        "{\"schemaVersion\":1,\"vendor\":\"mysql\",\"types\":[]}");

    given(getDbVendorUseCase.getDbVendor(any(GetDbVendorQuery.class)))
        .willReturn(Mono.just(vendor));

    webTestClient.get()
        .uri(API_BASE_PATH + "/vendors/{displayName}", "MySQL 8.0")
        .header("Accept", "application/json")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.displayName").isEqualTo("MySQL 8.0")
        .jsonPath("$.name").isEqualTo("mysql")
        .jsonPath("$.version").isEqualTo("8.0")
        .jsonPath("$.datatypeMappings.schemaVersion").isEqualTo(1)
        .consumeWith(document("vendor-get",
            pathParameters(
                parameterWithName("displayName")
                    .description("벤더 표시 이름")),
            requestHeaders(
                headerWithName("Accept")
                    .description("응답 포맷 (application/json)")),
            responseHeaders(
                headerWithName("Content-Type")
                    .description("응답 컨텐츠 타입")),
            responseFields(
                fieldWithPath("displayName").description("벤더 표시 이름"),
                fieldWithPath("name").description("벤더 이름"),
                fieldWithPath("version").description("벤더 버전"),
                fieldWithPath("datatypeMappings").description("데이터타입 매핑 정보"),
                fieldWithPath("datatypeMappings.schemaVersion")
                    .description("스키마 버전"),
                fieldWithPath("datatypeMappings.vendor")
                    .description("벤더 식별자"),
                fieldWithPath("datatypeMappings.types")
                    .description("데이터타입 목록"))));
  }

}
