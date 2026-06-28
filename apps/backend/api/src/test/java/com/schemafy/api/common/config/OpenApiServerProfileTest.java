package com.schemafy.api.common.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ActiveProfiles({ "test", "server" })
@SpringBootTest
@AutoConfigureWebTestClient
class OpenApiServerProfileTest {

  @Autowired
  WebTestClient webTestClient;

  @Test
  @DisplayName("server 프로파일에서 OpenAPI 문서와 Swagger UI는 비활성화된다")
  void openApiDocsDisabledInServerProfile() throws IOException {
    Path specPath = Path.of("build/api-spec/openapi3.json");
    Files.createDirectories(specPath.getParent());
    Files.writeString(specPath, """
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
        .uri("/v3/api-docs")
        .exchange()
        .expectStatus().isNotFound();

    webTestClient.get()
        .uri("/openapi/openapi3.json")
        .exchange()
        .expectStatus().isNotFound();

    webTestClient.get()
        .uri("/swagger-ui.html")
        .exchange()
        .expectStatus().isNotFound();

    webTestClient.get()
        .uri("/swagger-ui-hmac.html")
        .exchange()
        .expectStatus().isNotFound();

    webTestClient.get()
        .uri("/swagger-ui-hmac/swagger-ui-hmac.js")
        .exchange()
        .expectStatus().isNotFound();
  }

}
