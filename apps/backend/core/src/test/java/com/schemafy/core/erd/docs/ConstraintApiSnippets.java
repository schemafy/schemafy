package com.schemafy.core.erd.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.core.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

/** Constraint API 문서화를 위한 스니펫 제공 클래스 */
public class ConstraintApiSnippets extends RestDocsSnippets {

  // ========== Constraint 도메인 공통 필드 ==========

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

  // ========== POST /api/constraints - 제약조건 생성 ==========

  public static Snippet createConstraintRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet createConstraintRequest() {
    return requestFields(
        fieldWithPath("tableId").type(JsonFieldType.STRING)
            .description("테이블 ID (ULID)"),
        fieldWithPath("name").type(JsonFieldType.STRING)
            .description("제약조건 이름 (미입력 시 자동 생성)").optional(),
        fieldWithPath("kind").type(JsonFieldType.STRING)
            .description("제약조건 종류 (PRIMARY_KEY, UNIQUE, CHECK, DEFAULT, NOT_NULL)"),
        fieldWithPath("checkExpr").type(JsonFieldType.STRING)
            .description("CHECK 표현식").optional(),
        fieldWithPath("defaultExpr").type(JsonFieldType.STRING)
            .description("DEFAULT 표현식").optional(),
        fieldWithPath("columns").type(JsonFieldType.ARRAY)
            .description("제약조건 컬럼 목록").optional(),
        fieldWithPath("columns[].columnId").type(JsonFieldType.STRING)
            .description("컬럼 ID (ULID, columns 항목 제공 시 필수)").optional(),
        fieldWithPath("columns[].seqNo").type(JsonFieldType.NUMBER)
            .description("순서 번호 (미입력 시 0부터 자동 배정)").optional());
  }

  public static Snippet createConstraintResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet createConstraintResponse() {
    return createResponseFieldsSnippet(
        mutationResponseFields(constraintResponseFields("result.data.")));
  }

  // ========== GET /api/constraints/{constraintId} ==========

  public static Snippet getConstraintPathParameters() {
    return pathParameters(
        parameterWithName("constraintId").description("조회할 제약조건 ID (ULID)"));
  }

  public static Snippet getConstraintRequestHeaders() { return createRequestHeadersSnippet(commonRequestHeaders()); }

  public static Snippet getConstraintResponseHeaders() { return createResponseHeadersSnippet(commonResponseHeaders()); }

  public static Snippet getConstraintResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(constraintResponseFields("result.")));
  }

  // ========== GET /api/tables/{tableId}/constraints ==========

  public static Snippet getConstraintsByTableIdPathParameters() {
    return pathParameters(
        parameterWithName("tableId").description("테이블 ID (ULID)"));
  }

  public static Snippet getConstraintsByTableIdRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet getConstraintsByTableIdResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet getConstraintsByTableIdResponse() {
    return createResponseFieldsSnippet(concat(
        new FieldDescriptor[] {
          fieldWithPath("success").type(JsonFieldType.BOOLEAN)
              .description("요청 성공 여부"),
          fieldWithPath("result").type(JsonFieldType.ARRAY)
              .description("제약조건 목록")
        },
        concat(
            constraintResponseFields("result[]."),
            new FieldDescriptor[] {
              fieldWithPath("error").type(JsonFieldType.NULL)
                  .description("에러 정보 (성공 시 null)").optional()
            })));
  }

  // ========== PATCH /api/constraints/{constraintId}/name ==========

  public static Snippet changeConstraintNamePathParameters() {
    return pathParameters(
        parameterWithName("constraintId").description("변경할 제약조건 ID (ULID)"));
  }

  public static Snippet changeConstraintNameRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet changeConstraintNameRequest() {
    return requestFields(
        fieldWithPath("newName").type(JsonFieldType.STRING).description("변경할 제약조건 이름"));
  }

  public static Snippet changeConstraintNameResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet changeConstraintNameResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== PATCH /api/constraints/{constraintId}/check-expr ==========

  public static Snippet changeConstraintCheckExprPathParameters() {
    return pathParameters(
        parameterWithName("constraintId").description("변경할 제약조건 ID (ULID)"));
  }

  public static Snippet changeConstraintCheckExprRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet changeConstraintCheckExprRequest() {
    return requestFields(
        fieldWithPath("checkExpr").type(JsonFieldType.VARIES)
            .description("변경할 CHECK 표현식 (null 또는 공백 문자열 전달 시 제거)"));
  }

  public static Snippet changeConstraintCheckExprResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet changeConstraintCheckExprResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== PATCH /api/constraints/{constraintId}/default-expr ==========

  public static Snippet changeConstraintDefaultExprPathParameters() {
    return pathParameters(
        parameterWithName("constraintId").description("변경할 제약조건 ID (ULID)"));
  }

  public static Snippet changeConstraintDefaultExprRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet changeConstraintDefaultExprRequest() {
    return requestFields(
        fieldWithPath("defaultExpr").type(JsonFieldType.VARIES)
            .description("변경할 DEFAULT 표현식 (null 또는 공백 문자열 전달 시 제거)"));
  }

  public static Snippet changeConstraintDefaultExprResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet changeConstraintDefaultExprResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== DELETE /api/constraints/{constraintId} ==========

  public static Snippet deleteConstraintPathParameters() {
    return pathParameters(
        parameterWithName("constraintId").description("삭제할 제약조건 ID (ULID)"));
  }

  public static Snippet deleteConstraintRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet deleteConstraintResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet deleteConstraintResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== GET /api/constraints/{constraintId}/columns ==========

  public static Snippet getConstraintColumnsPathParameters() {
    return pathParameters(
        parameterWithName("constraintId").description("제약조건 ID (ULID)"));
  }

  public static Snippet getConstraintColumnsRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet getConstraintColumnsResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet getConstraintColumnsResponse() {
    return createResponseFieldsSnippet(concat(
        new FieldDescriptor[] {
          fieldWithPath("success").type(JsonFieldType.BOOLEAN)
              .description("요청 성공 여부"),
          fieldWithPath("result").type(JsonFieldType.ARRAY)
              .description("제약조건 컬럼 목록")
        },
        concat(
            constraintColumnResponseFields("result[]."),
            new FieldDescriptor[] {
              fieldWithPath("error").type(JsonFieldType.NULL)
                  .description("에러 정보 (성공 시 null)").optional()
            })));
  }

  // ========== POST /api/constraints/{constraintId}/columns ==========

  public static Snippet addConstraintColumnPathParameters() {
    return pathParameters(
        parameterWithName("constraintId").description("제약조건 ID (ULID)"));
  }

  public static Snippet addConstraintColumnRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet addConstraintColumnRequest() {
    return requestFields(
        fieldWithPath("columnId").type(JsonFieldType.STRING).description("추가할 컬럼 ID (ULID)"),
        fieldWithPath("seqNo").type(JsonFieldType.NUMBER)
            .description("순서 번호 (미입력 시 마지막 위치로 자동 설정)").optional());
  }

  public static Snippet addConstraintColumnResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet addConstraintColumnResponse() {
    return createResponseFieldsSnippet(
        mutationResponseFields(
            fieldWithPath("result.data.id").type(JsonFieldType.STRING)
                .description("제약조건 컬럼 ID (ULID)"),
            fieldWithPath("result.data.constraintId").type(JsonFieldType.STRING)
                .description("제약조건 ID (ULID)"),
            fieldWithPath("result.data.columnId").type(JsonFieldType.STRING)
                .description("컬럼 ID (ULID)"),
            fieldWithPath("result.data.seqNo").type(JsonFieldType.NUMBER)
                .description("순서 번호"),
            fieldWithPath("result.data.cascadeCreatedColumns").type(JsonFieldType.ARRAY)
                .description("FK 제약조건 추가 시 자동 생성된 컬럼 목록").optional()));
  }

  // ========== DELETE /api/constraint-columns/{constraintColumnId} ==========

  public static Snippet removeConstraintColumnPathParameters() {
    return pathParameters(
        parameterWithName("constraintColumnId").description("제약조건 컬럼 ID (ULID)"));
  }

  public static Snippet removeConstraintColumnRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet removeConstraintColumnResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet removeConstraintColumnResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== GET /api/constraint-columns/{constraintColumnId} ==========

  public static Snippet getConstraintColumnPathParameters() {
    return pathParameters(
        parameterWithName("constraintColumnId").description("조회할 제약조건 컬럼 ID (ULID)"));
  }

  public static Snippet getConstraintColumnRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet getConstraintColumnResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet getConstraintColumnResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(constraintColumnResponseFields("result.")));
  }

  // ========== PATCH /api/constraint-columns/{constraintColumnId}/position ==========

  public static Snippet changeConstraintColumnPositionPathParameters() {
    return pathParameters(
        parameterWithName("constraintColumnId").description("변경할 제약조건 컬럼 ID (ULID)"));
  }

  public static Snippet changeConstraintColumnPositionRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet changeConstraintColumnPositionRequest() {
    return requestFields(
        fieldWithPath("seqNo").type(JsonFieldType.NUMBER)
            .description("변경할 순서 번호 (미입력 시 0)").optional());
  }

  public static Snippet changeConstraintColumnPositionResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet changeConstraintColumnPositionResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

}
