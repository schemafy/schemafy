package com.schemafy.api.common.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dev")
@SpringBootTest(properties = "schemafy.openapi.resource-locations=file:build/test-openapi-spec/security/")
@AutoConfigureWebTestClient
@Import(TestSecurityConfig.class)
class SecurityConfigTest {

  private static final Path TEST_SPEC_PATH = Path.of(
      "build/test-openapi-spec/security/openapi3.json");

  @Autowired
  WebTestClient webTestClient;

  @Test
  @DisplayName("보호된 엔드포인트는 인증 없이 401을 반환한다")
  void protectedEndpointsRequireAuth() {
    webTestClient.get()
        .uri("/api/v1/protected/resource")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("ERD 엔드포인트는 인증 없이 401을 반환한다")
  void erdEndpointsRequireAuth() {
    webTestClient.get()
        .uri("/api/v1/schemas/schema-id")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("유효하지 않은 JWT 토큰은 거부된다")
  void invalidJwtTokenRejected() {
    webTestClient.get()
        .uri("/api/v1/protected/resource")
        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("잘못된 형식의 Authorization 헤더는 거부된다")
  void malformedAuthHeaderRejected() {
    webTestClient.get()
        .uri("/api/v1/protected/resource")
        .header(HttpHeaders.AUTHORIZATION, "InvalidFormat token")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("보호된 엔드포인트에서 Authorization 헤더가 없는 요청은 거부된다")
  void missingAuthHeaderRejected() {
    webTestClient.get()
        .uri("/api/v1/protected/resource")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  @DisplayName("dev 프로파일에서 REST Docs 기반 OpenAPI 정적 문서는 인증 없이 조회할 수 있다")
  void generatedOpenApiDocsEndpointAllowedInDev() throws IOException {
    writeTestOpenApiSpec("""
        {
          "openapi": "3.0.1",
          "info": {
            "title": "Schemafy API",
            "version": "v1.0"
          },
          "paths": {
            "/public/api/v1.0/users/login": {},
            "/api/v1.0/users": {}
          },
          "components": {
            "securitySchemes": {
              "bearerAuth": {
                "type": "http",
                "scheme": "bearer"
              }
            }
          }
        }
        """);

    webTestClient.get()
        .uri("/openapi/openapi3.json")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.info.title").isEqualTo("Schemafy API")
        .jsonPath("$.components.securitySchemes.bearerAuth.scheme")
        .isEqualTo("bearer")
        .jsonPath("$.paths['/public/api/v1.0/users/login']").exists()
        .jsonPath("$.paths['/api/v1.0/users']").exists();
  }

  @Test
  @DisplayName("OpenAPI 정적 문서는 낡은 Bearer 토큰이 있어도 조회할 수 있다")
  void generatedOpenApiDocsEndpointAllowedWithStaleBearerToken()
      throws IOException {
    writeTestOpenApiSpec("""
        {
          "openapi": "3.0.1",
          "info": {
            "title": "Schemafy API",
            "version": "v1.0"
          },
          "paths": {}
        }
        """);

    webTestClient.get()
        .uri("/openapi/openapi3.json")
        .header(HttpHeaders.AUTHORIZATION, "Bearer stale-token")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.info.title").isEqualTo("Schemafy API");
  }

  private static void writeTestOpenApiSpec(String spec) throws IOException {
    Files.createDirectories(TEST_SPEC_PATH.getParent());
    Files.writeString(TEST_SPEC_PATH, spec);
  }

  @Test
  @DisplayName("dev 프로파일에서 Swagger UI는 인증 없이 접근할 수 있다")
  void swaggerUiEndpointAllowedInDev() {
    webTestClient.get()
        .uri("/swagger-ui.html")
        .exchange()
        .expectStatus()
        .value(status -> assertThat(status).isIn(200, 301, 302, 303, 307, 308));
  }

  @Test
  @DisplayName("dev 프로파일에서 HMAC 개발용 Swagger UI는 인증 없이 접근할 수 있다")
  void swaggerUiHmacEndpointAllowedInDev() {
    webTestClient.get()
        .uri("/swagger-ui-hmac.html")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
  }

}
