package com.schemafy.core.erd.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.core.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

/** Index API 문서화를 위한 스니펫 제공 클래스 */
public class IndexApiSnippets extends RestDocsSnippets {

  // ========== Index 도메인 공통 필드 ==========

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
          .description("인덱스 타입 (BTREE, HASH, FULLTEXT, SPATIAL)")
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

  // ========== POST /api/indexes - 인덱스 생성 ==========

  public static Snippet createIndexRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet createIndexRequest() {
    return requestFields(
        fieldWithPath("tableId").type(JsonFieldType.STRING)
            .description("테이블 ID (ULID)"),
        fieldWithPath("name").type(JsonFieldType.STRING)
            .description("인덱스 이름"),
        fieldWithPath("type").type(JsonFieldType.STRING)
            .description("인덱스 타입 (BTREE, HASH, FULLTEXT, SPATIAL)"),
        fieldWithPath("columns").type(JsonFieldType.ARRAY)
            .description("인덱스 컬럼 목록").optional(),
        fieldWithPath("columns[].columnId").type(JsonFieldType.STRING)
            .description("컬럼 ID (ULID)").optional(),
        fieldWithPath("columns[].seqNo").type(JsonFieldType.NUMBER)
            .description("순서 번호 (미입력 시 0부터 자동 배정)").optional(),
        fieldWithPath("columns[].sortDirection").type(JsonFieldType.STRING)
            .description("정렬 방향 (ASC, DESC)").optional());
  }

  public static Snippet createIndexResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet createIndexResponse() {
    return createResponseFieldsSnippet(
        mutationResponseFields(indexResponseFields("result.data.")));
  }

  // ========== GET /api/indexes/{indexId} ==========

  public static Snippet getIndexPathParameters() {
    return pathParameters(
        parameterWithName("indexId").description("조회할 인덱스 ID (ULID)"));
  }

  public static Snippet getIndexRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet getIndexResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet getIndexResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(indexResponseFields("result.")));
  }

  // ========== GET /api/tables/{tableId}/indexes ==========

  public static Snippet getIndexesByTableIdPathParameters() {
    return pathParameters(
        parameterWithName("tableId").description("테이블 ID (ULID)"));
  }

  public static Snippet getIndexesByTableIdRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet getIndexesByTableIdResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet getIndexesByTableIdResponse() {
    return createResponseFieldsSnippet(concat(
        new FieldDescriptor[] {
          fieldWithPath("success").type(JsonFieldType.BOOLEAN)
              .description("요청 성공 여부"),
          fieldWithPath("result").type(JsonFieldType.ARRAY)
              .description("인덱스 목록")
        },
        concat(
            indexResponseFields("result[]."),
            new FieldDescriptor[] {
              fieldWithPath("error").type(JsonFieldType.NULL)
                  .description("에러 정보 (성공 시 null)").optional()
            })));
  }

  // ========== PATCH /api/indexes/{indexId}/name ==========

  public static Snippet changeIndexNamePathParameters() {
    return pathParameters(
        parameterWithName("indexId").description("변경할 인덱스 ID (ULID)"));
  }

  public static Snippet changeIndexNameRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet changeIndexNameRequest() {
    return requestFields(
        fieldWithPath("newName").type(JsonFieldType.STRING).description("변경할 인덱스 이름"));
  }

  public static Snippet changeIndexNameResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet changeIndexNameResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== PATCH /api/indexes/{indexId}/type ==========

  public static Snippet changeIndexTypePathParameters() {
    return pathParameters(
        parameterWithName("indexId").description("변경할 인덱스 ID (ULID)"));
  }

  public static Snippet changeIndexTypeRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet changeIndexTypeRequest() {
    return requestFields(
        fieldWithPath("type").type(JsonFieldType.STRING)
            .description("변경할 인덱스 타입 (BTREE, HASH, FULLTEXT, SPATIAL)"));
  }

  public static Snippet changeIndexTypeResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet changeIndexTypeResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== DELETE /api/indexes/{indexId} ==========

  public static Snippet deleteIndexPathParameters() {
    return pathParameters(
        parameterWithName("indexId").description("삭제할 인덱스 ID (ULID)"));
  }

  public static Snippet deleteIndexRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet deleteIndexResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet deleteIndexResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== GET /api/indexes/{indexId}/columns ==========

  public static Snippet getIndexColumnsPathParameters() {
    return pathParameters(
        parameterWithName("indexId").description("인덱스 ID (ULID)"));
  }

  public static Snippet getIndexColumnsRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet getIndexColumnsResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet getIndexColumnsResponse() {
    return createResponseFieldsSnippet(concat(
        new FieldDescriptor[] {
          fieldWithPath("success").type(JsonFieldType.BOOLEAN)
              .description("요청 성공 여부"),
          fieldWithPath("result").type(JsonFieldType.ARRAY)
              .description("인덱스 컬럼 목록")
        },
        concat(
            indexColumnResponseFields("result[]."),
            new FieldDescriptor[] {
              fieldWithPath("error").type(JsonFieldType.NULL)
                  .description("에러 정보 (성공 시 null)").optional()
            })));
  }

  // ========== POST /api/indexes/{indexId}/columns ==========

  public static Snippet addIndexColumnPathParameters() {
    return pathParameters(
        parameterWithName("indexId").description("인덱스 ID (ULID)"));
  }

  public static Snippet addIndexColumnRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet addIndexColumnRequest() {
    return requestFields(
        fieldWithPath("columnId").type(JsonFieldType.STRING).description("추가할 컬럼 ID (ULID)"),
        fieldWithPath("seqNo").type(JsonFieldType.NUMBER)
            .description("순서 번호 (미입력 시 마지막 위치로 자동 설정)").optional(),
        fieldWithPath("sortDirection").type(JsonFieldType.STRING).description("정렬 방향 (ASC, DESC)"));
  }

  public static Snippet addIndexColumnResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet addIndexColumnResponse() {
    return createResponseFieldsSnippet(
        mutationResponseFields(indexColumnResponseFields("result.data.")));
  }

  // ========== DELETE /api/index-columns/{indexColumnId} ==========

  public static Snippet removeIndexColumnPathParameters() {
    return pathParameters(
        parameterWithName("indexColumnId").description("인덱스 컬럼 ID (ULID)"));
  }

  public static Snippet removeIndexColumnRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet removeIndexColumnResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet removeIndexColumnResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== GET /api/index-columns/{indexColumnId} ==========

  public static Snippet getIndexColumnPathParameters() {
    return pathParameters(
        parameterWithName("indexColumnId").description("조회할 인덱스 컬럼 ID (ULID)"));
  }

  public static Snippet getIndexColumnRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet getIndexColumnResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet getIndexColumnResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(indexColumnResponseFields("result.")));
  }

  // ========== PATCH /api/index-columns/{indexColumnId}/position ==========

  public static Snippet changeIndexColumnPositionPathParameters() {
    return pathParameters(
        parameterWithName("indexColumnId").description("변경할 인덱스 컬럼 ID (ULID)"));
  }

  public static Snippet changeIndexColumnPositionRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet changeIndexColumnPositionRequest() {
    return requestFields(
        fieldWithPath("seqNo").type(JsonFieldType.NUMBER).description("변경할 순서 번호"));
  }

  public static Snippet changeIndexColumnPositionResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet changeIndexColumnPositionResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== PATCH /api/index-columns/{indexColumnId}/sort-direction ==========

  public static Snippet changeIndexColumnSortDirectionPathParameters() {
    return pathParameters(
        parameterWithName("indexColumnId").description("변경할 인덱스 컬럼 ID (ULID)"));
  }

  public static Snippet changeIndexColumnSortDirectionRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet changeIndexColumnSortDirectionRequest() {
    return requestFields(
        fieldWithPath("sortDirection").type(JsonFieldType.STRING).description("변경할 정렬 방향 (ASC, DESC)"));
  }

  public static Snippet changeIndexColumnSortDirectionResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet changeIndexColumnSortDirectionResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

}
