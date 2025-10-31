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
import com.schemafy.core.erd.repository.ConstraintColumnRepository;
import com.schemafy.core.erd.repository.ConstraintRepository;
import com.schemafy.core.erd.repository.entity.Constraint;
import com.schemafy.core.erd.repository.entity.ConstraintColumn;
import com.schemafy.core.validation.client.ValidationClient;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import validation.Validation;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("ConstraintService 테스트")
class ConstraintServiceTest {

    @Autowired
    ConstraintService constraintService;

    @Autowired
    ConstraintRepository constraintRepository;

    @Autowired
    ConstraintColumnRepository constraintColumnRepository;

    @MockitoBean
    ValidationClient validationClient;

    @MockitoBean
    AffectedEntitiesSaver affectedEntitiesSaver;

    @BeforeEach
    void setUp() {
        constraintColumnRepository.deleteAll().block();
        constraintRepository.deleteAll().block();

        given(validationClient.changeConstraintName(
                any(Validation.ChangeConstraintNameRequest.class)))
                        .willReturn(Mono.just(
                                Validation.Database.newBuilder().build()));
        given(validationClient.addColumnToConstraint(
                any(Validation.AddColumnToConstraintRequest.class)))
                        .willReturn(Mono.just(
                                Validation.Database.newBuilder().build()));
        given(validationClient.removeColumnFromConstraint(
                any(Validation.RemoveColumnFromConstraintRequest.class)))
                        .willReturn(Mono.just(
                                Validation.Database.newBuilder().build()));
        given(validationClient.deleteConstraint(
                any(Validation.DeleteConstraintRequest.class)))
                        .willReturn(Mono.just(
                                Validation.Database.newBuilder().build()));
    }

    @Test
    @DisplayName("createConstraint: 제약조건 생성 시 매핑 정보가 올바르게 반환된다")
    void createConstraint_mappingResponse_success() {
        // given
        Validation.CreateConstraintRequest request = Validation.CreateConstraintRequest
                .newBuilder()
                .setConstraint(Validation.Constraint.newBuilder()
                        .setId("fe-constraint-id")
                        .setTableId("table-1")
                        .setName("pk_test")
                        .setKind(Validation.ConstraintKind.PRIMARY_KEY)
                        .build())
                .setDatabase(Validation.Database.newBuilder()
                        .setId("proj-1")
                        .addSchemas(Validation.Schema.newBuilder()
                                .setId("schema-1")
                                .setName("test-schema")
                                .addTables(Validation.Table.newBuilder()
                                        .setId("table-1")
                                        .setName("test-table")
                                        .build())
                                .build())
                        .build())
                .build();

        // ValidationClient 모킹
        Validation.Database mockResponse = Validation.Database.newBuilder()
                .setId("proj-1")
                .setIsAffected(true)
                .addSchemas(Validation.Schema.newBuilder()
                        .setId("schema-1")
                        .setName("test-schema")
                        .addTables(Validation.Table.newBuilder()
                                .setId("table-1")
                                .setName("test-table")
                                .addConstraints(Validation.Constraint
                                        .newBuilder()
                                        .setId("be-constraint-id")
                                        .setTableId("table-1")
                                        .setName("pk_test")
                                        .setKind(
                                                Validation.ConstraintKind.PRIMARY_KEY)
                                        .setIsAffected(true)
                                        .build())
                                .build())
                        .build())
                .build();

        given(validationClient.createConstraint(
                any(Validation.CreateConstraintRequest.class)))
                        .willReturn(Mono.just(mockResponse));
        given(affectedEntitiesSaver.saveAffectedEntities(any(), any(), any(),
                any(), any()))
                        .willReturn(Mono
                                .just(AffectedMappingResponse.PropagatedEntities
                                        .empty()));

        // when
        Mono<AffectedMappingResponse> result = constraintService
                .createConstraint(request);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    // 제약조건 매핑 정보 확인 (FE-ID → BE-ID 매핑)
                    // constraints는 tableId로 그룹핑된 nested map
                    assertThat(response.constraints()).hasSize(1);
                    assertThat(response.constraints().get("table-1"))
                            .containsEntry("fe-constraint-id",
                                    "be-constraint-id");

                    // 다른 매핑들은 비어있어야 함 (제약조건만 생성했으므로)
                    assertThat(response.schemas()).isEmpty();
                    assertThat(response.tables()).isEmpty();
                    assertThat(response.columns()).isEmpty();
                    assertThat(response.indexes()).isEmpty();
                    assertThat(response.indexColumns()).isEmpty();
                    assertThat(response.constraintColumns()).isEmpty();
                    assertThat(response.relationships()).isEmpty();
                    assertThat(response.relationshipColumns()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getConstraint: 저장된 제약조건을 조회한다")
    void getConstraint_success() {
        Constraint saved = constraintRepository.save(
                Constraint.builder()
                        .tableId("table-1")
                        .name("pk_test")
                        .kind("PRIMARY_KEY")
                        .build())
                .block();

        Mono<Constraint> result = constraintService
                .getConstraint(saved.getId());

        StepVerifier.create(result)
                .assertNext(found -> {
                    assertThat(found.getId()).isEqualTo(saved.getId());
                    assertThat(found.getName()).isEqualTo("pk_test");
                    assertThat(found.getKind()).isEqualTo("PRIMARY_KEY");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getConstraintsByTableId: 테이블 기준으로 제약조건 목록을 조회한다")
    void getConstraintsByTableId_success() {
        Constraint c1 = Constraint.builder().tableId("table-A").name("pk_a")
                .kind("PRIMARY_KEY").build();
        Constraint c2 = Constraint.builder().tableId("table-A").name("uk_a")
                .kind("UNIQUE").build();
        Constraint cOther = Constraint.builder().tableId("table-B").name("pk_b")
                .kind("PRIMARY_KEY").build();

        constraintRepository.save(c1)
                .then(constraintRepository.save(c2))
                .then(constraintRepository.save(cOther))
                .block();

        StepVerifier
                .create(constraintService.getConstraintsByTableId("table-A")
                        .collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.stream().map(Constraint::getName).toList())
                            .containsExactlyInAnyOrder("pk_a", "uk_a");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateConstraintName: 존재하면 이름을 변경한다")
    void updateConstraintName_success() {
        Constraint saved = constraintRepository.save(
                Constraint.builder()
                        .tableId("table-1")
                        .name("old_name")
                        .kind("PRIMARY_KEY")
                        .build())
                .block();

        StepVerifier.create(constraintService.updateConstraintName(
                Validation.ChangeConstraintNameRequest.newBuilder()
                        .setConstraintId(saved.getId())
                        .setNewName("new_name")
                        .build()))
                .assertNext(updated -> {
                    assertThat(updated.getId()).isEqualTo(saved.getId());
                    assertThat(updated.getName()).isEqualTo("new_name");
                })
                .verifyComplete();

        // DB 반영 확인
        StepVerifier.create(constraintRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.getName())
                        .isEqualTo("new_name"))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateConstraintName: 존재하지 않으면 에러를 반환한다")
    void updateConstraintName_notFound() {
        StepVerifier.create(constraintService.updateConstraintName(
                Validation.ChangeConstraintNameRequest.newBuilder()
                        .setConstraintId("non-existent")
                        .setNewName("new_name")
                        .build()))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.ERD_CONSTRAINT_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("addColumnToConstraint: 제약조건에 컬럼을 추가한다")
    void addColumnToConstraint_success() {
        // given
        Validation.AddColumnToConstraintRequest request = Validation.AddColumnToConstraintRequest
                .newBuilder()
                .setConstraintColumn(Validation.ConstraintColumn.newBuilder()
                        .setId("constraint-column-1")
                        .setConstraintId("constraint-1")
                        .setColumnId("column-1")
                        .setSeqNo(1)
                        .build())
                .build();

        // when
        Mono<ConstraintColumn> result = constraintService
                .addColumnToConstraint(request);

        // then
        StepVerifier.create(result)
                .assertNext(constraintColumn -> {
                    assertThat(constraintColumn.getId())
                            .isEqualTo("constraint-column-1");
                    assertThat(constraintColumn.getConstraintId())
                            .isEqualTo("constraint-1");
                    assertThat(constraintColumn.getColumnId())
                            .isEqualTo("column-1");
                    assertThat(constraintColumn.getSeqNo()).isEqualTo(1);
                })
                .verifyComplete();

        // DB 반영 확인
        StepVerifier
                .create(constraintColumnRepository
                        .findById("constraint-column-1"))
                .assertNext(found -> {
                    assertThat(found.getConstraintId())
                            .isEqualTo("constraint-1");
                    assertThat(found.getColumnId()).isEqualTo("column-1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("removeColumnFromConstraint: 제약조건에서 컬럼을 제거한다 (소프트 삭제)")
    void removeColumnFromConstraint_success() {
        ConstraintColumn saved = constraintColumnRepository.save(
                ConstraintColumn.builder()
                        .constraintId("constraint-1")
                        .columnId("column-1")
                        .seqNo(1)
                        .build())
                .block();

        StepVerifier.create(constraintService.removeColumnFromConstraint(
                Validation.RemoveColumnFromConstraintRequest.newBuilder()
                        .setConstraintColumnId(saved.getId())
                        .build()))
                .verifyComplete();

        // 삭제 플래그 확인 (deletedAt not null)
        StepVerifier.create(constraintColumnRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.isDeleted()).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteConstraint: 소프트 삭제가 수행된다")
    void deleteConstraint_softDelete() {
        Constraint saved = constraintRepository.save(
                Constraint.builder()
                        .tableId("table-1")
                        .name("to_delete")
                        .kind("PRIMARY_KEY")
                        .build())
                .block();

        StepVerifier.create(constraintService.deleteConstraint(
                Validation.DeleteConstraintRequest.newBuilder()
                        .setConstraintId(saved.getId())
                        .build()))
                .verifyComplete();

        // 삭제 플래그 확인 (deletedAt not null)
        StepVerifier.create(constraintRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.isDeleted()).isTrue())
                .verifyComplete();
    }

}
