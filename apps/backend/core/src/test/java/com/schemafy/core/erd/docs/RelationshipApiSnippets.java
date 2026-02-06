package com.schemafy.core.erd.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

import com.schemafy.core.common.docs.RestDocsSnippets;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;

/** Relationship API 문서화를 위한 스니펫 제공 클래스 */
public class RelationshipApiSnippets extends RestDocsSnippets {

  // ========== Relationship 도메인 공통 필드 ==========

  /** RelationshipResponse 필드 */
  private static FieldDescriptor[] relationshipResponseFields(String prefix) {
    return new FieldDescriptor[] {
      fieldWithPath(prefix + "id").type(JsonFieldType.STRING)
          .description("관계 ID (ULID)"),
      fieldWithPath(prefix + "pkTableId").type(JsonFieldType.STRING)
          .description("PK 테이블 ID (ULID)"),
      fieldWithPath(prefix + "fkTableId").type(JsonFieldType.STRING)
          .description("FK 테이블 ID (ULID)"),
      fieldWithPath(prefix + "name").type(JsonFieldType.STRING)
          .description("관계 이름").optional(),
      fieldWithPath(prefix + "kind").type(JsonFieldType.STRING)
          .description("관계 종류 (IDENTIFYING, NON_IDENTIFYING)"),
      fieldWithPath(prefix + "cardinality").type(JsonFieldType.STRING)
          .description("카디널리티 (ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY)"),
      fieldWithPath(prefix + "extra").type(JsonFieldType.STRING)
          .description("추가 정보 (JSON)").optional()
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

  // ========== POST /api/relationships - 관계 생성 ==========

  public static Snippet createRelationshipRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet createRelationshipRequest() {
    return requestFields(
        fieldWithPath("fkTableId").type(JsonFieldType.STRING)
            .description("FK 테이블 ID (ULID)"),
        fieldWithPath("pkTableId").type(JsonFieldType.STRING)
            .description("PK 테이블 ID (ULID)"),
        fieldWithPath("kind").type(JsonFieldType.STRING)
            .description("관계 종류 (IDENTIFYING, NON_IDENTIFYING)"),
        fieldWithPath("cardinality").type(JsonFieldType.STRING)
            .description("카디널리티 (ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY)"));
  }

  public static Snippet createRelationshipResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet createRelationshipResponse() {
    return createResponseFieldsSnippet(
        mutationResponseFields(relationshipResponseFields("result.data.")));
  }

  // ========== GET /api/relationships/{relationshipId} ==========

  public static Snippet getRelationshipPathParameters() {
    return pathParameters(
        parameterWithName("relationshipId").description("조회할 관계 ID (ULID)"));
  }

  public static Snippet getRelationshipRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet getRelationshipResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet getRelationshipResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(relationshipResponseFields("result.")));
  }

  // ========== GET /api/tables/{tableId}/relationships ==========

  public static Snippet getRelationshipsByTableIdPathParameters() {
    return pathParameters(
        parameterWithName("tableId").description("테이블 ID (ULID)"));
  }

  public static Snippet getRelationshipsByTableIdRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet getRelationshipsByTableIdResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet getRelationshipsByTableIdResponse() {
    return createResponseFieldsSnippet(concat(
        new FieldDescriptor[] {
          fieldWithPath("success").type(JsonFieldType.BOOLEAN)
              .description("요청 성공 여부"),
          fieldWithPath("result").type(JsonFieldType.ARRAY)
              .description("관계 목록")
        },
        concat(
            relationshipResponseFields("result[]."),
            new FieldDescriptor[] {
              fieldWithPath("error").type(JsonFieldType.NULL)
                  .description("에러 정보 (성공 시 null)").optional()
            })));
  }

  // ========== PATCH /api/relationships/{relationshipId}/name ==========

  public static Snippet changeRelationshipNamePathParameters() {
    return pathParameters(
        parameterWithName("relationshipId").description("변경할 관계 ID (ULID)"));
  }

  public static Snippet changeRelationshipNameRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet changeRelationshipNameRequest() {
    return requestFields(
        fieldWithPath("newName").type(JsonFieldType.STRING).description("변경할 관계 이름"));
  }

  public static Snippet changeRelationshipNameResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet changeRelationshipNameResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== PATCH /api/relationships/{relationshipId}/kind ==========

  public static Snippet changeRelationshipKindPathParameters() {
    return pathParameters(
        parameterWithName("relationshipId").description("변경할 관계 ID (ULID)"));
  }

  public static Snippet changeRelationshipKindRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet changeRelationshipKindRequest() {
    return requestFields(
        fieldWithPath("kind").type(JsonFieldType.STRING)
            .description("변경할 관계 종류 (IDENTIFYING, NON_IDENTIFYING)"));
  }

  public static Snippet changeRelationshipKindResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet changeRelationshipKindResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== PATCH /api/relationships/{relationshipId}/cardinality ==========

  public static Snippet changeRelationshipCardinalityPathParameters() {
    return pathParameters(
        parameterWithName("relationshipId").description("변경할 관계 ID (ULID)"));
  }

  public static Snippet changeRelationshipCardinalityRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet changeRelationshipCardinalityRequest() {
    return requestFields(
        fieldWithPath("cardinality").type(JsonFieldType.STRING)
            .description("변경할 카디널리티 (ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY)"));
  }

  public static Snippet changeRelationshipCardinalityResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet changeRelationshipCardinalityResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== PATCH /api/relationships/{relationshipId}/extra ==========

  public static Snippet changeRelationshipExtraPathParameters() {
    return pathParameters(
        parameterWithName("relationshipId").description("변경할 관계 ID (ULID)"));
  }

  public static Snippet changeRelationshipExtraRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet changeRelationshipExtraRequest() {
    return requestFields(
        fieldWithPath("extra").type(JsonFieldType.STRING)
            .description("변경할 추가 정보 (JSON)").optional());
  }

  public static Snippet changeRelationshipExtraResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet changeRelationshipExtraResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== DELETE /api/relationships/{relationshipId} ==========

  public static Snippet deleteRelationshipPathParameters() {
    return pathParameters(
        parameterWithName("relationshipId").description("삭제할 관계 ID (ULID)"));
  }

  public static Snippet deleteRelationshipRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet deleteRelationshipResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet deleteRelationshipResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== GET /api/relationships/{relationshipId}/columns ==========

  public static Snippet getRelationshipColumnsPathParameters() {
    return pathParameters(
        parameterWithName("relationshipId").description("관계 ID (ULID)"));
  }

  public static Snippet getRelationshipColumnsRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet getRelationshipColumnsResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet getRelationshipColumnsResponse() {
    return createResponseFieldsSnippet(concat(
        new FieldDescriptor[] {
          fieldWithPath("success").type(JsonFieldType.BOOLEAN)
              .description("요청 성공 여부"),
          fieldWithPath("result").type(JsonFieldType.ARRAY)
              .description("관계 컬럼 목록")
        },
        concat(
            relationshipColumnResponseFields("result[]."),
            new FieldDescriptor[] {
              fieldWithPath("error").type(JsonFieldType.NULL)
                  .description("에러 정보 (성공 시 null)").optional()
            })));
  }

  // ========== POST /api/relationships/{relationshipId}/columns ==========

  public static Snippet addRelationshipColumnPathParameters() {
    return pathParameters(
        parameterWithName("relationshipId").description("관계 ID (ULID)"));
  }

  public static Snippet addRelationshipColumnRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet addRelationshipColumnRequest() {
    return requestFields(
        fieldWithPath("pkColumnId").type(JsonFieldType.STRING).description("PK 컬럼 ID (ULID)"),
        fieldWithPath("fkColumnId").type(JsonFieldType.STRING).description("FK 컬럼 ID (ULID)"),
        fieldWithPath("seqNo").type(JsonFieldType.NUMBER).description("순서 번호"));
  }

  public static Snippet addRelationshipColumnResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet addRelationshipColumnResponse() {
    return createResponseFieldsSnippet(
        mutationResponseFields(relationshipColumnResponseFields("result.data.")));
  }

  // ========== DELETE /api/relationships/{relationshipId}/columns/{relationshipColumnId} ==========

  public static Snippet removeRelationshipColumnPathParameters() {
    return pathParameters(
        parameterWithName("relationshipId").description("관계 ID (ULID)"),
        parameterWithName("relationshipColumnId").description("관계 컬럼 ID (ULID)"));
  }

  public static Snippet removeRelationshipColumnRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet removeRelationshipColumnResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet removeRelationshipColumnResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

  // ========== GET /api/relationship-columns/{relationshipColumnId} ==========

  public static Snippet getRelationshipColumnPathParameters() {
    return pathParameters(
        parameterWithName("relationshipColumnId").description("조회할 관계 컬럼 ID (ULID)"));
  }

  public static Snippet getRelationshipColumnRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet getRelationshipColumnResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet getRelationshipColumnResponse() {
    return createResponseFieldsSnippet(
        successResponseFields(relationshipColumnResponseFields("result.")));
  }

  // ========== PATCH /api/relationship-columns/{relationshipColumnId}/position ==========

  public static Snippet changeRelationshipColumnPositionPathParameters() {
    return pathParameters(
        parameterWithName("relationshipColumnId").description("변경할 관계 컬럼 ID (ULID)"));
  }

  public static Snippet changeRelationshipColumnPositionRequestHeaders() {
    return createRequestHeadersSnippet(commonRequestHeaders());
  }

  public static Snippet changeRelationshipColumnPositionRequest() {
    return requestFields(
        fieldWithPath("seqNo").type(JsonFieldType.NUMBER).description("변경할 순서 번호"));
  }

  public static Snippet changeRelationshipColumnPositionResponseHeaders() {
    return createResponseHeadersSnippet(commonResponseHeaders());
  }

  public static Snippet changeRelationshipColumnPositionResponse() {
    return createResponseFieldsSnippet(mutationResponseFieldsWithNullData());
  }

}
