package com.schemafy.core.erd.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.core.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

/** Table API 문서화를 위한 스니펫 제공 클래스 */
public class TableApiSnippets extends RestDocsSnippets {

  // ========== Table 도메인 공통 필드 ==========

  /** TableResponse 필드 */
  private static FieldDescriptor[] tableResponseFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("테이블 ID (ULID)"),
      fieldWithPath(prefix + "schemaId").type(JsonFieldType.STRING)
          .description("스키마 ID (ULID)"),
      fieldWithPath(prefix + "name").type(JsonFieldType.STRING)
          .description("테이블 이름"),
      fieldWithPath(prefix + "charset").type(JsonFieldType.STRING)
          .description("문자셋").optional(),
      fieldWithPath(prefix + "collation").type(JsonFieldType.STRING)
          .description("콜레이션").optional(),
      fieldWithPath(prefix + "extra").type(JsonFieldType.STRING)
          .description("프론트엔드 메타데이터(JSON 문자열)").optional()
    };
  }

  /** ColumnResponse 필드 */
  private static FieldDescriptor[] columnResponseFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("컬럼 ID (ULID)"),
      fieldWithPath(prefix + "tableId").type(JsonFieldType.STRING)
          .description("테이블 ID (ULID)"),
      fieldWithPath(prefix + "name").type(JsonFieldType.STRING)
          .description("컬럼 이름"),
      fieldWithPath(prefix + "dataType").type(JsonFieldType.STRING)
          .description("데이터 타입"),
      fieldWithPath(prefix + "lengthScale").type(JsonFieldType.OBJECT)
          .description("길이/스케일 정보").optional(),
      fieldWithPath(prefix + "lengthScale.length").type(JsonFieldType.NUMBER)
          .description("길이").optional(),
      fieldWithPath(prefix + "lengthScale.precision").type(JsonFieldType.NUMBER)
          .description("정밀도").optional(),
      fieldWithPath(prefix + "lengthScale.scale").type(JsonFieldType.NUMBER)
          .description("스케일").optional(),
      fieldWithPath(prefix + "lengthScale.empty").type(JsonFieldType.BOOLEAN)
          .description("길이/스케일 비어있음 여부").optional(),
      fieldWithPath(prefix + "seqNo").type(JsonFieldType.NUMBER)
          .description("순서 번호"),
      fieldWithPath(prefix + "autoIncrement").type(JsonFieldType.BOOLEAN)
          .description("자동 증가 여부"),
      fieldWithPath(prefix + "charset").type(JsonFieldType.STRING)
          .description("문자셋").optional(),
      fieldWithPath(prefix + "collation").type(JsonFieldType.STRING)
          .description("콜레이션").optional(),
      fieldWithPath(prefix + "comment").type(JsonFieldType.STRING)
          .description("코멘트").optional()
    };
  }

  /** ConstraintResponse 필드 */
  private static FieldDescriptor[] constraintResponseFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("제약조건 ID (ULID)"),
      fieldWithPath(prefix + "tableId").type(JsonFieldType.STRING)
          .description("테이블 ID (ULID)"),
      fieldWithPath(prefix + "name").type(JsonFieldType.STRING)
          .description("제약조건 이름"),
      fieldWithPath(prefix + "kind").type(JsonFieldType.STRING)
          .description("제약조건 종류 (PRIMARY_KEY, UNIQUE, CHECK, DEFAULT, NOT_NULL)"),
      fieldWithPath(prefix + "checkExpr").type(JsonFieldType.STRING)
          .description("CHECK 표현식").optional(),
      fieldWithPath(prefix + "defaultExpr").type(JsonFieldType.STRING)
          .description("DEFAULT 표현식").optional()
    };
  }

  /** ConstraintColumnResponse 필드 */
  private static FieldDescriptor[] constraintColumnResponseFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("제약조건 컬럼 ID (ULID)"),
      fieldWithPath(prefix + "constraintId").type(JsonFieldType.STRING)
          .description("제약조건 ID (ULID)"),
      fieldWithPath(prefix + "columnId").type(JsonFieldType.STRING)
          .description("컬럼 ID (ULID)"),
      fieldWithPath(prefix + "seqNo").type(JsonFieldType.NUMBER)
          .description("순서 번호")
    };
  }

  /** RelationshipResponse 필드 */
  private static FieldDescriptor[] relationshipResponseFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("관계 ID (ULID)"),
      fieldWithPath(prefix + "fkTableId").type(JsonFieldType.STRING)
          .description("FK 테이블 ID (ULID)"),
      fieldWithPath(prefix + "pkTableId").type(JsonFieldType.STRING)
          .description("PK 테이블 ID (ULID)"),
      fieldWithPath(prefix + "name").type(JsonFieldType.STRING)
          .description("관계 이름"),
      fieldWithPath(prefix + "kind").type(JsonFieldType.STRING)
          .description("관계 종류 (IDENTIFYING, NON_IDENTIFYING)"),
      fieldWithPath(prefix + "cardinality").type(JsonFieldType.STRING)
          .description("카디널리티 (ONE_TO_ONE, ONE_TO_MANY)"),
      fieldWithPath(prefix + "extra").type(JsonFieldType.STRING)
          .description("프론트엔드 메타데이터(JSON 문자열, 예: position, color)").optional()
    };
  }

  /** RelationshipColumnResponse 필드 */
  private static FieldDescriptor[] relationshipColumnResponseFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("관계 컬럼 ID (ULID)"),
      fieldWithPath(prefix + "relationshipId").type(JsonFieldType.STRING)
          .description("관계 ID (ULID)"),
      fieldWithPath(prefix + "pkColumnId").type(JsonFieldType.STRING)
          .description("PK 컬럼 ID (ULID)"),
      fieldWithPath(prefix + "fkColumnId").type(JsonFieldType.STRING)
          .description("FK 컬럼 ID (ULID)"),
      fieldWithPath(prefix + "seqNo").type(JsonFieldType.NUMBER)
          .description("순서 번호")
    };
  }

  /** IndexResponse 필드 */
  private static FieldDescriptor[] indexResponseFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("인덱스 ID (ULID)"),
      fieldWithPath(prefix + "tableId").type(JsonFieldType.STRING)
          .description("테이블 ID (ULID)"),
      fieldWithPath(prefix + "name").type(JsonFieldType.STRING)
          .description("인덱스 이름"),
      fieldWithPath(prefix + "type").type(JsonFieldType.STRING)
          .description("인덱스 타입 (BTREE, HASH, FULLTEXT, SPATIAL, OTHER)")
    };
  }

  /** IndexColumnResponse 필드 */
  private static FieldDescriptor[] indexColumnResponseFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("인덱스 컬럼 ID (ULID)"),
      fieldWithPath(prefix + "indexId").type(JsonFieldType.STRING)
          .description("인덱스 ID (ULID)"),
      fieldWithPath(prefix + "columnId").type(JsonFieldType.STRING)
          .description("컬럼 ID (ULID)"),
      fieldWithPath(prefix + "seqNo").type(JsonFieldType.NUMBER)
          .description("순서 번호"),
      fieldWithPath(prefix + "sortDirection").type(JsonFieldType.STRING)
          .description("정렬 방향 (ASC, DESC)")
    };
  }

  // ========== POST /api/tables - 테이블 생성 ==========

  /** 테이블 생성 요청 헤더 */
  public static Snippet createTableRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 테이블 생성 요청 바디 */
  public static Snippet createTableRequest() {
    return requestFields(
        fieldWithPath("schemaId").type(JsonFieldType.STRING)
            .description("스키마 ID (ULID)"),
        fieldWithPath("name").type(JsonFieldType.STRING)
            .description("테이블 이름"),
        fieldWithPath("charset").type(JsonFieldType.STRING)
            .description("문자셋").optional(),
        fieldWithPath("collation").type(JsonFieldType.STRING)
            .description("콜레이션").optional());
  }

  /** 테이블 생성 응답 헤더 */
  public static Snippet createTableResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 테이블 생성 응답 */
  public static Snippet createTableResponse() {
    return createResponseFieldsSnippet(
        mutationResponseFields(tableResponseFields("data.")));
  }

  // ========== GET /api/tables/{tableId} - 테이블 조회 ==========

  /** 테이블 조회 경로 파라미터 */
  public static Snippet getTablePathParameters() {
    return pathParameters(
        parameterWithName("tableId")
            .description("조회할 테이블 ID (ULID)"));
  }

  /** 테이블 조회 요청 헤더 */
  public static Snippet getTableRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 테이블 조회 응답 헤더 */
  public static Snippet getTableResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 테이블 조회 응답 */
  public static Snippet getTableResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(tableResponseFields("")));
  }

  // ========== GET /api/tables/{tableId}/snapshot - 테이블 스냅샷 조회 ==========

  /** 테이블 스냅샷 조회 경로 파라미터 */
  public static Snippet getTableSnapshotPathParameters() {
    return pathParameters(
        parameterWithName("tableId")
            .description("조회할 테이블 ID (ULID)"));
  }

  /** 테이블 스냅샷 조회 요청 헤더 */
  public static Snippet getTableSnapshotRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 테이블 스냅샷 조회 응답 헤더 */
  public static Snippet getTableSnapshotResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 테이블 스냅샷 조회 응답 */
  public static Snippet getTableSnapshotResponse() {
    return createResponseFieldsSnippet(concat(
        successResponseFields(
            fieldWithPath("table").type(JsonFieldType.OBJECT)
                .description("테이블 정보"),
            fieldWithPath("columns").type(JsonFieldType.ARRAY)
                .description("컬럼 목록"),
            fieldWithPath("constraints").type(JsonFieldType.ARRAY)
                .description("제약조건 목록"),
            fieldWithPath("relationships").type(JsonFieldType.ARRAY)
                .description("관계 목록"),
            fieldWithPath("indexes").type(JsonFieldType.ARRAY)
                .description("인덱스 목록")),
        concat(
            tableResponseFields("table."),
            concat(
                columnResponseFields("columns[]."),
                concat(
                    new FieldDescriptor[] {
                      fieldWithPath("constraints[].constraint")
                          .type(JsonFieldType.OBJECT)
                          .description("제약조건 정보"),
                      fieldWithPath("constraints[].columns")
                          .type(JsonFieldType.ARRAY)
                          .description("제약조건 컬럼 목록")
                    },
                    concat(
                        constraintResponseFields("constraints[].constraint."),
                        concat(
                            constraintColumnResponseFields("constraints[].columns[]."),
                            concat(
                                new FieldDescriptor[] {
                                  fieldWithPath("relationships[].relationship")
                                      .type(JsonFieldType.OBJECT)
                                      .description("관계 정보"),
                                  fieldWithPath("relationships[].columns")
                                      .type(JsonFieldType.ARRAY)
                                      .description("관계 컬럼 목록"),
                                  fieldWithPath("indexes[].index")
                                      .type(JsonFieldType.OBJECT)
                                      .description("인덱스 정보"),
                                  fieldWithPath("indexes[].columns")
                                      .type(JsonFieldType.ARRAY)
                                      .description("인덱스 컬럼 목록")
                                },
                                concat(
                                    relationshipResponseFields("relationships[].relationship."),
                                    concat(
                                        relationshipColumnResponseFields("relationships[].columns[]."),
                                        concat(
                                            indexResponseFields("indexes[].index."),
                                            indexColumnResponseFields("indexes[].columns[]."))))))))))));
  }

  // ========== GET /api/tables/snapshots - 배치 테이블 스냅샷 조회 ==========

  /** 배치 테이블 스냅샷 조회 쿼리 파라미터 */
  public static Snippet getTableSnapshotsQueryParameters() {
    return queryParameters(
        parameterWithName("tableIds")
            .description("조회할 테이블 ID 목록 (콤마로 구분)"));
  }

  /** 배치 테이블 스냅샷 조회 요청 헤더 */
  public static Snippet getTableSnapshotsRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 배치 테이블 스냅샷 조회 응답 헤더 */
  public static Snippet getTableSnapshotsResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 배치 테이블 스냅샷 조회 응답 (Map 구조) */
  public static Snippet getTableSnapshotsResponse() {
    return createResponseFieldsSnippet(concat(
        new FieldDescriptor[] {
          fieldWithPath("*.table").type(JsonFieldType.OBJECT)
              .description("테이블 정보"),
          fieldWithPath("*.columns").type(JsonFieldType.ARRAY)
              .description("컬럼 목록"),
          fieldWithPath("*.constraints").type(JsonFieldType.ARRAY)
              .description("제약조건 목록"),
          fieldWithPath("*.relationships").type(JsonFieldType.ARRAY)
              .description("관계 목록"),
          fieldWithPath("*.indexes").type(JsonFieldType.ARRAY)
              .description("인덱스 목록")
        },
        tableResponseFields("*.table.")));
  }

  // ========== GET /api/schemas/{schemaId}/tables - 스키마별 테이블 목록 조회 ==========

  /** 스키마별 테이블 목록 조회 경로 파라미터 */
  public static Snippet getTablesBySchemaIdPathParameters() {
    return pathParameters(
        parameterWithName("schemaId")
            .description("조회할 스키마 ID (ULID)"));
  }

  /** 스키마별 테이블 목록 조회 요청 헤더 */
  public static Snippet getTablesBySchemaIdRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 스키마별 테이블 목록 조회 응답 헤더 */
  public static Snippet getTablesBySchemaIdResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 스키마별 테이블 목록 조회 응답 */
  public static Snippet getTablesBySchemaIdResponse() {
    return createResponseFieldsSnippet(
        tableResponseFields("[]."));
  }

  // ========== PATCH /api/tables/{tableId}/name - 테이블 이름 변경 ==========

  /** 테이블 이름 변경 경로 파라미터 */
  public static Snippet changeTableNamePathParameters() {
    return pathParameters(
        parameterWithName("tableId")
            .description("변경할 테이블 ID (ULID)"));
  }

  /** 테이블 이름 변경 요청 헤더 */
  public static Snippet changeTableNameRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 테이블 이름 변경 요청 바디 */
  public static Snippet changeTableNameRequest() {
    return requestFields(
        fieldWithPath("newName").type(JsonFieldType.STRING)
            .description("변경할 테이블 이름"));
  }

  /** 테이블 이름 변경 응답 헤더 */
  public static Snippet changeTableNameResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 테이블 이름 변경 응답 */
  public static Snippet changeTableNameResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== PATCH /api/tables/{tableId}/meta - 테이블 메타 변경 ==========

  /** 테이블 메타 변경 경로 파라미터 */
  public static Snippet changeTableMetaPathParameters() {
    return pathParameters(
        parameterWithName("tableId")
            .description("변경할 테이블 ID (ULID)"));
  }

  /** 테이블 메타 변경 요청 헤더 */
  public static Snippet changeTableMetaRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 테이블 메타 변경 요청 바디 */
  public static Snippet changeTableMetaRequest() {
    return requestFields(
        fieldWithPath("charset").type(JsonFieldType.STRING)
            .description("문자셋").optional(),
        fieldWithPath("collation").type(JsonFieldType.STRING)
            .description("콜레이션").optional());
  }

  /** 테이블 메타 변경 응답 헤더 */
  public static Snippet changeTableMetaResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 테이블 메타 변경 응답 */
  public static Snippet changeTableMetaResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== PATCH /api/tables/{tableId}/extra - 테이블 추가정보 변경 ==========

  /** 테이블 추가정보 변경 경로 파라미터 */
  public static Snippet changeTableExtraPathParameters() {
    return pathParameters(
        parameterWithName("tableId")
            .description("변경할 테이블 ID (ULID)"));
  }

  /** 테이블 추가정보 변경 요청 헤더 */
  public static Snippet changeTableExtraRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 테이블 추가정보 변경 요청 바디 */
  public static Snippet changeTableExtraRequest() {
    return requestFields(
        subsectionWithPath("extra")
            .description("프론트엔드 메타데이터 (예: position, color 등 임의 JSON 구조, 문자열도 허용)")
            .optional());
  }

  /** 테이블 추가정보 변경 응답 헤더 */
  public static Snippet changeTableExtraResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 테이블 추가정보 변경 응답 */
  public static Snippet changeTableExtraResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== DELETE /api/tables/{tableId} - 테이블 삭제 ==========

  /** 테이블 삭제 경로 파라미터 */
  public static Snippet deleteTablePathParameters() {
    return pathParameters(
        parameterWithName("tableId")
            .description("삭제할 테이블 ID (ULID)"));
  }

  /** 테이블 삭제 요청 헤더 */
  public static Snippet deleteTableRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 테이블 삭제 응답 헤더 */
  public static Snippet deleteTableResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 테이블 삭제 응답 */
  public static Snippet deleteTableResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

}
