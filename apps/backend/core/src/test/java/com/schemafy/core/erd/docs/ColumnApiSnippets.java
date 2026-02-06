package com.schemafy.core.erd.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.core.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

/** Column API 문서화를 위한 스니펫 제공 클래스 */
public class ColumnApiSnippets extends RestDocsSnippets {

  // ========== Column 도메인 공통 필드 ==========

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

  // ========== POST /api/columns - 컬럼 생성 ==========

  /** 컬럼 생성 요청 헤더 */
  public static Snippet createColumnRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 컬럼 생성 요청 바디 */
  public static Snippet createColumnRequest() {
    return requestFields(
        fieldWithPath("tableId").type(JsonFieldType.STRING)
            .description("테이블 ID (ULID)"),
        fieldWithPath("name").type(JsonFieldType.STRING)
            .description("컬럼 이름"),
        fieldWithPath("dataType").type(JsonFieldType.STRING)
            .description("데이터 타입 (VARCHAR, INT, BIGINT 등)"),
        fieldWithPath("length").type(JsonFieldType.NUMBER)
            .description("길이 (VARCHAR 등에 사용)").optional(),
        fieldWithPath("precision").type(JsonFieldType.NUMBER)
            .description("정밀도 (DECIMAL 등에 사용)").optional(),
        fieldWithPath("scale").type(JsonFieldType.NUMBER)
            .description("스케일 (DECIMAL 등에 사용)").optional(),
        fieldWithPath("seqNo").type(JsonFieldType.NUMBER)
            .description("순서 번호 (미입력 시 마지막 위치로 자동 설정)").optional(),
        fieldWithPath("autoIncrement").type(JsonFieldType.BOOLEAN)
            .description("자동 증가 여부"),
        fieldWithPath("charset").type(JsonFieldType.STRING)
            .description("문자셋").optional(),
        fieldWithPath("collation").type(JsonFieldType.STRING)
            .description("콜레이션").optional(),
        fieldWithPath("comment").type(JsonFieldType.STRING)
            .description("코멘트").optional());
  }

  /** 컬럼 생성 응답 헤더 */
  public static Snippet createColumnResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 컬럼 생성 응답 */
  public static Snippet createColumnResponse() {
    return createResponseFieldsSnippet(
        mutationResponseFields(columnResponseFields("result.data.")));
  }

  // ========== GET /api/columns/{columnId} - 컬럼 조회 ==========

  /** 컬럼 조회 경로 파라미터 */
  public static Snippet getColumnPathParameters() {
    return pathParameters(
        parameterWithName("columnId")
            .description("조회할 컬럼 ID (ULID)"));
  }

  /** 컬럼 조회 요청 헤더 */
  public static Snippet getColumnRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 컬럼 조회 응답 헤더 */
  public static Snippet getColumnResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 컬럼 조회 응답 */
  public static Snippet getColumnResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(columnResponseFields("result.")));
  }

  // ========== GET /api/tables/{tableId}/columns - 테이블별 컬럼 목록 조회 ==========

  /** 테이블별 컬럼 목록 조회 경로 파라미터 */
  public static Snippet getColumnsByTableIdPathParameters() {
    return pathParameters(
        parameterWithName("tableId")
            .description("테이블 ID (ULID)"));
  }

  /** 테이블별 컬럼 목록 조회 요청 헤더 */
  public static Snippet getColumnsByTableIdRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 테이블별 컬럼 목록 조회 응답 헤더 */
  public static Snippet getColumnsByTableIdResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 테이블별 컬럼 목록 조회 응답 */
  public static Snippet getColumnsByTableIdResponse() {
    return createResponseFieldsSnippet(concat(
        new FieldDescriptor[] {
          fieldWithPath("success").type(JsonFieldType.BOOLEAN)
              .description("요청 성공 여부"),
          fieldWithPath("result").type(JsonFieldType.ARRAY)
              .description("컬럼 목록")
        },
        concat(
            columnResponseFields("result[]."),
            new FieldDescriptor[] {
              fieldWithPath("error").type(JsonFieldType.NULL)
                  .description("에러 정보 (성공 시 null)").optional()
            })));
  }

  // ========== PATCH /api/columns/{columnId}/name - 컬럼 이름 변경 ==========

  /** 컬럼 이름 변경 경로 파라미터 */
  public static Snippet changeColumnNamePathParameters() {
    return pathParameters(
        parameterWithName("columnId")
            .description("변경할 컬럼 ID (ULID)"));
  }

  /** 컬럼 이름 변경 요청 헤더 */
  public static Snippet changeColumnNameRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 컬럼 이름 변경 요청 바디 */
  public static Snippet changeColumnNameRequest() {
    return requestFields(
        fieldWithPath("newName").type(JsonFieldType.STRING)
            .description("변경할 컬럼 이름"));
  }

  /** 컬럼 이름 변경 응답 헤더 */
  public static Snippet changeColumnNameResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 컬럼 이름 변경 응답 */
  public static Snippet changeColumnNameResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== PATCH /api/columns/{columnId}/type - 컬럼 타입 변경 ==========

  /** 컬럼 타입 변경 경로 파라미터 */
  public static Snippet changeColumnTypePathParameters() {
    return pathParameters(
        parameterWithName("columnId")
            .description("변경할 컬럼 ID (ULID)"));
  }

  /** 컬럼 타입 변경 요청 헤더 */
  public static Snippet changeColumnTypeRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 컬럼 타입 변경 요청 바디 */
  public static Snippet changeColumnTypeRequest() {
    return requestFields(
        fieldWithPath("dataType").type(JsonFieldType.STRING)
            .description("데이터 타입"),
        fieldWithPath("length").type(JsonFieldType.NUMBER)
            .description("길이").optional(),
        fieldWithPath("precision").type(JsonFieldType.NUMBER)
            .description("정밀도").optional(),
        fieldWithPath("scale").type(JsonFieldType.NUMBER)
            .description("스케일").optional());
  }

  /** 컬럼 타입 변경 응답 헤더 */
  public static Snippet changeColumnTypeResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 컬럼 타입 변경 응답 */
  public static Snippet changeColumnTypeResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== PATCH /api/columns/{columnId}/meta - 컬럼 메타 변경 ==========

  /** 컬럼 메타 변경 경로 파라미터 */
  public static Snippet changeColumnMetaPathParameters() {
    return pathParameters(
        parameterWithName("columnId")
            .description("변경할 컬럼 ID (ULID)"));
  }

  /** 컬럼 메타 변경 요청 헤더 */
  public static Snippet changeColumnMetaRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 컬럼 메타 변경 요청 바디 */
  public static Snippet changeColumnMetaRequest() {
    return requestFields(
        fieldWithPath("autoIncrement").type(JsonFieldType.BOOLEAN)
            .description("자동 증가 여부").optional(),
        fieldWithPath("charset").type(JsonFieldType.STRING)
            .description("문자셋").optional(),
        fieldWithPath("collation").type(JsonFieldType.STRING)
            .description("콜레이션").optional(),
        fieldWithPath("comment").type(JsonFieldType.STRING)
            .description("코멘트").optional());
  }

  /** 컬럼 메타 변경 응답 헤더 */
  public static Snippet changeColumnMetaResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 컬럼 메타 변경 응답 */
  public static Snippet changeColumnMetaResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== PATCH /api/columns/{columnId}/position - 컬럼 위치 변경 ==========

  /** 컬럼 위치 변경 경로 파라미터 */
  public static Snippet changeColumnPositionPathParameters() {
    return pathParameters(
        parameterWithName("columnId")
            .description("변경할 컬럼 ID (ULID)"));
  }

  /** 컬럼 위치 변경 요청 헤더 */
  public static Snippet changeColumnPositionRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 컬럼 위치 변경 요청 바디 */
  public static Snippet changeColumnPositionRequest() {
    return requestFields(
        fieldWithPath("seqNo").type(JsonFieldType.NUMBER)
            .description("변경할 순서 번호"));
  }

  /** 컬럼 위치 변경 응답 헤더 */
  public static Snippet changeColumnPositionResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 컬럼 위치 변경 응답 */
  public static Snippet changeColumnPositionResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== DELETE /api/columns/{columnId} - 컬럼 삭제 ==========

  /** 컬럼 삭제 경로 파라미터 */
  public static Snippet deleteColumnPathParameters() {
    return pathParameters(
        parameterWithName("columnId")
            .description("삭제할 컬럼 ID (ULID)"));
  }

  /** 컬럼 삭제 요청 헤더 */
  public static Snippet deleteColumnRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  /** 컬럼 삭제 응답 헤더 */
  public static Snippet deleteColumnResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  /** 컬럼 삭제 응답 */
  public static Snippet deleteColumnResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

}
