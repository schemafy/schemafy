package com.schemafy.core.erd.service;

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
import com.schemafy.core.erd.controller.dto.response.ColumnResponse;
import com.schemafy.core.erd.repository.ColumnRepository;
import com.schemafy.core.erd.repository.entity.Column;
import com.schemafy.core.validation.client.ValidationClient;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import validation.Validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("ColumnService 테스트")
class ColumnServiceTest {

    @Autowired
    ColumnService columnService;

    @Autowired
    ColumnRepository columnRepository;

    @MockitoBean
    ValidationClient validationClient;

    @BeforeEach
    void setUp() {
        columnRepository.deleteAll().block();
        given(validationClient.changeColumnName(
                any(Validation.ChangeColumnNameRequest.class)))
                .willReturn(Mono.just(
                        Validation.Database.newBuilder().build()));
        given(validationClient.changeColumnType(
                any(Validation.ChangeColumnTypeRequest.class)))
                .willReturn(Mono.just(
                        Validation.Database.newBuilder().build()));
        given(validationClient.changeColumnPosition(
                any(Validation.ChangeColumnPositionRequest.class)))
                .willReturn(Mono.just(
                        Validation.Database.newBuilder().build()));
        given(validationClient
                .deleteColumn(any(Validation.DeleteColumnRequest.class)))
                .willReturn(Mono.just(
                        Validation.Database.newBuilder().build()));
    }

    @Test
    @DisplayName("createColumn: 컬럼 생성 시 매핑 정보가 올바르게 반환된다")
    void createColumn_mappingResponse_success() {
        // given
        Validation.CreateColumnRequest request = Validation.CreateColumnRequest
                .newBuilder()
                .setColumn(Validation.Column.newBuilder()
                        .setId("fe-column-id")
                        .setTableId("table-1")
                        .setName("test_column")
                        .setDataType("VARCHAR(255)")
                        .setIsAutoIncrement(false)
                        .setComment("테스트 컬럼")
                        .setOrdinalPosition(1)
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
                                .addColumns(Validation.Column.newBuilder()
                                        .setId("be-column-id")
                                        .setTableId("table-1")
                                        .setName("test_column")
                                        .setDataType("VARCHAR(255)")
                                        .setIsAutoIncrement(false)
                                        .setComment("테스트 컬럼")
                                        .setOrdinalPosition(1)
                                        .setIsAffected(true)
                                        .build())
                                .build())
                        .build())
                .build();

        given(validationClient
                .createColumn(any(Validation.CreateColumnRequest.class)))
                .willReturn(Mono.just(mockResponse));

        // when
        Mono<AffectedMappingResponse> result = columnService
                .createColumn(request);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    // 컬럼 매핑 정보 확인 (FE-ID → BE-ID 매핑)
                    // columns는 tableId로 그룹핑된 nested map
                    assertThat(response.columns()).hasSize(1);
                    assertThat(response.columns().get("table-1"))
                            .containsEntry("fe-column-id", "be-column-id");

                    // 다른 매핑들은 비어있어야 함 (컬럼만 생성했으므로)
                    assertThat(response.schemas()).isEmpty();
                    assertThat(response.tables()).isEmpty();
                    assertThat(response.indexes()).isEmpty();
                    assertThat(response.indexColumns()).isEmpty();
                    assertThat(response.constraints()).isEmpty();
                    assertThat(response.constraintColumns()).isEmpty();
                    assertThat(response.relationships()).isEmpty();
                    assertThat(response.relationshipColumns()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getColumn: 저장된 컬럼을 조회한다")
    void getColumn_success() {
        Column saved = columnRepository.save(
                Column.builder()
                        .tableId("table-1")
                        .name("test_column")
                        .dataType("VARCHAR(255)")
                        .comment("테스트 컬럼")
                        .ordinalPosition(1)
                        .build())
                .block();

        Mono<ColumnResponse> result = columnService.getColumn(saved.getId());

        StepVerifier.create(result)
                .assertNext(found -> {
                    assertThat(found.getId()).isEqualTo(saved.getId());
                    assertThat(found.getName()).isEqualTo("test_column");
                    assertThat(found.getDataType()).isEqualTo("VARCHAR(255)");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getColumnsByTableId: 테이블 기준으로 컬럼 목록을 조회한다")
    void getColumnsByTableId_success() {
        Column c1 = Column.builder().tableId("table-A").name("column_a")
                .dataType("INT").ordinalPosition(1).build();
        Column c2 = Column.builder().tableId("table-A").name("column_b")
                .dataType("VARCHAR(100)").ordinalPosition(2)
                .build();
        Column cOther = Column.builder().tableId("table-B").name("column_c")
                .dataType("TEXT").ordinalPosition(1).build();

        columnRepository.save(c1)
                .then(columnRepository.save(c2))
                .then(columnRepository.save(cOther))
                .block();

        StepVerifier
                .create(columnService.getColumnsByTableId("table-A")
                        .collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(
                            list.stream().map(ColumnResponse::getName).toList())
                            .containsExactlyInAnyOrder("column_a", "column_b");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateColumnName: 존재하면 이름을 변경한다")
    void updateColumnName_success() {
        Column saved = columnRepository.save(
                Column.builder()
                        .tableId("table-1")
                        .name("old_name")
                        .dataType("VARCHAR(255)")
                        .ordinalPosition(1)
                        .build())
                .block();

        StepVerifier.create(columnService.updateColumnName(
                Validation.ChangeColumnNameRequest.newBuilder()
                        .setColumnId(saved.getId())
                        .setNewName("new_name")
                        .build()))
                .assertNext(updated -> {
                    assertThat(updated.getId()).isEqualTo(saved.getId());
                    assertThat(updated.getName()).isEqualTo("new_name");
                })
                .verifyComplete();

        // DB 반영 확인
        StepVerifier.create(columnRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.getName())
                        .isEqualTo("new_name"))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateColumnName: 존재하지 않으면 에러를 반환한다")
    void updateColumnName_notFound() {
        StepVerifier.create(columnService.updateColumnName(
                Validation.ChangeColumnNameRequest.newBuilder()
                        .setColumnId("non-existent")
                        .setNewName("new_name")
                        .build()))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e)
                                .getErrorCode() == ErrorCode.ERD_COLUMN_NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("updateColumnType: 데이터 타입을 변경한다")
    void updateColumnType_success() {
        Column saved = columnRepository.save(
                Column.builder()
                        .tableId("table-1")
                        .name("test_column")
                        .dataType("VARCHAR(255)")
                        .ordinalPosition(1)
                        .build())
                .block();

        StepVerifier.create(columnService.updateColumnType(
                Validation.ChangeColumnTypeRequest.newBuilder()
                        .setColumnId(saved.getId())
                        .setDataType("TEXT")
                        .build()))
                .assertNext(updated -> {
                    assertThat(updated.getId()).isEqualTo(saved.getId());
                    assertThat(updated.getDataType()).isEqualTo("TEXT");
                })
                .verifyComplete();

        // DB 반영 확인
        StepVerifier.create(columnRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.getDataType())
                        .isEqualTo("TEXT"))
                .verifyComplete();
    }

    @Test
    @DisplayName("updateColumnPosition: 컬럼 순서를 변경한다")
    void updateColumnPosition_success() {
        Column saved = columnRepository.save(
                Column.builder()
                        .tableId("table-1")
                        .name("test_column")
                        .dataType("VARCHAR(255)")
                        .ordinalPosition(1)
                        .build())
                .block();

        StepVerifier.create(columnService.updateColumnPosition(
                Validation.ChangeColumnPositionRequest.newBuilder()
                        .setColumnId(saved.getId())
                        .setNewPosition(3)
                        .build()))
                .assertNext(updated -> {
                    assertThat(updated.getId()).isEqualTo(saved.getId());
                    assertThat(updated.getOrdinalPosition()).isEqualTo(3);
                })
                .verifyComplete();

        // DB 반영 확인
        StepVerifier.create(columnRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.getOrdinalPosition())
                        .isEqualTo(3))
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteColumn: 소프트 삭제가 수행된다")
    void deleteColumn_softDelete() {
        Column saved = columnRepository.save(
                Column.builder()
                        .tableId("table-1")
                        .name("to_delete")
                        .dataType("VARCHAR(255)")
                        .ordinalPosition(1)
                        .build())
                .block();

        StepVerifier.create(columnService.deleteColumn(
                Validation.DeleteColumnRequest.newBuilder()
                        .setColumnId(saved.getId())
                        .build()))
                .verifyComplete();

        // 삭제 플래그 확인 (deletedAt not null)
        StepVerifier.create(columnRepository.findById(saved.getId()))
                .assertNext(found -> assertThat(found.isDeleted()).isTrue())
                .verifyComplete();
    }

}
