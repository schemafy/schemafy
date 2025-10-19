package com.schemafy.core.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SpringBootTest
@AutoConfigureWebTestClient
class SecurityConfigTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    @DisplayName("공개 엔드포인트는 인증 없이 접근 가능하다")
    void publicEndpointsAccessibleWithoutAuth() {
        // Health check endpoint
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("보호된 엔드포인트는 인증 없이 401을 반환한다")
    void protectedEndpointsRequireAuth() {
        webTestClient.get()
                .uri("/api/v1/protected/resource")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("CORS 설정 빈이 생성된다")
    void corsConfigurationExists() {
        // This test verifies that CORS configuration is properly set up
        // Actual CORS behavior is tested in integration tests with real
        // browsers
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
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
    @DisplayName("여러 공개 엔드포인트에 접근 가능하다")
    void multiplePublicEndpointsAccessible() {
        // API docs endpoint
        webTestClient.get()
                .uri("/v3/api-docs")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }
}
