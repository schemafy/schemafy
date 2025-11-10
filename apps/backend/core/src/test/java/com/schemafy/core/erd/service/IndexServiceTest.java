package com.schemafy.core.erd.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;
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
import com.schemafy.core.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.core.erd.controller.dto.response.IndexResponse;
import com.schemafy.core.erd.repository.IndexColumnRepository;
import com.schemafy.core.erd.repository.IndexRepository;
import com.schemafy.core.erd.repository.entity.Index;
import com.schemafy.core.erd.repository.entity.IndexColumn;
import com.schemafy.core.validation.client.ValidationClient;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import validation.Validation;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("IndexService 테스트")
class IndexServiceTest {

    @Autowired
    IndexService indexService;

    @Autowired
    IndexRepository indexRepository;

    @Autowired
    IndexColumnRepository indexColumnRepository;

    @MockitoBean
    ValidationClient validationClient;

    @BeforeEach
    void setUp() {
        indexColumnRepository.deleteAll().block();
        indexRepository.deleteAll().block();

        given(validationClient
                .changeIndexName(any(Validation.ChangeIndexNameRequest.class)))
                        .willReturn(Mono.just(
                                Validation.Database.newBuilder().build()));
        given(validationClient.addColumnToIndex(
                any(Validation.AddColumnToIndexRequest.class)))
                        .willReturn(Mono.just(
                                Validation.Database.newBuilder().build()));
        given(validationClient.removeColumnFromIndex(
                any(Validation.RemoveColumnFromIndexRequest.class)))
                        .willReturn(Mono.just(
                                Validation.Database.newBuilder().build()));
        given(validationClient
                .deleteIndex(any(Validation.DeleteIndexRequest.class)))
                        .willReturn(Mono.just(
                                Validation.Database.newBuilder().build()));
    }

    @Test
    @DisplayName("createIndex: 인덱스 생성 시 매핑 정보가 올바르게 반환된다")
    void createIndex_mappingResponse_success() {
        // given
        Validation.CreateIndexRequest request = Validation.CreateIndexRequest
                .newBuilder()
                .setIndex(Validation.Index.newBuilder()
                        .setId("fe-index-id")
                        .setTableId("table-1")
                        .setName("idx_test")
                        .setType(Validation.IndexType.BTREE)
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
                        .setIsAffected(true)
                        .addTables(Validation.Table.newBuilder()
                                .setId("table-1")
                                .setName("test-table")
                                .setIsAffected(true)
                                .addIndexes(Validation.Index.newBuilder()
                                        .setId("be-index-id")
                                        .setTableId("table-1")
                                        .setName("idx_test")
                                        .setType(Validation.IndexType.BTREE)
                                        .setComment("")
                                        .setIsAffected(true)
                                        .build())
                                .build())
                        .build())
                .build();

        given(validationClient
                .createIndex(any(Validation.CreateIndexRequest.class)))
                        .willReturn(Mono.just(mockResponse));

        // when
        Mono<AffectedMappingResponse> result = indexService
                .createIndex(request);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    // 인덱스 매핑 정보 확인 (FE-ID → BE-ID 매핑)
                    // indexes는 tableId로 그룹핑된 nested map
                    assertThat(response.indexes()).hasSize(1);
                    assertThat(response.indexes().get("table-1"))
                            .containsEntry("fe-index-id", "be-index-id");

                    // 다른 매핑들은 비어있어야 함 (인덱스만 생성했으므로)
                    assertThat(response.schemas()).isEmpty();
                    assertThat(response.tables()).isEmpty();
                    assertThat(response.columns()).isEmpty();
                    assertThat(response.indexColumns()).isEmpty();
                    assertThat(response.constraints()).isEmpty();
                    assertThat(response.constraintColumns()).isEmpty();
                    assertThat(response.relationships()).isEmpty();
                    assertThat(response.relationshipColumns()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getIndex: 저장된 인덱스를 조회한다")
    void getIndex_success() {
        Index saved = indexRepository.save(
                Index.builder()
                        .tableId("table-1")
                        .name("idx_test")
                        .type("BTREE")
                        .comment("")
                        .build())
                .block();

        Mono<IndexResponse> result = indexService.getIndex(saved.getId());

        StepVerifier.create(result)
                .assertNext(found -> {
                    assertThat(found.getId()).isEqualTo(saved.getId());
                    assertThat(found.getName()).isEqualTo("idx_test");
                    assertThat(found.getType()).isEqualTo("BTREE");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getIndexesByTableId: 테이블 기준으로 인덱스 목록을 조회한다")
    void getIndexesByTableId_success() {
        Index i1 = Index.builder().tableId("table-A").name("idx_a")
                .type("BTREE").comment("").build();
        Index i2 = Index.builder().tableId("table-A").name("idx_b")
                .type("HASH").comment("").build();
        Index iOther = Index.builder().tableId("table-B").name("idx_c")
                .type("BTREE").comment("").build();

        indexRepository.save(i1)
                .then(indexRepository.save(i2))
                .then(indexRepository.save(iOther))
                .block();

        StepVerifier
                .create(indexService.getIndexesByTableId("table-A")
                        .collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.stream().map(IndexResponse::getName).toList())
                            .containsExactlyInAnyOrder("idx_a", "idx_b");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateIndexName: 존재하면 이름을 변경한다")
    void updateIndexName_success() {
        Index saved = indexRepository.save(
                Index.builder()
                        .tableId("table-1")
                        .name("old_name")
                        .type("BTREE")
                        .comment("")
                        .build())
                .block();

        StepVerifier.create(indexService.updateIndexName(
                Validation.ChangeIndexNameRequest.newBuilder()
                        .setIndexId(saved.getId())
                        .setNewName("new_name")
                        .build()))
                .assertNext(updated -> {
                    assertThat(updated.getId()).isEqualTo(saved.getId());
                    assertThat(updated.getName()).isEqualTo("new_name");
                })
                .verifyComplete();

        // DB 반영 확인
        StepVerifier.create(indexRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.getName())
                        .isEqualTo("new_name"))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateIndexName: 존재하지 않으면 에러를 반환한다")
    void updateIndexName_notFound() {
        StepVerifier.create(indexService.updateIndexName(
                Validation.ChangeIndexNameRequest.newBuilder()
                        .setIndexId("non-existent")
                        .setNewName("new_name")
                        .build()))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.ERD_INDEX_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("addColumnToIndex: 인덱스에 컬럼을 추가한다")
    void addColumnToIndex_success() {
        // given
        Validation.AddColumnToIndexRequest request = Validation.AddColumnToIndexRequest
                .newBuilder()
                .setIndexColumn(Validation.IndexColumn.newBuilder()
                        .setId("index-column-1")
                        .setIndexId("index-1")
                        .setColumnId("column-1")
                        .setSeqNo(1)
                        .setSortDir(Validation.IndexSortDir.ASC)
                        .build())
                .build();

        // when
        Mono<IndexColumnResponse> result = indexService.addColumnToIndex(request);

        // then
        StepVerifier.create(result)
                .assertNext(indexColumn -> {
                    assertThat(indexColumn.getId()).isNotNull(); // 자동 생성된 ID
                    assertThat(indexColumn.getIndexId()).isEqualTo("index-1");
                    assertThat(indexColumn.getColumnId()).isEqualTo("column-1");
                    assertThat(indexColumn.getSeqNo()).isEqualTo(1);
                    assertThat(indexColumn.getSortDir()).isEqualTo("ASC");
                })
                .verifyComplete();

        // DB 반영 확인 - 자동 생성된 ID 사용
        StepVerifier.create(indexColumnRepository.findAll().collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(1);
                    IndexColumn found = list.get(0);
                    assertThat(found.getIndexId()).isEqualTo("index-1");
                    assertThat(found.getColumnId()).isEqualTo("column-1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("removeColumnFromIndex: 인덱스에서 컬럼을 제거한다 (소프트 삭제)")
    void removeColumnFromIndex_success() {
        IndexColumn saved = indexColumnRepository.save(
                IndexColumn.builder()
                        .indexId("index-1")
                        .columnId("column-1")
                        .seqNo(1)
                        .sortDir("ASC")
                        .build())
                .block();

        StepVerifier.create(indexService.removeColumnFromIndex(
                Validation.RemoveColumnFromIndexRequest.newBuilder()
                        .setIndexColumnId(saved.getId())
                        .build()))
                .verifyComplete();

        // 삭제 플래그 확인 (deletedAt not null)
        StepVerifier.create(indexColumnRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.isDeleted()).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteIndex: 소프트 삭제가 수행된다")
    void deleteIndex_softDelete() {
        Index saved = indexRepository.save(
                Index.builder()
                        .tableId("table-1")
                        .name("to_delete")
                        .type("BTREE")
                        .comment("")
                        .build())
                .block();

        StepVerifier.create(indexService.deleteIndex(
                Validation.DeleteIndexRequest.newBuilder()
                        .setIndexId(saved.getId())
                        .build()))
                .verifyComplete();

        // 삭제 플래그 확인 (deletedAt not null)
        StepVerifier.create(indexRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.isDeleted()).isTrue())
                .verifyComplete();
    }

}
