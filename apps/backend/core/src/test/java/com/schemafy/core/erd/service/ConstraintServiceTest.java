package com.schemafy.core.erd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.core.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.core.erd.repository.ConstraintColumnRepository;
import com.schemafy.core.erd.repository.ConstraintRepository;
import com.schemafy.core.erd.repository.entity.Constraint;
import com.schemafy.core.erd.repository.entity.ConstraintColumn;
import com.schemafy.core.validation.client.ValidationClient;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import validation.Validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
    @DisplayName("createConstraint: PK 제약조건 생성 시 하위 테이블로 전파가 올바르게 반영된다")
    void createConstraint_propagation_success() {
        // given
        // 부모 테이블과 자식 테이블, 식별 관계가 이미 존재하는 상황
        Validation.CreateConstraintRequest request = Validation.CreateConstraintRequest
                .newBuilder()
                .setConstraint(Validation.Constraint.newBuilder()
                        .setId("fe-constraint-id")
                        .setTableId("parent-table")
                        .setName("pk_parent")
                        .setKind(Validation.ConstraintKind.PRIMARY_KEY)
                        .addColumns(Validation.ConstraintColumn.newBuilder()
                                .setId("fe-constraint-col-1")
                                .setConstraintId("fe-constraint-id")
                                .setColumnId("parent-id-col")
                                .setSeqNo(1)
                                .build())
                        .build())
                .setDatabase(Validation.Database.newBuilder()
                        .setId("proj-1")
                        .addSchemas(Validation.Schema.newBuilder()
                                .setId("schema-1")
                                .setName("test-schema")
                                .addTables(Validation.Table.newBuilder()
                                        .setId("parent-table")
                                        .setName("parent")
                                        .addColumns(
                                                Validation.Column.newBuilder()
                                                        .setId("parent-id-col")
                                                        .setName("id")
                                                        .setDataType("INT")
                                                        .build())
                                        .build())
                                .addTables(Validation.Table.newBuilder()
                                        .setId("child-table")
                                        .setName("child")
                                        .addColumns(
                                                Validation.Column.newBuilder()
                                                        .setId("child-id-col")
                                                        .setName("id")
                                                        .setDataType("INT")
                                                        .build())
                                        .addConstraints(Validation.Constraint
                                                .newBuilder()
                                                .setId("child-pk-constraint")
                                                .setTableId("child-table")
                                                .setName("pk_child")
                                                .setKind(
                                                        Validation.ConstraintKind.PRIMARY_KEY)
                                                .addColumns(
                                                        Validation.ConstraintColumn
                                                                .newBuilder()
                                                                .setId("child-constraint-col-1")
                                                                .setConstraintId(
                                                                        "child-pk-constraint")
                                                                .setColumnId(
                                                                        "child-id-col")
                                                                .setSeqNo(1)
                                                                .build())
                                                .build())
                                        .addRelationships(
                                                Validation.Relationship
                                                        .newBuilder()
                                                        .setId("identifying-rel")
                                                        .setSrcTableId(
                                                                "child-table")
                                                        .setTgtTableId(
                                                                "parent-table")
                                                        .setName(
                                                                "fk_child_parent")
                                                        .setKind(
                                                                Validation.RelationshipKind.IDENTIFYING)
                                                        .build())
                                        .build())
                                .build())
                        .build())
                .build();

        // ValidationClient 응답: PK 제약조건 생성 후 전파된 컬럼과 제약조건 컬럼이 포함됨
        Validation.Database mockResponse = Validation.Database.newBuilder()
                .setId("proj-1")
                .setIsAffected(true)
                .addSchemas(Validation.Schema.newBuilder()
                        .setId("schema-1")
                        .setName("test-schema")
                        .setIsAffected(true)
                        .addTables(Validation.Table.newBuilder()
                                .setId("parent-table")
                                .setName("parent")
                                .setIsAffected(true)
                                .addColumns(Validation.Column.newBuilder()
                                        .setId("parent-id-col")
                                        .setName("id")
                                        .setDataType("INT")
                                        .setIsAffected(true)
                                        .build())
                                .addConstraints(Validation.Constraint
                                        .newBuilder()
                                        .setId("be-constraint-id")
                                        .setTableId("parent-table")
                                        .setName("pk_parent")
                                        .setKind(
                                                Validation.ConstraintKind.PRIMARY_KEY)
                                        .setIsAffected(true)
                                        .addColumns(Validation.ConstraintColumn
                                                .newBuilder()
                                                .setId("be-constraint-col-1")
                                                .setConstraintId(
                                                        "be-constraint-id")
                                                .setColumnId("parent-id-col")
                                                .setSeqNo(1)
                                                .setIsAffected(true)
                                                .build())
                                        .build())
                                .build())
                        // 전파된 엔티티들: 자식 테이블에 부모 PK 컬럼이 전파됨
                        .addTables(Validation.Table.newBuilder()
                                .setId("child-table")
                                .setName("child")
                                .setIsAffected(true)
                                .addColumns(Validation.Column.newBuilder()
                                        .setId("child-id-col")
                                        .setName("id")
                                        .setDataType("INT")
                                        .setIsAffected(false)
                                        .build())
                                // 전파된 컬럼: 부모 테이블의 PK 컬럼이 자식 테이블로 전파
                                .addColumns(Validation.Column.newBuilder()
                                        .setId("propagated-parent-id-col")
                                        .setName("parent_id")
                                        .setDataType("INT")
                                        .setIsAffected(true) // 전파된 컬럼
                                        .build())
                                .addConstraints(Validation.Constraint
                                        .newBuilder()
                                        .setId("child-pk-constraint")
                                        .setTableId("child-table")
                                        .setName("pk_child")
                                        .setKind(
                                                Validation.ConstraintKind.PRIMARY_KEY)
                                        .setIsAffected(true)
                                        .addColumns(Validation.ConstraintColumn
                                                .newBuilder()
                                                .setId("child-constraint-col-1")
                                                .setConstraintId(
                                                        "child-pk-constraint")
                                                .setColumnId("child-id-col")
                                                .setSeqNo(1)
                                                .setIsAffected(false)
                                                .build())
                                        // 전파된 제약조건 컬럼: 전파된 컬럼이 자식 PK에 추가됨
                                        .addColumns(Validation.ConstraintColumn
                                                .newBuilder()
                                                .setId("propagated-constraint-col")
                                                .setConstraintId(
                                                        "child-pk-constraint")
                                                .setColumnId(
                                                        "propagated-parent-id-col")
                                                .setSeqNo(2)
                                                .setIsAffected(true) // 전파된 제약조건 컬럼
                                                .build())
                                        .build())
                                .addRelationships(Validation.Relationship
                                        .newBuilder()
                                        .setId("identifying-rel")
                                        .setSrcTableId("child-table")
                                        .setTgtTableId("parent-table")
                                        .setName("fk_child_parent")
                                        .setKind(
                                                Validation.RelationshipKind.IDENTIFYING)
                                        .setIsAffected(false)
                                        .build())
                                .build())
                        .build())
                .build();

        // 전파된 엔티티 정보 모킹
        AffectedMappingResponse.PropagatedEntities propagatedEntities = new AffectedMappingResponse.PropagatedEntities(
                List.of(
                        // 전파된 컬럼
                        new AffectedMappingResponse.PropagatedColumn(
                                "propagated-parent-id-col",
                                "child-table",
                                "CONSTRAINT",
                                "be-constraint-id",
                                "parent-id-col" // 원본 컬럼 ID
                        )),
                List.of(
                        // 전파된 제약조건 컬럼
                        new AffectedMappingResponse.PropagatedConstraintColumn(
                                "propagated-constraint-col",
                                "child-pk-constraint",
                                "propagated-parent-id-col",
                                "CONSTRAINT",
                                "be-constraint-id")),
                List.of() // 전파된 인덱스 컬럼 없음
        );

        given(validationClient.createConstraint(
                any(Validation.CreateConstraintRequest.class)))
                .willReturn(Mono.just(mockResponse));
        given(affectedEntitiesSaver.saveAffectedEntities(any(), any(), any(),
                any(), any()))
                .willReturn(Mono.just(propagatedEntities));

        // when
        Mono<AffectedMappingResponse> result = constraintService
                .createConstraint(request);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    // 원본 제약조건 매핑 확인
                    assertThat(response.constraints()).hasSize(1);
                    assertThat(response.constraints().get("parent-table"))
                            .containsEntry("fe-constraint-id",
                                    "be-constraint-id");

                    // 전파된 엔티티 정보 확인
                    assertThat(response.propagated()).isNotNull();
                    assertThat(response.propagated().columns()).hasSize(1);
                    AffectedMappingResponse.PropagatedColumn propagatedColumn = response
                            .propagated().columns().get(0);
                    assertThat(propagatedColumn.columnId())
                            .isEqualTo("propagated-parent-id-col");
                    assertThat(propagatedColumn.tableId())
                            .isEqualTo("child-table");
                    assertThat(propagatedColumn.sourceType())
                            .isEqualTo("CONSTRAINT");
                    assertThat(propagatedColumn.sourceId())
                            .isEqualTo("be-constraint-id");
                    assertThat(propagatedColumn.sourceColumnId())
                            .isEqualTo("parent-id-col");

                    assertThat(response.propagated().constraintColumns())
                            .hasSize(1);
                    AffectedMappingResponse.PropagatedConstraintColumn propagatedConstraintColumn = response
                            .propagated().constraintColumns().get(0);
                    assertThat(propagatedConstraintColumn.constraintColumnId())
                            .isEqualTo("propagated-constraint-col");
                    assertThat(propagatedConstraintColumn.constraintId())
                            .isEqualTo("child-pk-constraint");
                    assertThat(propagatedConstraintColumn.columnId())
                            .isEqualTo("propagated-parent-id-col");
                    assertThat(propagatedConstraintColumn.sourceType())
                            .isEqualTo("CONSTRAINT");
                    assertThat(propagatedConstraintColumn.sourceId())
                            .isEqualTo("be-constraint-id");

                    // 전파된 인덱스 컬럼은 없음
                    assertThat(response.propagated().indexColumns())
                            .isEmpty();
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

        Mono<ConstraintResponse> result = constraintService
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
                    assertThat(list.stream().map(ConstraintResponse::getName)
                            .toList())
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
        Validation.AddColumnToConstraintRequest request = Validation.AddColumnToConstraintRequest
                .newBuilder()
                .setConstraintColumn(Validation.ConstraintColumn.newBuilder()
                        .setId("constraint-column-1")
                        .setConstraintId("constraint-1")
                        .setColumnId("column-1")
                        .setSeqNo(1)
                        .build())
                .build();

        Mono<ConstraintColumnResponse> result = constraintService
                .addColumnToConstraint(request);

        StepVerifier.create(result)
                .assertNext(constraintColumn -> {
                    assertThat(constraintColumn.getId()).isNotNull(); // 자동 생성된 ID
                    assertThat(constraintColumn.getConstraintId())
                            .isEqualTo("constraint-1");
                    assertThat(constraintColumn.getColumnId())
                            .isEqualTo("column-1");
                    assertThat(constraintColumn.getSeqNo()).isEqualTo(1);
                })
                .verifyComplete();

        // DB 반영 확인 - 자동 생성된 ID 사용
        StepVerifier
                .create(constraintColumnRepository.findAll().collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(1);
                    ConstraintColumn found = list.get(0);
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
