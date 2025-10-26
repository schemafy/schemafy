package com.schemafy.core.erd.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.repository.SchemaRepository;
import com.schemafy.core.erd.repository.entity.Schema;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("SchemaService 테스트")
class SchemaServiceTest {

    @Autowired
    SchemaService schemaService;

    @Autowired
    SchemaRepository schemaRepository;

    @BeforeEach
    void setUp() {
        schemaRepository.deleteAll().block();
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
    @DisplayName("changeSchemaName: 존재하면 이름을 변경한다")
    void changeSchemaName_success() {
        Schema saved = schemaRepository.save(
                Schema.builder()
                        .projectId("proj-1")
                        .dbVendorId("mysql")
                        .name("old")
                        .build())
                .block();

        StepVerifier
                .create(schemaService.changeSchemaName(saved.getId(), "new"))
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
    @DisplayName("changeSchemaName: 존재하지 않으면 에러를 반환한다")
    void changeSchemaName_notFound() {
        StepVerifier
                .create(schemaService.changeSchemaName("non-existent", "new"))
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

        StepVerifier.create(schemaService.deleteSchema(saved.getId()))
                .verifyComplete();

        // 삭제 플래그 확인 (deletedAt not null)
        StepVerifier.create(schemaRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.isDeleted()).isTrue())
                .verifyComplete();
    }

}
