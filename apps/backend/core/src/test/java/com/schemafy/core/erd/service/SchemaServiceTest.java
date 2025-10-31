package com.schemafy.core.erd.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.repository.SchemaRepository;
import com.schemafy.core.erd.repository.entity.Schema;
import com.schemafy.core.validation.client.ValidationClient;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import validation.Validation;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("SchemaService 테스트")
class SchemaServiceTest {

    @Autowired
    SchemaService schemaService;

    @Autowired
    SchemaRepository schemaRepository;

    @MockitoBean
    ValidationClient validationClient;

    @BeforeEach
    void setUp() {
        schemaRepository.deleteAll().block();
        given(validationClient
                .changeSchemaName(any(Validation.ChangeSchemaNameRequest.class)))
                        .willReturn(Mono.just(validation.Validation.Database
                                .newBuilder().build()));
        given(validationClient.deleteSchema(any(Validation.DeleteSchemaRequest.class)))
                .willReturn(Mono.just(
                        validation.Validation.Database.newBuilder().build()));
    }

    @Test
    @DisplayName("getSchema: 저장된 스키마를 조회한다")
    void getSchema_success() {
        Schema saved = schemaRepository.save(
                Schema.builder()
                        .projectId("proj-1")
                        .dbVendorId("mysql")
                        .name("orders")
                        .build())
                .block();

        Mono<Schema> result = schemaService.getSchema(saved.getId());

        StepVerifier.create(result)
                .assertNext(found -> {
                    assertThat(found.getId()).isEqualTo(saved.getId());
                    assertThat(found.getName()).isEqualTo("orders");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getSchemasByProjectId: 프로젝트 기준으로 목록을 조회한다")
    void getSchemasByProjectId_success() {
        Schema s1 = Schema.builder().projectId("proj-A").dbVendorId("mysql")
                .name("a").build();
        Schema s2 = Schema.builder().projectId("proj-A").dbVendorId("mysql")
                .name("b").build();
        Schema sOther = Schema.builder().projectId("proj-B").dbVendorId("mysql")
                .name("c").build();
        schemaRepository.save(s1).then(schemaRepository.save(s2))
                .then(schemaRepository.save(sOther)).block();

        StepVerifier
                .create(schemaService.getSchemasByProjectId("proj-A")
                        .collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.stream().map(Schema::getName).toList())
                            .containsExactlyInAnyOrder("a", "b");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateSchemaName: 존재하면 이름을 변경한다")
    void updateSchemaName_success() {
        Schema saved = schemaRepository.save(
                Schema.builder()
                        .projectId("proj-1")
                        .dbVendorId("mysql")
                        .name("old")
                        .build())
                .block();

        StepVerifier
                .create(schemaService
                        .updateSchemaName(Validation.ChangeSchemaNameRequest.newBuilder()
                                .setSchemaId(saved.getId())
                                .setNewName("new")
                                .build()))
                .assertNext(updated -> {
                    assertThat(updated.getId()).isEqualTo(saved.getId());
                    assertThat(updated.getName()).isEqualTo("new");
                })
                .verifyComplete();

        // DB 반영 확인
        StepVerifier.create(schemaRepository.findById(saved.getId()))
                .assertNext(
                        found -> assertThat(found.getName()).isEqualTo("new"))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateSchemaName: 존재하지 않으면 에러를 반환한다")
    void updateSchemaName_notFound() {
        StepVerifier
                .create(schemaService
                        .updateSchemaName(Validation.ChangeSchemaNameRequest.newBuilder()
                                .setSchemaId("non-existent")
                                .setNewName("new")
                                .build()))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.ERD_SCHEMA_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("deleteSchema: 소프트 삭제가 수행된다")
    void deleteSchema_softDelete() {
        Schema saved = schemaRepository.save(
                Schema.builder()
                        .projectId("proj-1")
                        .dbVendorId("mysql")
                        .name("to-delete")
                        .build())
                .block();

        StepVerifier
                .create(schemaService
                        .deleteSchema(Validation.DeleteSchemaRequest.newBuilder()
                                .setSchemaId(saved.getId())
                                .build()))
                .verifyComplete();

        // 삭제 플래그 확인 (deletedAt not null)
        StepVerifier.create(schemaRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.isDeleted()).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("createSchema: 스키마 생성 시 매핑 정보가 올바르게 반환된다")
    void createSchema_mappingResponse_success() {
        // given
        Validation.CreateSchemaRequest request = Validation.CreateSchemaRequest.newBuilder()
                .setSchema(validation.Validation.Schema.newBuilder()
                        .setId("fe-schema-id")
                        .setProjectId("proj-1")
                        .setDbVendorId(validation.Validation.DbVendor.MYSQL)
                        .setName("test-schema")
                        .setCharset("utf8mb4")
                        .setCollation("utf8mb4_general_ci")
                        .setVendorOption("ENGINE=InnoDB")
                        .build())
                .setDatabase(validation.Validation.Database.newBuilder()
                        .setId("proj-1")
                        .build())
                .build();

        // ValidationClient 모킹
        validation.Validation.Database mockResponse = validation.Validation.Database
                .newBuilder()
                .setId("proj-1")
                .addSchemas(validation.Validation.Schema.newBuilder()
                        .setId("be-schema-id")
                        .setProjectId("proj-1")
                        .setDbVendorId(validation.Validation.DbVendor.MYSQL)
                        .setName("test-schema")
                        .setCharset("utf8mb4")
                        .setCollation("utf8mb4_general_ci")
                        .setVendorOption("ENGINE=InnoDB")
                        .setIsAffected(true)  // 이 값이 true여야 매핑됨
                        .build())
                .build();

        given(validationClient.createSchema(any(Validation.CreateSchemaRequest.class)))
                .willReturn(Mono.just(mockResponse));

        // when
        Mono<AffectedMappingResponse> result = schemaService
                .createSchema(request);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    // 스키마 매핑 정보 확인 (새로 생성된 엔티티는 FE-ID = BE-ID)
                    assertThat(response.schemas()).hasSize(1);
                    assertThat(response.schemas()).containsEntry("fe-schema-id",
                            "be-schema-id");

                    // 다른 매핑들은 비어있어야 함
                    assertThat(response.tables()).isEmpty();
                    assertThat(response.columns()).isEmpty();
                    assertThat(response.indexes()).isEmpty();
                    assertThat(response.indexColumns()).isEmpty();
                    assertThat(response.constraints()).isEmpty();
                    assertThat(response.constraintColumns()).isEmpty();
                    assertThat(response.relationships()).isEmpty();
                    assertThat(response.relationshipColumns()).isEmpty();
                })
                .verifyComplete();
    }

}
