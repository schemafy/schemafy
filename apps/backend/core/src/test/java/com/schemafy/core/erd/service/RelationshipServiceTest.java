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
import com.schemafy.core.erd.controller.dto.request.CreateRelationshipRequestWithExtra;
import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.repository.RelationshipColumnRepository;
import com.schemafy.core.erd.repository.RelationshipRepository;
import com.schemafy.core.erd.repository.entity.Relationship;
import com.schemafy.core.erd.repository.entity.RelationshipColumn;
import com.schemafy.core.validation.client.ValidationClient;

import java.util.List;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import validation.Validation;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("RelationshipService 테스트")
class RelationshipServiceTest {

    @Autowired
    RelationshipService relationshipService;

    @Autowired
    RelationshipRepository relationshipRepository;

    @Autowired
    RelationshipColumnRepository relationshipColumnRepository;

    @MockitoBean
    ValidationClient validationClient;

    @MockitoBean
    AffectedEntitiesSaver affectedEntitiesSaver;

    @BeforeEach
    void setUp() {
        relationshipColumnRepository.deleteAll().block();
        relationshipRepository.deleteAll().block();

        given(validationClient.changeRelationshipName(
                any(Validation.ChangeRelationshipNameRequest.class)))
                        .willReturn(Mono.just(
                                Validation.Database.newBuilder().build()));
        given(validationClient.changeRelationshipCardinality(
                any(Validation.ChangeRelationshipCardinalityRequest.class)))
                        .willReturn(Mono.just(
                                Validation.Database.newBuilder().build()));
        given(validationClient.addColumnToRelationship(
                any(Validation.AddColumnToRelationshipRequest.class)))
                        .willReturn(Mono.just(
                                Validation.Database.newBuilder().build()));
        given(validationClient.removeColumnFromRelationship(
                any(Validation.RemoveColumnFromRelationshipRequest.class)))
                        .willReturn(Mono.just(
                                Validation.Database.newBuilder().build()));
        given(validationClient.deleteRelationship(
                any(Validation.DeleteRelationshipRequest.class)))
                        .willReturn(Mono.just(
                                Validation.Database.newBuilder().build()));
    }

    @Test
    @DisplayName("createRelationship: 관계 생성 시 매핑 정보가 올바르게 반환된다")
    void createRelationship_mappingResponse_success() {
        // given
        Validation.CreateRelationshipRequest request = Validation.CreateRelationshipRequest
                .newBuilder()
                .setRelationship(Validation.Relationship.newBuilder()
                        .setId("fe-relationship-id")
                        .setSrcTableId("table-1")
                        .setTgtTableId("table-2")
                        .setName("fk_test")
                        .setKind(Validation.RelationshipKind.NON_IDENTIFYING)
                        .setCardinality(
                                Validation.RelationshipCardinality.ONE_TO_MANY)
                        .build())
                .setDatabase(Validation.Database.newBuilder()
                        .setId("proj-1")
                        .addSchemas(Validation.Schema.newBuilder()
                                .setId("schema-1")
                                .setName("test-schema")
                                .addTables(Validation.Table.newBuilder()
                                        .setId("table-1")
                                        .setName("parent-table")
                                        .build())
                                .addTables(Validation.Table.newBuilder()
                                        .setId("table-2")
                                        .setName("child-table")
                                        .build())
                                .build())
                        .build())
                .build();

        CreateRelationshipRequestWithExtra requestWithExtra = new CreateRelationshipRequestWithExtra(
                request, "extra-data");

        // ValidationClient 모킹
        Validation.Database mockResponse = Validation.Database.newBuilder()
                .setId("proj-1")
                .setIsAffected(true)
                .addSchemas(Validation.Schema.newBuilder()
                        .setId("schema-1")
                        .setName("test-schema")
                        .setIsAffected(true)
                        .addTables(Validation.Table.newBuilder()
                                .setId("table-1")
                                .setName("parent-table")
                                .setIsAffected(true)
                                .addRelationships(Validation.Relationship
                                        .newBuilder()
                                        .setId("be-relationship-id")
                                        .setSrcTableId("table-1")
                                        .setTgtTableId("table-2")
                                        .setName("fk_test")
                                        .setKind(
                                                Validation.RelationshipKind.NON_IDENTIFYING)
                                        .setCardinality(
                                                Validation.RelationshipCardinality.ONE_TO_MANY)
                                        .setIsAffected(true)
                                        .build())
                                .build())
                        .addTables(Validation.Table.newBuilder()
                                .setId("table-2")
                                .setName("child-table")
                                .build())
                        .build())
                .build();

        given(validationClient.createRelationship(
                any(Validation.CreateRelationshipRequest.class)))
                        .willReturn(Mono.just(mockResponse));
        given(affectedEntitiesSaver.saveAffectedEntities(any(), any(), any(),
                any(), any()))
                        .willReturn(Mono
                                .just(AffectedMappingResponse.PropagatedEntities
                                        .empty()));

        // when
        Mono<AffectedMappingResponse> result = relationshipService
                .createRelationship(requestWithExtra);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    // 관계 매핑 정보 확인 (FE-ID → BE-ID 매핑)
                    // relationships는 tableId로 그룹핑된 nested map
                    assertThat(response.relationships()).hasSize(1);
                    assertThat(response.relationships().get("table-1"))
                            .containsEntry("fe-relationship-id",
                                    "be-relationship-id");

                    // 다른 매핑들은 비어있어야 함 (관계만 생성했으므로)
                    assertThat(response.schemas()).isEmpty();
                    assertThat(response.tables()).isEmpty();
                    assertThat(response.columns()).isEmpty();
                    assertThat(response.indexes()).isEmpty();
                    assertThat(response.indexColumns()).isEmpty();
                    assertThat(response.constraints()).isEmpty();
                    assertThat(response.constraintColumns()).isEmpty();
                    assertThat(response.relationshipColumns()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createRelationship: 식별 관계 생성 시 하위 테이블로 전파가 올바르게 반영된다")
    void createRelationship_propagation_success() {
        // given
        // 부모 테이블에 PK 제약조건이 있고, 자식 테이블에 식별 관계를 생성하는 상황
        Validation.CreateRelationshipRequest request = Validation.CreateRelationshipRequest
                .newBuilder()
                .setRelationship(Validation.Relationship.newBuilder()
                        .setId("fe-relationship-id")
                        .setSrcTableId("child-table")
                        .setTgtTableId("parent-table")
                        .setName("fk_child_parent")
                        .setKind(Validation.RelationshipKind.IDENTIFYING)
                        .setCardinality(
                                Validation.RelationshipCardinality.ONE_TO_MANY)
                        .addColumns(Validation.RelationshipColumn.newBuilder()
                                .setId("fe-relationship-col-1")
                                .setRelationshipId("fe-relationship-id")
                                .setFkColumnId("fe-fk-column-id") // 자식 테이블에 생성될 FK 컬럼 ID
                                .setRefColumnId("parent-id-col") // 부모 테이블의 PK 컬럼 ID
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
                                        .addColumns(Validation.Column.newBuilder()
                                                .setId("parent-id-col")
                                                .setName("id")
                                                .setDataType("INT")
                                                .build())
                                        .addConstraints(Validation.Constraint.newBuilder()
                                                .setId("parent-pk-constraint")
                                                .setTableId("parent-table")
                                                .setName("pk_parent")
                                                .setKind(Validation.ConstraintKind.PRIMARY_KEY)
                                                .addColumns(Validation.ConstraintColumn.newBuilder()
                                                        .setId("parent-constraint-col-1")
                                                        .setConstraintId("parent-pk-constraint")
                                                        .setColumnId("parent-id-col")
                                                        .setSeqNo(1)
                                                        .build())
                                                .build())
                                        .build())
                                .addTables(Validation.Table.newBuilder()
                                        .setId("child-table")
                                        .setName("child")
                                        .addColumns(Validation.Column.newBuilder()
                                                .setId("child-id-col")
                                                .setName("id")
                                                .setDataType("INT")
                                                .build())
                                        .addConstraints(Validation.Constraint.newBuilder()
                                                .setId("child-pk-constraint")
                                                .setTableId("child-table")
                                                .setName("pk_child")
                                                .setKind(Validation.ConstraintKind.PRIMARY_KEY)
                                                .addColumns(Validation.ConstraintColumn.newBuilder()
                                                        .setId("child-constraint-col-1")
                                                        .setConstraintId("child-pk-constraint")
                                                        .setColumnId("child-id-col")
                                                        .setSeqNo(1)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        CreateRelationshipRequestWithExtra requestWithExtra = new CreateRelationshipRequestWithExtra(
                request, "extra-data");

        // ValidationClient 응답: 식별 관계 생성 후 전파된 컬럼과 제약조건 컬럼이 포함됨
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
                                .setIsAffected(false)
                                .addColumns(Validation.Column.newBuilder()
                                        .setId("parent-id-col")
                                        .setName("id")
                                        .setDataType("INT")
                                        .setIsAffected(false)
                                        .build())
                                .addConstraints(Validation.Constraint.newBuilder()
                                        .setId("parent-pk-constraint")
                                        .setTableId("parent-table")
                                        .setName("pk_parent")
                                        .setKind(Validation.ConstraintKind.PRIMARY_KEY)
                                        .setIsAffected(false)
                                        .addColumns(Validation.ConstraintColumn.newBuilder()
                                                .setId("parent-constraint-col-1")
                                                .setConstraintId("parent-pk-constraint")
                                                .setColumnId("parent-id-col")
                                                .setSeqNo(1)
                                                .setIsAffected(false)
                                                .build())
                                        .build())
                                .build())
                        // 전파된 엔티티들: 자식 테이블에 FK 컬럼이 생성되고 PK 제약조건에 추가됨
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
                                        .setId("be-fk-column-id")
                                        .setName("parent_id")
                                        .setDataType("INT")
                                        .setIsAffected(true) // 전파된 컬럼
                                        .build())
                                .addConstraints(Validation.Constraint.newBuilder()
                                        .setId("child-pk-constraint")
                                        .setTableId("child-table")
                                        .setName("pk_child")
                                        .setKind(Validation.ConstraintKind.PRIMARY_KEY)
                                        .setIsAffected(true)
                                        .addColumns(Validation.ConstraintColumn.newBuilder()
                                                .setId("child-constraint-col-1")
                                                .setConstraintId("child-pk-constraint")
                                                .setColumnId("child-id-col")
                                                .setSeqNo(1)
                                                .setIsAffected(false)
                                                .build())
                                        // 전파된 제약조건 컬럼: 전파된 FK 컬럼이 자식 PK에 추가됨
                                        .addColumns(Validation.ConstraintColumn.newBuilder()
                                                .setId("propagated-constraint-col")
                                                .setConstraintId("child-pk-constraint")
                                                .setColumnId("be-fk-column-id")
                                                .setSeqNo(2)
                                                .setIsAffected(true) // 전파된 제약조건 컬럼
                                                .build())
                                        .build())
                                .addRelationships(Validation.Relationship.newBuilder()
                                        .setId("be-relationship-id")
                                        .setSrcTableId("child-table")
                                        .setTgtTableId("parent-table")
                                        .setName("fk_child_parent")
                                        .setKind(Validation.RelationshipKind.IDENTIFYING)
                                        .setCardinality(
                                                Validation.RelationshipCardinality.ONE_TO_MANY)
                                        .setIsAffected(true)
                                        .addColumns(Validation.RelationshipColumn.newBuilder()
                                                .setId("be-relationship-col-1")
                                                .setRelationshipId("be-relationship-id")
                                                .setFkColumnId("be-fk-column-id")
                                                .setRefColumnId("parent-id-col")
                                                .setSeqNo(1)
                                                .setIsAffected(true)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        // 전파된 엔티티 정보 모킹
        AffectedMappingResponse.PropagatedEntities propagatedEntities =
                new AffectedMappingResponse.PropagatedEntities(
                        List.of(
                                // 전파된 컬럼
                                new AffectedMappingResponse.PropagatedColumn(
                                        "be-fk-column-id",
                                        "child-table",
                                        "RELATIONSHIP",
                                        "be-relationship-id",
                                        "parent-id-col" // 원본 컬럼 ID
                                )),
                        List.of(
                                // 전파된 제약조건 컬럼
                                new AffectedMappingResponse.PropagatedConstraintColumn(
                                        "propagated-constraint-col",
                                        "child-pk-constraint",
                                        "be-fk-column-id",
                                        "RELATIONSHIP",
                                        "be-relationship-id"
                                )),
                        List.of() // 전파된 인덱스 컬럼 없음
                );

        given(validationClient.createRelationship(
                any(Validation.CreateRelationshipRequest.class)))
                .willReturn(Mono.just(mockResponse));
        given(affectedEntitiesSaver.saveAffectedEntities(any(), any(), any(),
                any(), any()))
                .willReturn(Mono.just(propagatedEntities));

        // when
        Mono<AffectedMappingResponse> result = relationshipService
                .createRelationship(requestWithExtra);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    // 원본 관계 매핑 확인
                    assertThat(response.relationships()).hasSize(1);
                    assertThat(response.relationships().get("child-table"))
                            .containsEntry("fe-relationship-id",
                                    "be-relationship-id");

                    // 전파된 엔티티 정보 확인
                    assertThat(response.propagated()).isNotNull();
                    assertThat(response.propagated().columns()).hasSize(1);
                    AffectedMappingResponse.PropagatedColumn propagatedColumn =
                            response.propagated().columns().get(0);
                    assertThat(propagatedColumn.columnId())
                            .isEqualTo("be-fk-column-id");
                    assertThat(propagatedColumn.tableId())
                            .isEqualTo("child-table");
                    assertThat(propagatedColumn.sourceType())
                            .isEqualTo("RELATIONSHIP");
                    assertThat(propagatedColumn.sourceId())
                            .isEqualTo("be-relationship-id");
                    assertThat(propagatedColumn.sourceColumnId())
                            .isEqualTo("parent-id-col");

                    assertThat(response.propagated().constraintColumns())
                            .hasSize(1);
                    AffectedMappingResponse.PropagatedConstraintColumn propagatedConstraintColumn =
                            response.propagated().constraintColumns().get(0);
                    assertThat(propagatedConstraintColumn.constraintColumnId())
                            .isEqualTo("propagated-constraint-col");
                    assertThat(propagatedConstraintColumn.constraintId())
                            .isEqualTo("child-pk-constraint");
                    assertThat(propagatedConstraintColumn.columnId())
                            .isEqualTo("be-fk-column-id");
                    assertThat(propagatedConstraintColumn.sourceType())
                            .isEqualTo("RELATIONSHIP");
                    assertThat(propagatedConstraintColumn.sourceId())
                            .isEqualTo("be-relationship-id");

                    // 전파된 인덱스 컬럼은 없음
                    assertThat(response.propagated().indexColumns())
                            .isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getRelationship: 저장된 관계를 조회한다")
    void getRelationship_success() {
        Relationship saved = relationshipRepository.save(
                Relationship.builder()
                        .srcTableId("table-1")
                        .tgtTableId("table-2")
                        .name("fk_test")
                        .kind("NON_IDENTIFYING")
                        .cardinality("ONE_TO_MANY")
                        .onDelete("NO_ACTION")
                        .onUpdate("NO_ACTION")
                        .extra("extra-data")
                        .build())
                .block();

        Mono<Relationship> result = relationshipService
                .getRelationship(saved.getId());

        StepVerifier.create(result)
                .assertNext(found -> {
                    assertThat(found.getId()).isEqualTo(saved.getId());
                    assertThat(found.getName()).isEqualTo("fk_test");
                    assertThat(found.getKind()).isEqualTo("NON_IDENTIFYING");
                    assertThat(found.getCardinality()).isEqualTo("ONE_TO_MANY");
                    assertThat(found.getExtra()).isEqualTo("extra-data");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getRelationshipsByTableId: 테이블 기준으로 관계 목록을 조회한다")
    void getRelationshipsByTableId_success() {
        Relationship r1 = Relationship.builder()
                .srcTableId("table-A").tgtTableId("table-B")
                .name("fk_a_b").kind("NON_IDENTIFYING")
                .cardinality("ONE_TO_MANY").onDelete("NO_ACTION")
                .onUpdate("NO_ACTION").extra("").build();
        Relationship r2 = Relationship.builder()
                .srcTableId("table-A").tgtTableId("table-C")
                .name("fk_a_c").kind("IDENTIFYING")
                .cardinality("ONE_TO_ONE").onDelete("NO_ACTION")
                .onUpdate("NO_ACTION").extra("").build();
        Relationship rOther = Relationship.builder()
                .srcTableId("table-B").tgtTableId("table-C")
                .name("fk_b_c").kind("NON_IDENTIFYING")
                .cardinality("ONE_TO_MANY").onDelete("NO_ACTION")
                .onUpdate("NO_ACTION").extra("").build();

        relationshipRepository.save(r1)
                .then(relationshipRepository.save(r2))
                .then(relationshipRepository.save(rOther))
                .block();

        StepVerifier
                .create(relationshipService.getRelationshipsByTableId("table-A")
                        .collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(
                            list.stream().map(Relationship::getName).toList())
                                    .containsExactlyInAnyOrder("fk_a_b",
                                            "fk_a_c");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateRelationshipName: 존재하면 이름을 변경한다")
    void updateRelationshipName_success() {
        Relationship saved = relationshipRepository.save(
                Relationship.builder()
                        .srcTableId("table-1")
                        .tgtTableId("table-2")
                        .name("old_name")
                        .kind("NON_IDENTIFYING")
                        .cardinality("ONE_TO_MANY")
                        .onDelete("NO_ACTION")
                        .onUpdate("NO_ACTION")
                        .extra("")
                        .build())
                .block();

        StepVerifier.create(relationshipService.updateRelationshipName(
                Validation.ChangeRelationshipNameRequest.newBuilder()
                        .setRelationshipId(saved.getId())
                        .setNewName("new_name")
                        .build()))
                .assertNext(updated -> {
                    assertThat(updated.getId()).isEqualTo(saved.getId());
                    assertThat(updated.getName()).isEqualTo("new_name");
                })
                .verifyComplete();

        // DB 반영 확인
        StepVerifier.create(relationshipRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.getName())
                        .isEqualTo("new_name"))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateRelationshipName: 존재하지 않으면 에러를 반환한다")
    void updateRelationshipName_notFound() {
        StepVerifier.create(relationshipService.updateRelationshipName(
                Validation.ChangeRelationshipNameRequest.newBuilder()
                        .setRelationshipId("non-existent")
                        .setNewName("new_name")
                        .build()))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.ERD_RELATIONSHIP_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("updateRelationshipCardinality: 카디널리티를 변경한다")
    void updateRelationshipCardinality_success() {
        Relationship saved = relationshipRepository.save(
                Relationship.builder()
                        .srcTableId("table-1")
                        .tgtTableId("table-2")
                        .name("fk_test")
                        .kind("NON_IDENTIFYING")
                        .cardinality("ONE_TO_MANY")
                        .onDelete("NO_ACTION")
                        .onUpdate("NO_ACTION")
                        .extra("")
                        .build())
                .block();

        StepVerifier.create(relationshipService.updateRelationshipCardinality(
                Validation.ChangeRelationshipCardinalityRequest.newBuilder()
                        .setRelationshipId(saved.getId())
                        .setCardinality(
                                Validation.RelationshipCardinality.ONE_TO_ONE)
                        .build()))
                .assertNext(updated -> {
                    assertThat(updated.getId()).isEqualTo(saved.getId());
                    assertThat(updated.getCardinality())
                            .isEqualTo("ONE_TO_ONE");
                })
                .verifyComplete();

        // DB 반영 확인
        StepVerifier.create(relationshipRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.getCardinality())
                        .isEqualTo("ONE_TO_ONE"))
                .verifyComplete();
    }

    @Test
    @DisplayName("addColumnToRelationship: 관계에 컬럼을 추가한다")
    void addColumnToRelationship_success() {
        // given
        Validation.AddColumnToRelationshipRequest request = Validation.AddColumnToRelationshipRequest
                .newBuilder()
                .setRelationshipColumn(
                        Validation.RelationshipColumn.newBuilder()
                                .setId("relationship-column-1")
                                .setRelationshipId("relationship-1")
                                .setFkColumnId("src-column-1")
                                .setRefColumnId("tgt-column-1")
                                .setSeqNo(1)
                                .build())
                .build();

        // when
        Mono<RelationshipColumn> result = relationshipService
                .addColumnToRelationship(request);

        // then
        StepVerifier.create(result)
                .assertNext(relationshipColumn -> {
                    assertThat(relationshipColumn.getId()).isNotNull(); // 자동 생성된 ID
                    assertThat(relationshipColumn.getRelationshipId())
                            .isEqualTo("relationship-1");
                    assertThat(relationshipColumn.getSrcColumnId())
                            .isEqualTo("src-column-1");
                    assertThat(relationshipColumn.getTgtColumnId())
                            .isEqualTo("tgt-column-1");
                    assertThat(relationshipColumn.getSeqNo()).isEqualTo(1);
                })
                .verifyComplete();

        // DB 반영 확인 - 자동 생성된 ID 사용
        StepVerifier
                .create(relationshipColumnRepository.findAll().collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(1);
                    RelationshipColumn found = list.get(0);
                    assertThat(found.getRelationshipId())
                            .isEqualTo("relationship-1");
                    assertThat(found.getSrcColumnId())
                            .isEqualTo("src-column-1");
                    assertThat(found.getTgtColumnId())
                            .isEqualTo("tgt-column-1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("removeColumnFromRelationship: 관계에서 컬럼을 제거한다 (소프트 삭제)")
    void removeColumnFromRelationship_success() {
        RelationshipColumn saved = relationshipColumnRepository.save(
                RelationshipColumn.builder()
                        .relationshipId("relationship-1")
                        .srcColumnId("src-column-1")
                        .tgtColumnId("tgt-column-1")
                        .seqNo(1)
                        .build())
                .block();

        StepVerifier.create(relationshipService.removeColumnFromRelationship(
                Validation.RemoveColumnFromRelationshipRequest.newBuilder()
                        .setRelationshipColumnId(saved.getId())
                        .build()))
                .verifyComplete();

        // 삭제 플래그 확인 (deletedAt not null)
        StepVerifier
                .create(relationshipColumnRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.isDeleted()).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteRelationship: 소프트 삭제가 수행된다")
    void deleteRelationship_softDelete() {
        Relationship saved = relationshipRepository.save(
                Relationship.builder()
                        .srcTableId("table-1")
                        .tgtTableId("table-2")
                        .name("to_delete")
                        .kind("NON_IDENTIFYING")
                        .cardinality("ONE_TO_MANY")
                        .onDelete("NO_ACTION")
                        .onUpdate("NO_ACTION")
                        .extra("")
                        .build())
                .block();

        StepVerifier.create(relationshipService.deleteRelationship(
                Validation.DeleteRelationshipRequest.newBuilder()
                        .setRelationshipId(saved.getId())
                        .build()))
                .verifyComplete();

        // 삭제 플래그 확인 (deletedAt not null)
        StepVerifier.create(relationshipRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.isDeleted()).isTrue())
                .verifyComplete();
    }

}
