package com.schemafy.core.erd.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.erd.controller.dto.response.AffectedMappingResponse;
import com.schemafy.core.erd.model.EntityType;
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
import com.schemafy.core.erd.repository.entity.Constraint;
import com.schemafy.core.erd.repository.entity.RelationshipColumn;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
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
                        EntityType.RELATIONSHIP.name(),
                        Set.of());

        StepVerifier.create(result)
                .assertNext(propagated -> {
                    assertThat(propagated.columns()).hasSize(1);
                    AffectedMappingResponse.PropagatedColumn propagatedColumn = propagated
                            .columns().get(0);

                    assertThat(propagatedColumn.tableId())
                            .isEqualTo(childTableId);
                    assertThat(propagatedColumn.sourceType())
                            .isEqualTo(EntityType.RELATIONSHIP.name());
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
                            .isEqualTo(EntityType.RELATIONSHIP.name());
                    assertThat(propagatedRelationshipColumn.sourceId())
                            .isEqualTo(relationshipId);

                    assertThat(propagated.constraintColumns()).isEmpty();
                    assertThat(propagated.indexColumns()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("전파로 새로 생성된 constraint는 propagated.constraints로 반환된다")
    void saveAffectedEntities_propagatedConstraint_includedInResponse() {
        String schemaId = "schema-1";
        String childTableId = "child-table";
        String constraintLogicalId = "pk-constraint-logical";
        String sourceId = "relationship-1";

        Validation.Database before = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
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
                                .setId(childTableId)
                                .setName("child")
                                .addConstraints(Validation.Constraint
                                        .newBuilder()
                                        .setId(constraintLogicalId)
                                        .setTableId(childTableId)
                                        .setName("pk_child")
                                        .setKind(
                                                Validation.ConstraintKind.PRIMARY_KEY)
                                        .setIsAffected(true)
                                        .build())
                                .build())
                        .build())
                .build();

        Mono<Tuple2<AffectedMappingResponse.PropagatedEntities, Constraint>> result = affectedEntitiesSaver
                .saveAffectedEntities(
                        before,
                        after,
                        null,
                        sourceId,
                        EntityType.RELATIONSHIP.name(),
                        Set.of())
                .flatMap(propagated -> {
                    assertThat(propagated.constraints()).hasSize(1);
                    AffectedMappingResponse.PropagatedConstraint propagatedConstraint = propagated
                            .constraints().get(0);

                    return constraintRepository
                            .findByIdAndDeletedAtIsNull(
                                    propagatedConstraint.constraintId())
                            .map(savedConstraint -> Tuples.of(
                                    propagated,
                                    savedConstraint));
                });

        StepVerifier.create(result)
                .assertNext(tuple -> {
                    AffectedMappingResponse.PropagatedEntities propagated = tuple
                            .getT1();
                    Constraint savedConstraint = tuple.getT2();

                    AffectedMappingResponse.PropagatedConstraint propagatedConstraint = propagated
                            .constraints().get(0);

                    assertThat(propagatedConstraint.sourceType())
                            .isEqualTo(EntityType.RELATIONSHIP.name());
                    assertThat(propagatedConstraint.sourceId())
                            .isEqualTo(sourceId);
                    assertThat(propagatedConstraint.tableId())
                            .isEqualTo(childTableId);
                    assertThat(propagatedConstraint.kind())
                            .isEqualTo("PRIMARY_KEY");
                    assertThat(propagatedConstraint.name())
                            .isEqualTo("pk_child");
                    assertThat(propagatedConstraint.constraintId())
                            .isNotEqualTo(constraintLogicalId);

                    assertThat(savedConstraint.getId())
                            .isEqualTo(propagatedConstraint.constraintId());
                    assertThat(savedConstraint.getTableId())
                            .isEqualTo(childTableId);
                    assertThat(savedConstraint.getKind())
                            .isEqualTo("PRIMARY_KEY");
                    assertThat(savedConstraint.getName())
                            .isEqualTo("pk_child");
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
                        EntityType.RELATIONSHIP.name(),
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
                            .isEqualTo(
                                    savedRelationshipColumn.getSrcColumnId());
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
                        EntityType.CONSTRAINT.name(),
                        Set.of(constraintColumnId));

        StepVerifier.create(result)
                .assertNext(propagated -> {
                    assertThat(propagated.columns()).hasSize(1);
                    AffectedMappingResponse.PropagatedColumn propagatedColumn = propagated
                            .columns().get(0);

                    assertThat(propagatedColumn.tableId())
                            .isEqualTo(childTableId);
                    assertThat(propagatedColumn.sourceType())
                            .isEqualTo(EntityType.CONSTRAINT.name());
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
                            .isEqualTo(EntityType.CONSTRAINT.name());
                    assertThat(propagatedRelationshipColumn.sourceId())
                            .isEqualTo(constraintId);

                    assertThat(propagated.constraintColumns()).isEmpty();
                    assertThat(propagated.indexColumns()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("요청 relationshipColumn은 propagated에서 제외된다")
    void saveAffectedEntitiesResult_excludesRequestedRelationshipColumnFromPropagation() {
        String schemaId = "schema-1";
        String parentTableId = "parent-table";
        String childTableId = "child-table";
        String parentColumnId = "parent-id";
        String relationshipId = "relationship-1";
        String requestRelationshipColumnId = "relcol-request";
        String propagatedRelationshipColumnId = "relcol-propagated";

        Validation.Database before = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .addTables(Validation.Table.newBuilder()
                                .setId(parentTableId)
                                .setSchemaId(schemaId)
                                .setName("parent")
                                .build())
                        .addTables(Validation.Table.newBuilder()
                                .setId(childTableId)
                                .setSchemaId(schemaId)
                                .setName("child")
                                .addRelationships(Validation.Relationship
                                        .newBuilder()
                                        .setId(relationshipId)
                                        .setSrcTableId(childTableId)
                                        .setTgtTableId(parentTableId)
                                        .setName("fk_child_parent")
                                        .setKind(
                                                Validation.RelationshipKind.NON_IDENTIFYING)
                                        .setCardinality(
                                                Validation.RelationshipCardinality.ONE_TO_MANY)
                                        .build())
                                .build())
                        .build())
                .build();

        Validation.Database after = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .addTables(Validation.Table.newBuilder()
                                .setId(parentTableId)
                                .setSchemaId(schemaId)
                                .setName("parent")
                                .setIsAffected(true)
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(parentColumnId)
                                        .setTableId(parentTableId)
                                        .setName("id")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .setIsAffected(true)
                                        .build())
                                .build())
                        .addTables(Validation.Table.newBuilder()
                                .setId(childTableId)
                                .setSchemaId(schemaId)
                                .setName("child")
                                .setIsAffected(true)
                                .addColumns(Validation.Column.newBuilder()
                                        .setId("child-fk-a")
                                        .setTableId(childTableId)
                                        .setName("parent_a")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .setIsAffected(true)
                                        .build())
                                .addColumns(Validation.Column.newBuilder()
                                        .setId("child-fk-b")
                                        .setTableId(childTableId)
                                        .setName("parent_b")
                                        .setOrdinalPosition(2)
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
                                                Validation.RelationshipKind.NON_IDENTIFYING)
                                        .setCardinality(
                                                Validation.RelationshipCardinality.ONE_TO_MANY)
                                        .setIsAffected(true)
                                        .addColumns(
                                                Validation.RelationshipColumn
                                                        .newBuilder()
                                                        .setId(requestRelationshipColumnId)
                                                        .setRelationshipId(
                                                                relationshipId)
                                                        .setFkColumnId(
                                                                "child-fk-a")
                                                        .setRefColumnId(
                                                                parentColumnId)
                                                        .setSeqNo(1)
                                                        .setIsAffected(true)
                                                        .build())
                                        .addColumns(
                                                Validation.RelationshipColumn
                                                        .newBuilder()
                                                        .setId(propagatedRelationshipColumnId)
                                                        .setRelationshipId(
                                                                relationshipId)
                                                        .setFkColumnId(
                                                                "child-fk-b")
                                                        .setRefColumnId(
                                                                parentColumnId)
                                                        .setSeqNo(2)
                                                        .setIsAffected(true)
                                                        .build())
                                        .build())
                                .build())
                        .build())
                .build();

        Mono<AffectedEntitiesSaver.SaveResult> result = affectedEntitiesSaver
                .saveAffectedEntitiesResult(
                        before,
                        after,
                        null,
                        relationshipId,
                        EntityType.RELATIONSHIP.name(),
                        Set.of(),
                        Set.of(requestRelationshipColumnId));

        StepVerifier.create(result)
                .assertNext(saveResult -> {
                    String savedRequestRelationshipColumnId = saveResult
                            .idMappings()
                            .relationshipColumns()
                            .get(requestRelationshipColumnId);
                    String savedPropagatedRelationshipColumnId = saveResult
                            .idMappings()
                            .relationshipColumns()
                            .get(propagatedRelationshipColumnId);

                    assertThat(savedRequestRelationshipColumnId).isNotNull();
                    assertThat(savedPropagatedRelationshipColumnId).isNotNull();
                    assertThat(saveResult.propagated().relationshipColumns())
                            .extracting(
                                    AffectedMappingResponse.PropagatedRelationshipColumn::relationshipColumnId)
                            .containsExactly(
                                    savedPropagatedRelationshipColumnId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("요청 constraint/constraintColumn은 propagated에서 제외된다")
    void saveAffectedEntitiesResult_excludesRequestedConstraintFromPropagation() {
        String schemaId = "schema-1";
        String tableId = "table-1";
        String constraintId = "constraint-1";
        String columnIdA = "column-a";
        String columnIdB = "column-b";
        String requestConstraintColumnId = "constraint-col-request";
        String propagatedConstraintColumnId = "constraint-col-propagated";

        Validation.Database before = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .addTables(Validation.Table.newBuilder()
                                .setId(tableId)
                                .setSchemaId(schemaId)
                                .setName("table")
                                .build())
                        .build())
                .build();

        Validation.Database after = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .addTables(Validation.Table.newBuilder()
                                .setId(tableId)
                                .setSchemaId(schemaId)
                                .setName("table")
                                .setIsAffected(true)
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(columnIdA)
                                        .setTableId(tableId)
                                        .setName("a")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .setIsAffected(true)
                                        .build())
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(columnIdB)
                                        .setTableId(tableId)
                                        .setName("b")
                                        .setOrdinalPosition(2)
                                        .setDataType("INT")
                                        .setIsAffected(true)
                                        .build())
                                .addConstraints(Validation.Constraint
                                        .newBuilder()
                                        .setId(constraintId)
                                        .setTableId(tableId)
                                        .setName("pk_table")
                                        .setKind(
                                                Validation.ConstraintKind.PRIMARY_KEY)
                                        .setIsAffected(true)
                                        .addColumns(
                                                Validation.ConstraintColumn
                                                        .newBuilder()
                                                        .setId(requestConstraintColumnId)
                                                        .setConstraintId(
                                                                constraintId)
                                                        .setColumnId(columnIdA)
                                                        .setSeqNo(1)
                                                        .setIsAffected(true)
                                                        .build())
                                        .addColumns(
                                                Validation.ConstraintColumn
                                                        .newBuilder()
                                                        .setId(propagatedConstraintColumnId)
                                                        .setConstraintId(
                                                                constraintId)
                                                        .setColumnId(columnIdB)
                                                        .setSeqNo(2)
                                                        .setIsAffected(true)
                                                        .build())
                                        .build())
                                .build())
                        .build())
                .build();

        Mono<AffectedEntitiesSaver.SaveResult> result = affectedEntitiesSaver
                .saveAffectedEntitiesResult(
                        before,
                        after,
                        null,
                        constraintId,
                        EntityType.CONSTRAINT.name(),
                        Set.of(),
                        Set.of(constraintId, requestConstraintColumnId));

        StepVerifier.create(result)
                .assertNext(saveResult -> {
                    String savedConstraintId = saveResult.idMappings()
                            .constraints()
                            .get(constraintId);
                    String savedRequestConstraintColumnId = saveResult
                            .idMappings()
                            .constraintColumns()
                            .get(requestConstraintColumnId);
                    String savedPropagatedConstraintColumnId = saveResult
                            .idMappings()
                            .constraintColumns()
                            .get(propagatedConstraintColumnId);

                    assertThat(savedConstraintId).isNotNull();
                    assertThat(savedRequestConstraintColumnId).isNotNull();
                    assertThat(savedPropagatedConstraintColumnId).isNotNull();

                    assertThat(saveResult.propagated().constraints()).isEmpty();
                    assertThat(saveResult.propagated().constraintColumns())
                            .extracting(
                                    AffectedMappingResponse.PropagatedConstraintColumn::constraintColumnId)
                            .containsExactly(
                                    savedPropagatedConstraintColumnId);
                    assertThat(saveResult.propagated().constraintColumns()
                            .get(0)
                            .constraintId())
                            .isEqualTo(savedConstraintId);
                    assertThat(saveResult.propagated().constraintColumns()
                            .get(0)
                            .seqNo())
                            .isEqualTo(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("전파로 생성된 indexColumn은 propagated.indexColumns로 반환된다")
    void saveAffectedEntities_propagatedIndexColumn_includedInResponse() {
        String schemaId = "schema-1";
        String tableId = "table-1";
        String columnId = "col-logical";
        String indexId = "idx-logical";
        String indexColumnId = "idxcol-logical";
        String sourceId = "relationship-1";

        Validation.Database before = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .addTables(Validation.Table.newBuilder()
                                .setId(tableId)
                                .setSchemaId(schemaId)
                                .setName("table")
                                .build())
                        .build())
                .build();

        Validation.Database after = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .addTables(Validation.Table.newBuilder()
                                .setId(tableId)
                                .setSchemaId(schemaId)
                                .setName("table")
                                .setIsAffected(true)
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(columnId)
                                        .setTableId(tableId)
                                        .setName("id")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .setIsAffected(true)
                                        .build())
                                .addIndexes(Validation.Index.newBuilder()
                                        .setId(indexId)
                                        .setTableId(tableId)
                                        .setName("idx_test")
                                        .setType(Validation.IndexType.BTREE)
                                        .setIsAffected(true)
                                        .addColumns(Validation.IndexColumn
                                                .newBuilder()
                                                .setId(indexColumnId)
                                                .setIndexId(indexId)
                                                .setColumnId(columnId)
                                                .setSeqNo(1)
                                                .setSortDir(
                                                        Validation.IndexSortDir.ASC)
                                                .setIsAffected(true)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        Mono<AffectedEntitiesSaver.SaveResult> result = affectedEntitiesSaver
                .saveAffectedEntitiesResult(
                        before,
                        after,
                        null,
                        sourceId,
                        EntityType.RELATIONSHIP.name(),
                        Set.of(),
                        Set.of());

        StepVerifier.create(result)
                .assertNext(saveResult -> {
                    String savedColumnId = saveResult.idMappings()
                            .columns()
                            .get(columnId);
                    String savedIndexId = saveResult.idMappings()
                            .indexes()
                            .get(indexId);
                    String savedIndexColumnId = saveResult.idMappings()
                            .indexColumns()
                            .get(indexColumnId);

                    assertThat(savedColumnId).isNotNull();
                    assertThat(savedIndexId).isNotNull();
                    assertThat(savedIndexColumnId).isNotNull();

                    assertThat(saveResult.propagated().indexColumns())
                            .hasSize(1);
                    AffectedMappingResponse.PropagatedIndexColumn propagatedIndexColumn = saveResult
                            .propagated()
                            .indexColumns()
                            .get(0);

                    assertThat(propagatedIndexColumn.indexColumnId())
                            .isEqualTo(savedIndexColumnId);
                    assertThat(propagatedIndexColumn.indexId())
                            .isEqualTo(savedIndexId);
                    assertThat(propagatedIndexColumn.columnId())
                            .isEqualTo(savedColumnId);
                    assertThat(propagatedIndexColumn.seqNo())
                            .isEqualTo(1);
                    assertThat(propagatedIndexColumn.sourceType())
                            .isEqualTo(EntityType.RELATIONSHIP.name());
                    assertThat(propagatedIndexColumn.sourceId())
                            .isEqualTo(sourceId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("saveAffectedEntitiesResult: 저장된 엔티티의 logicalId -> persistedId 매핑을 반환한다")
    void saveAffectedEntitiesResult_returnsIdMappings() {
        String schemaId = "schema-1";
        String tableId = "table-1";
        String columnId = "col-1";
        String indexId = "idx-1";
        String indexColumnId = "idxcol-1";
        String constraintId = "constraint-1";
        String constraintColumnId = "constraintcol-1";
        String relationshipId = "relationship-1";
        String relationshipColumnId = "relcol-1";

        Validation.Database before = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .addTables(Validation.Table.newBuilder()
                                .setId(tableId)
                                .addIndexes(Validation.Index.newBuilder()
                                        .setId(indexId)
                                        .build())
                                .addConstraints(Validation.Constraint
                                        .newBuilder()
                                        .setId(constraintId)
                                        .build())
                                .addRelationships(Validation.Relationship
                                        .newBuilder()
                                        .setId(relationshipId)
                                        .build())
                                .build())
                        .build())
                .build();

        Validation.Database after = Validation.Database.newBuilder()
                .addSchemas(Validation.Schema.newBuilder()
                        .setId(schemaId)
                        .setName("test-schema")
                        .setIsAffected(true)
                        .addTables(Validation.Table.newBuilder()
                                .setId(tableId)
                                .setSchemaId(schemaId)
                                .setName("test-table")
                                .setIsAffected(true)
                                .addColumns(Validation.Column.newBuilder()
                                        .setId(columnId)
                                        .setTableId(tableId)
                                        .setName("id")
                                        .setOrdinalPosition(1)
                                        .setDataType("INT")
                                        .setIsAffected(true)
                                        .build())
                                .addIndexes(Validation.Index.newBuilder()
                                        .setId(indexId)
                                        .setTableId(tableId)
                                        .setName("idx_test")
                                        .setType(Validation.IndexType.BTREE)
                                        .setComment("")
                                        .addColumns(Validation.IndexColumn
                                                .newBuilder()
                                                .setId(indexColumnId)
                                                .setIndexId(indexId)
                                                .setColumnId(columnId)
                                                .setSeqNo(1)
                                                .setSortDir(
                                                        Validation.IndexSortDir.ASC)
                                                .setIsAffected(true)
                                                .build())
                                        .build())
                                .addConstraints(Validation.Constraint
                                        .newBuilder()
                                        .setId(constraintId)
                                        .setTableId(tableId)
                                        .setName("pk_test")
                                        .setKind(
                                                Validation.ConstraintKind.PRIMARY_KEY)
                                        .addColumns(
                                                Validation.ConstraintColumn
                                                        .newBuilder()
                                                        .setId(constraintColumnId)
                                                        .setConstraintId(
                                                                constraintId)
                                                        .setColumnId(columnId)
                                                        .setSeqNo(1)
                                                        .setIsAffected(true)
                                                        .build())
                                        .build())
                                .addRelationships(Validation.Relationship
                                        .newBuilder()
                                        .setId(relationshipId)
                                        .setSrcTableId(tableId)
                                        .setTgtTableId(tableId)
                                        .setName("rel_test")
                                        .setKind(
                                                Validation.RelationshipKind.NON_IDENTIFYING)
                                        .setCardinality(
                                                Validation.RelationshipCardinality.ONE_TO_MANY)
                                        .addColumns(
                                                Validation.RelationshipColumn
                                                        .newBuilder()
                                                        .setId(relationshipColumnId)
                                                        .setRelationshipId(
                                                                relationshipId)
                                                        .setFkColumnId(columnId)
                                                        .setRefColumnId(
                                                                columnId)
                                                        .setSeqNo(1)
                                                        .setIsAffected(true)
                                                        .build())
                                        .build())
                                .build())
                        .build())
                .build();

        Mono<Tuple3<AffectedEntitiesSaver.SaveResult, Column, RelationshipColumn>> result = affectedEntitiesSaver
                .saveAffectedEntitiesResult(before, after)
                .flatMap(saveResult -> {
                    IdMappings idMappings = saveResult.idMappings();
                    return Mono.zip(
                            Mono.just(saveResult),
                            columnRepository.findByIdAndDeletedAtIsNull(
                                    idMappings.columns().get(columnId)),
                            relationshipColumnRepository
                                    .findByIdAndDeletedAtIsNull(idMappings
                                            .relationshipColumns()
                                            .get(relationshipColumnId)));
                });

        StepVerifier.create(result)
                .assertNext(tuple -> {
                    AffectedEntitiesSaver.SaveResult saveResult = tuple.getT1();
                    Column savedColumn = tuple.getT2();
                    RelationshipColumn savedRelationshipColumn = tuple.getT3();

                    IdMappings idMappings = saveResult.idMappings();

                    assertThat(idMappings.schemas()).isEmpty();
                    assertThat(idMappings.tables()).isEmpty();
                    assertThat(idMappings.columns()).containsKey(columnId);
                    assertThat(idMappings.indexes()).isEmpty();
                    assertThat(idMappings.indexColumns())
                            .containsKey(indexColumnId);
                    assertThat(idMappings.constraints()).isEmpty();
                    assertThat(idMappings.constraintColumns())
                            .containsKey(constraintColumnId);
                    assertThat(idMappings.relationships()).isEmpty();
                    assertThat(idMappings.relationshipColumns())
                            .containsKey(relationshipColumnId);

                    assertThat(idMappings.columns().get(columnId))
                            .isNotEqualTo(columnId);
                    assertThat(idMappings.relationshipColumns()
                            .get(relationshipColumnId))
                            .isNotEqualTo(relationshipColumnId);

                    assertThat(savedRelationshipColumn.getSrcColumnId())
                            .isEqualTo(savedColumn.getId());
                    assertThat(savedRelationshipColumn.getTgtColumnId())
                            .isEqualTo(savedColumn.getId());
                })
                .verifyComplete();
    }

}
