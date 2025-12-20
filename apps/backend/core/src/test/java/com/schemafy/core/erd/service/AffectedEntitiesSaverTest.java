package com.schemafy.core.erd.service;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.repository.ColumnRepository;
import com.schemafy.core.erd.repository.ConstraintColumnRepository;
import com.schemafy.core.erd.repository.ConstraintRepository;
import com.schemafy.core.erd.repository.IndexColumnRepository;
import com.schemafy.core.erd.repository.IndexRepository;
import com.schemafy.core.erd.repository.RelationshipColumnRepository;
import com.schemafy.core.erd.repository.RelationshipRepository;
import com.schemafy.core.erd.repository.SchemaRepository;
import com.schemafy.core.erd.repository.TableRepository;
import com.schemafy.core.erd.repository.entity.Column;
import com.schemafy.core.erd.repository.entity.RelationshipColumn;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;
import validation.Validation;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("AffectedEntitiesSaver 테스트")
class AffectedEntitiesSaverTest {

    @Autowired
    AffectedEntitiesSaver affectedEntitiesSaver;

    @Autowired
    RelationshipColumnRepository relationshipColumnRepository;

    @Autowired
    RelationshipRepository relationshipRepository;

    @Autowired
    ConstraintColumnRepository constraintColumnRepository;

    @Autowired
    ConstraintRepository constraintRepository;

    @Autowired
    IndexColumnRepository indexColumnRepository;

    @Autowired
    IndexRepository indexRepository;

    @Autowired
    ColumnRepository columnRepository;

    @Autowired
    TableRepository tableRepository;

    @Autowired
    SchemaRepository schemaRepository;

    @BeforeEach
    void setUp() {
        relationshipColumnRepository.deleteAll().block();
        relationshipRepository.deleteAll().block();
        constraintColumnRepository.deleteAll().block();
        constraintRepository.deleteAll().block();
        indexColumnRepository.deleteAll().block();
        indexRepository.deleteAll().block();
        columnRepository.deleteAll().block();
        tableRepository.deleteAll().block();
        schemaRepository.deleteAll().block();
    }

    @Test
    @DisplayName("RELATIONSHIP 전파 컬럼의 sourceColumnId는 relationshipColumn.refColumnId를 따른다")
    void saveAffectedEntities_relationshipSource_setsSourceColumnId() {
        String schemaId = "schema-1";
        String parentTableId = "parent-table";
        String childTableId = "child-table";
        String parentColumnId = "parent-id-col";
        String relationshipId = "be-relationship-id";
        String fkColumnLogicalId = "fk-col-logical";
        String relationshipColumnLogicalId = "relcol-logical";

        Validation.Database before = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .addTables(Validation.Table.newBuilder()
                                .setId(parentTableId)
                                .setName("parent")
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(parentColumnId)
                                        .setTableId(parentTableId)
                                        .setName("id")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .build())
                                .build())
                        .addTables(Validation.Table.newBuilder()
                                .setId(childTableId)
                                .setName("child")
                                .build())
                        .build())
                .build();

        Validation.Database after = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .addTables(Validation.Table.newBuilder()
                                .setId(parentTableId)
                                .setName("parent")
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(parentColumnId)
                                        .setTableId(parentTableId)
                                        .setName("id")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .build())
                                .build())
                        .addTables(Validation.Table.newBuilder()
                                .setId(childTableId)
                                .setName("child")
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(fkColumnLogicalId)
                                        .setTableId(childTableId)
                                        .setName("parent_id")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .setIsAffected(true)
                                        .build())
                                .addRelationships(Validation.Relationship
                                        .newBuilder()
                                        .setId(relationshipId)
                                        .setSrcTableId(childTableId)
                                        .setTgtTableId(parentTableId)
                                        .setName("fk_child_parent")
                                        .setKind(
                                                Validation.RelationshipKind.IDENTIFYING)
                                        .setCardinality(
                                                Validation.RelationshipCardinality.ONE_TO_MANY)
                                        .setIsAffected(true)
                                        .addColumns(
                                                Validation.RelationshipColumn
                                                        .newBuilder()
                                                        .setId(relationshipColumnLogicalId)
                                                        .setRelationshipId(
                                                                relationshipId)
                                                        .setFkColumnId(
                                                                fkColumnLogicalId)
                                                        .setRefColumnId(
                                                                parentColumnId)
                                                        .setSeqNo(1)
                                                        .setIsAffected(true)
                                                        .build())
                                        .build())
                                .build())
                        .build())
                .build();

        Mono<AffectedMappingResponse.PropagatedEntities> result = affectedEntitiesSaver
                .saveAffectedEntities(
                        before,
                        after,
                        relationshipId,
                        relationshipId,
                        "RELATIONSHIP",
                        Set.of());

        StepVerifier.create(result)
                .assertNext(propagated -> {
                    assertThat(propagated.columns()).hasSize(1);
                    AffectedMappingResponse.PropagatedColumn propagatedColumn = propagated
                            .columns().get(0);

                    assertThat(propagatedColumn.tableId())
                            .isEqualTo(childTableId);
                    assertThat(propagatedColumn.sourceType())
                            .isEqualTo("RELATIONSHIP");
                    assertThat(propagatedColumn.sourceId())
                            .isEqualTo(relationshipId);
                    assertThat(propagatedColumn.sourceColumnId())
                            .isEqualTo(parentColumnId);

                    assertThat(propagated.relationshipColumns()).hasSize(1);
                    AffectedMappingResponse.PropagatedRelationshipColumn propagatedRelationshipColumn = propagated
                            .relationshipColumns().get(0);
                    assertThat(propagatedRelationshipColumn.relationshipId())
                            .isEqualTo(relationshipId);
                    assertThat(propagatedRelationshipColumn.fkColumnId())
                            .isEqualTo(propagatedColumn.columnId());
                    assertThat(propagatedRelationshipColumn.refColumnId())
                            .isEqualTo(parentColumnId);
                    assertThat(propagatedRelationshipColumn.seqNo())
                            .isEqualTo(1);
                    assertThat(propagatedRelationshipColumn.sourceType())
                            .isEqualTo("RELATIONSHIP");
                    assertThat(propagatedRelationshipColumn.sourceId())
                            .isEqualTo(relationshipId);

                    assertThat(propagated.constraintColumns()).isEmpty();
                    assertThat(propagated.indexColumns()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("저장된 relationshipColumn.srcColumnId는 실제 컬럼을 참조한다")
    void saveAffectedEntities_relationshipColumnSrcColumnId_referencesSavedColumn() {
        String schemaId = "schema-1";
        String parentTableId = "parent-table";
        String childTableId = "child-table";
        String parentColumnId = "parent-id-col";
        String relationshipId = "be-relationship-id";
        String fkColumnLogicalId = "fk-col-logical";
        String relationshipColumnLogicalId = "relcol-logical";

        Validation.Database before = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .addTables(Validation.Table.newBuilder()
                                .setId(parentTableId)
                                .setName("parent")
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(parentColumnId)
                                        .setTableId(parentTableId)
                                        .setName("id")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .build())
                                .build())
                        .addTables(Validation.Table.newBuilder()
                                .setId(childTableId)
                                .setName("child")
                                .build())
                        .build())
                .build();

        Validation.Database after = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .addTables(Validation.Table.newBuilder()
                                .setId(parentTableId)
                                .setName("parent")
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(parentColumnId)
                                        .setTableId(parentTableId)
                                        .setName("id")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .build())
                                .build())
                        .addTables(Validation.Table.newBuilder()
                                .setId(childTableId)
                                .setName("child")
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(fkColumnLogicalId)
                                        .setTableId(childTableId)
                                        .setName("parent_id")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .setIsAffected(true)
                                        .build())
                                .addRelationships(Validation.Relationship
                                        .newBuilder()
                                        .setId(relationshipId)
                                        .setSrcTableId(childTableId)
                                        .setTgtTableId(parentTableId)
                                        .setName("fk_child_parent")
                                        .setKind(
                                                Validation.RelationshipKind.IDENTIFYING)
                                        .setCardinality(
                                                Validation.RelationshipCardinality.ONE_TO_MANY)
                                        .setIsAffected(true)
                                        .addColumns(
                                                Validation.RelationshipColumn
                                                        .newBuilder()
                                                        .setId(relationshipColumnLogicalId)
                                                        .setRelationshipId(
                                                                relationshipId)
                                                        .setFkColumnId(
                                                                fkColumnLogicalId)
                                                        .setRefColumnId(
                                                                parentColumnId)
                                                        .setSeqNo(1)
                                                        .setIsAffected(true)
                                                        .build())
                                        .build())
                                .build())
                        .build())
                .build();

        Mono<Tuple3<AffectedMappingResponse.PropagatedEntities, RelationshipColumn, Column>> result = affectedEntitiesSaver
                .saveAffectedEntities(
                        before,
                        after,
                        relationshipId,
                        relationshipId,
                        "RELATIONSHIP",
                        Set.of())
                .flatMap(propagated -> {
                    assertThat(propagated.relationshipColumns()).hasSize(1);
                    AffectedMappingResponse.PropagatedRelationshipColumn relationshipColumn = propagated
                            .relationshipColumns().get(0);

                    return relationshipColumnRepository
                            .findByIdAndDeletedAtIsNull(
                                    relationshipColumn.relationshipColumnId())
                            .flatMap(savedRelationshipColumn -> columnRepository
                                    .findByIdAndDeletedAtIsNull(
                                            savedRelationshipColumn
                                                    .getSrcColumnId())
                                    .map(savedSrcColumn -> Tuples.of(
                                            propagated,
                                            savedRelationshipColumn,
                                            savedSrcColumn)));
                });

        StepVerifier.create(result)
                .assertNext(tuple -> {
                    AffectedMappingResponse.PropagatedEntities propagated = tuple
                            .getT1();
                    RelationshipColumn savedRelationshipColumn = tuple.getT2();
                    Column savedSrcColumn = tuple.getT3();

                    assertThat(propagated.relationshipColumns()).hasSize(1);
                    AffectedMappingResponse.PropagatedRelationshipColumn relationshipColumn = propagated
                            .relationshipColumns().get(0);

                    assertThat(savedRelationshipColumn.getSrcColumnId())
                            .isEqualTo(relationshipColumn.fkColumnId());
                    assertThat(savedSrcColumn.getId())
                            .isEqualTo(savedRelationshipColumn.getSrcColumnId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("CONSTRAINT 전파 컬럼의 sourceColumnId는 부모 PK 컬럼을 따른다")
    void saveAffectedEntities_constraintSource_setsSourceColumnId() {
        String schemaId = "schema-1";
        String parentTableId = "parent-table";
        String childTableId = "child-table";
        String parentColumnId = "parent-id-col";
        String constraintId = "be-constraint-id";
        String constraintColumnId = "fe-constraint-col-1";
        String relationshipId = "relationship-1";
        String fkColumnLogicalId = "fk-col-logical";
        String relationshipColumnLogicalId = "relcol-logical";

        Validation.Database before = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .addTables(Validation.Table.newBuilder()
                                .setId(parentTableId)
                                .setName("parent")
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(parentColumnId)
                                        .setTableId(parentTableId)
                                        .setName("id")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .build())
                                .build())
                        .addTables(Validation.Table.newBuilder()
                                .setId(childTableId)
                                .setName("child")
                                .build())
                        .build())
                .build();

        Validation.Database after = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .addTables(Validation.Table.newBuilder()
                                .setId(parentTableId)
                                .setName("parent")
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(parentColumnId)
                                        .setTableId(parentTableId)
                                        .setName("id")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .build())
                                .addConstraints(Validation.Constraint
                                        .newBuilder()
                                        .setId(constraintId)
                                        .setTableId(parentTableId)
                                        .setName("pk_parent")
                                        .setKind(
                                                Validation.ConstraintKind.PRIMARY_KEY)
                                        .setIsAffected(true)
                                        .addColumns(
                                                Validation.ConstraintColumn
                                                        .newBuilder()
                                                        .setId(constraintColumnId)
                                                        .setConstraintId(
                                                                constraintId)
                                                        .setColumnId(
                                                                parentColumnId)
                                                        .setSeqNo(1)
                                                        .setIsAffected(true)
                                                        .build())
                                        .build())
                                .build())
                        .addTables(Validation.Table.newBuilder()
                                .setId(childTableId)
                                .setName("child")
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(fkColumnLogicalId)
                                        .setTableId(childTableId)
                                        .setName("parent_id")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .setIsAffected(true)
                                        .build())
                                .addRelationships(Validation.Relationship
                                        .newBuilder()
                                        .setId(relationshipId)
                                        .setSrcTableId(childTableId)
                                        .setTgtTableId(parentTableId)
                                        .setName("fk_child_parent")
                                        .setKind(
                                                Validation.RelationshipKind.IDENTIFYING)
                                        .setCardinality(
                                                Validation.RelationshipCardinality.ONE_TO_MANY)
                                        .addColumns(
                                                Validation.RelationshipColumn
                                                        .newBuilder()
                                                        .setId(relationshipColumnLogicalId)
                                                        .setRelationshipId(
                                                                relationshipId)
                                                        .setFkColumnId(
                                                                fkColumnLogicalId)
                                                        .setRefColumnId(
                                                                parentColumnId)
                                                        .setSeqNo(1)
                                                        .setIsAffected(true)
                                                        .build())
                                        .build())
                                .build())
                        .build())
                .build();

        Mono<AffectedMappingResponse.PropagatedEntities> result = affectedEntitiesSaver
                .saveAffectedEntities(
                        before,
                        after,
                        constraintId,
                        constraintId,
                        "CONSTRAINT",
                        Set.of(constraintColumnId));

        StepVerifier.create(result)
                .assertNext(propagated -> {
                    assertThat(propagated.columns()).hasSize(1);
                    AffectedMappingResponse.PropagatedColumn propagatedColumn = propagated
                            .columns().get(0);

                    assertThat(propagatedColumn.tableId())
                            .isEqualTo(childTableId);
                    assertThat(propagatedColumn.sourceType())
                            .isEqualTo("CONSTRAINT");
                    assertThat(propagatedColumn.sourceId())
                            .isEqualTo(constraintId);
                    assertThat(propagatedColumn.sourceColumnId())
                            .isEqualTo(parentColumnId);

                    assertThat(propagated.relationshipColumns()).hasSize(1);
                    AffectedMappingResponse.PropagatedRelationshipColumn propagatedRelationshipColumn = propagated
                            .relationshipColumns().get(0);
                    assertThat(propagatedRelationshipColumn.fkColumnId())
                            .isEqualTo(propagatedColumn.columnId());
                    assertThat(propagatedRelationshipColumn.refColumnId())
                            .isEqualTo(parentColumnId);
                    assertThat(propagatedRelationshipColumn.sourceType())
                            .isEqualTo("CONSTRAINT");
                    assertThat(propagatedRelationshipColumn.sourceId())
                            .isEqualTo(constraintId);

                    assertThat(propagated.constraintColumns()).isEmpty();
                    assertThat(propagated.indexColumns()).isEmpty();
                })
                .verifyComplete();
    }

}
