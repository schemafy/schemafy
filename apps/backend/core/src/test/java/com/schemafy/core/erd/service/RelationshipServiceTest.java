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
                        .addTables(Validation.Table.newBuilder()
                                .setId("table-1")
                                .setName("parent-table")
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
    @DisplayName("getRelationship: 저장된 관계를 조회한다")
    void getRelationship_success() {
        Relationship saved = relationshipRepository.save(
                Relationship.builder()
                        .srcTableId("table-1")
                        .tgtTableId("table-2")
                        .name("fk_test")
                        .kind("NON_IDENTIFYING")
                        .cardinality("ONE_TO_MANY")
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
                .cardinality("ONE_TO_MANY").build();
        Relationship r2 = Relationship.builder()
                .srcTableId("table-A").tgtTableId("table-C")
                .name("fk_a_c").kind("IDENTIFYING")
                .cardinality("ONE_TO_ONE").build();
        Relationship rOther = Relationship.builder()
                .srcTableId("table-B").tgtTableId("table-C")
                .name("fk_b_c").kind("NON_IDENTIFYING")
                .cardinality("ONE_TO_MANY").build();

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
                    assertThat(relationshipColumn.getId())
                            .isEqualTo("relationship-column-1");
                    assertThat(relationshipColumn.getRelationshipId())
                            .isEqualTo("relationship-1");
                    assertThat(relationshipColumn.getSrcColumnId())
                            .isEqualTo("src-column-1");
                    assertThat(relationshipColumn.getTgtColumnId())
                            .isEqualTo("tgt-column-1");
                    assertThat(relationshipColumn.getSeqNo()).isEqualTo(1);
                })
                .verifyComplete();

        // DB 반영 확인
        StepVerifier
                .create(relationshipColumnRepository
                        .findById("relationship-column-1"))
                .assertNext(found -> {
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
